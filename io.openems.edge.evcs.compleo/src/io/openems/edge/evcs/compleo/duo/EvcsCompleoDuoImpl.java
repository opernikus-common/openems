package io.openems.edge.evcs.compleo.duo;

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
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
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
		name = "Evcs.Compleo.Duo", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class EvcsCompleoDuoImpl extends AbstractOpenemsModbusComponent
		implements EvcsCompleoDuo, Evcs, ManagedEvcs, EventHandler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EvcsCompleoDuoImpl.class);

	/** Handles charge states. */
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);
	/** Processes the controller's writes to this evcs component. */
	private final WriteHandler writeHandler = new WriteHandler(this);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private EvcsPower evcsPower;

	private static final int PRE_OFFSET = 0x100;
	private static final int OFFSET = 0x10;

	private Config config;
	private CompleoDuoReadHandler readHandler;

	public EvcsCompleoDuoImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EvcsCompleoDuo.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		this._setChargingType(ChargingType.AC);
		this._setPowerPrecision(230); // 1A steps
		this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
		this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());
		this.readHandler = new CompleoDuoReadHandler(this);

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
		int plug = this.config.plug().plug;
		return new ModbusProtocol(this, new FC4ReadInputRegistersTask(PRE_OFFSET + 0 + plug * OFFSET, Priority.HIGH,
				m(EvcsCompleoDuo.ChannelId.POWER_LIMIT, new UnsignedWordElement(PRE_OFFSET + 0 + plug * OFFSET),
						ElementToChannelConverter.SCALE_FACTOR_2),
				m(EvcsCompleoDuo.ChannelId.CHARGE_POINT_STATE, new UnsignedWordElement(PRE_OFFSET + 1 + plug + OFFSET)),
				m(Evcs.ChannelId.CHARGE_POWER, new UnsignedWordElement(PRE_OFFSET + 2 + plug * OFFSET),
						ElementToChannelConverter.SCALE_FACTOR_2),
				m(Evcs.ChannelId.CURRENT_L1, new UnsignedWordElement(PRE_OFFSET + 3 + plug * OFFSET),
						ElementToChannelConverter.SCALE_FACTOR_2),
				m(Evcs.ChannelId.CURRENT_L2, new UnsignedWordElement(PRE_OFFSET + 4 + plug * OFFSET),
						ElementToChannelConverter.SCALE_FACTOR_2),
				m(Evcs.ChannelId.CURRENT_L3, new UnsignedWordElement(PRE_OFFSET + 5 + plug * OFFSET),
						ElementToChannelConverter.SCALE_FACTOR_2),
				new DummyRegisterElement(PRE_OFFSET + 6 + plug * OFFSET, PRE_OFFSET + 7 + plug * OFFSET),
				m(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedWordElement(PRE_OFFSET + 8 + plug * OFFSET),
						ElementToChannelConverter.SCALE_FACTOR_2)),
				new FC6WriteRegisterTask(PRE_OFFSET + 0 + plug * OFFSET, //
						m(EvcsCompleoDuo.ChannelId.POWER_LIMIT, new UnsignedWordElement(PRE_OFFSET + 0 + plug * OFFSET),
								ElementToChannelConverter.SCALE_FACTOR_2)));
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> this.readHandler.run();
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> this.writeHandler.run();
		}
	}

	@Override
	public String debugLog() {
		return "Limit:" + this.getSetChargePowerLimit().toString() + "|" + this.getStatus().getName();
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
		if (power < DEFAULT_MINIMUM_HARDWARE_POWER) {
			power = 0;
		}
		this.setPowerLimit(power);
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
		return 30;
	}

	@Override
	public ChargeStateHandler getChargeStateHandler() {
		return this.chargeStateHandler;
	}

	@Override
	public void applyChargePowerPerPhase(boolean value) {
		this.writeHandler.applyChargePowerPerPhase(value);
	}
	
	@Override
	public void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}
}
