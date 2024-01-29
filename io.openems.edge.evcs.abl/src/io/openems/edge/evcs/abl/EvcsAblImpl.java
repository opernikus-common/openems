package io.openems.edge.evcs.abl;

import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.evcs.api.EvcsUtils;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.WriteHandler;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Abl", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class EvcsAblImpl extends AbstractOpenemsModbusComponent
		implements EvcsAbl, Evcs, ManagedEvcs, ModbusComponent, EventHandler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EvcsAblImpl.class);

	/** Handles charge states. */
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);
	/** Processes the controller's writes to this evcs component. */
	private final WriteHandler writeHandler = new WriteHandler(this);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private EvcsPower evcsPower;

	private Config config;
	private AblStatusUpdater statusUpdater;

	public EvcsAblImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EvcsAbl.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this._setPhaseRotation(config.phaseRotation());
		this._setPriority(config.priority());
		this._setChargingType(ChargingType.AC);
		this._setPowerPrecision(230); // 1A steps
		this._setFixedMinimumHardwarePower(
				EvcsUtils.currentInMilliampereToPower(this.config.minHwCurrent(), Phases.THREE_PHASE.getValue()));
		this._setFixedMaximumHardwarePower(
				EvcsUtils.currentInMilliampereToPower(this.config.maxHwCurrent(), Phases.THREE_PHASE.getValue()));
		this.statusUpdater = new AblStatusUpdater(this);

		/*
		 * Calculates the maximum and minimum hardware power dynamically by listening on
		 * the fixed hardware limit and the phases used for charging
		 */
		Evcs.addCalculatePowerLimitListeners(this);

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		var offset = 0x0100;
		int plug = this.config.plug().plug;
		/*
		 * The ABL EMH4 does not support reading Multiple Registers in one task
		 * with "gaps" in between. Therefore, this modbus protocol consists of many
		 * small Tasks to compensate.
		 */
		var modbusProtocol = new ModbusProtocol(this,
				new FC3ReadRegistersTask(12289 + plug * offset, Priority.HIGH,
						m(Evcs.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(12289 + plug * offset),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(Evcs.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(12291 + plug * offset),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(Evcs.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(12293 + plug * offset),
								ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC3ReadRegistersTask(12301 + plug * offset, Priority.HIGH,
						m(Evcs.ChannelId.CHARGE_POWER, new UnsignedDoublewordElement(12301 + plug * offset)),
						m(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY,
								new UnsignedDoublewordElement(12303 + plug * offset),
								ElementToChannelConverter.SCALE_FACTOR_1)),
				new FC3ReadRegistersTask(12337 + plug * offset, Priority.HIGH,
						m(EvcsAbl.ChannelId.CHARGE_POINT_STATE, new UnsignedWordElement(12337 + plug * offset))),
				new FC3ReadRegistersTask(12338 + plug * offset, Priority.HIGH,
						m(EvcsAbl.ChannelId.CHARGING_CURRENT, new UnsignedWordElement(12338 + plug * offset),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)),
				new FC6WriteRegisterTask(12338 + plug * offset, //
						m(EvcsAbl.ChannelId.CHARGING_CURRENT, new UnsignedWordElement(12338 + plug * offset),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)));
		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		return "Limit:" + this.getSetChargePowerLimit().orElse(null) + this.getStatus().getName();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return Math.round(this.config.minHwCurrent() / 1000f) * Evcs.DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return Math.round(this.config.maxHwCurrent() / 1000f) * Evcs.DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return false;
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
		var phases = this.getPhasesAsInt();
		var current = Math.round((float) power / phases / 230f);
		/*
		 * Limits the charging value because Abl knows only values between 6 and 32
		 */
		current = Math.min(current, 32);
		
		if (current < 6) {
			current = 0;
		}
		this.setCurrentLimit(current);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		return this.applyChargePowerLimit(0);
	}

	@Override
	public boolean applyDisplayText(String text) {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 5;
	}

	@Override
	public int getWriteInterval() {
		return 1;
	}

	@Override
	public ChargeStateHandler getChargeStateHandler() {
		return this.chargeStateHandler;
	}

	@Override
	public void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> this.statusUpdater.update();
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> this.writeHandler.run();
		}
	}

}
