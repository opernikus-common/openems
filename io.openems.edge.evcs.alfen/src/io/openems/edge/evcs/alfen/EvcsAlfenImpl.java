package io.openems.edge.evcs.alfen;

import org.osgi.service.cm.ConfigurationAdmin;
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

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.FloatQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Alfen", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class EvcsAlfenImpl extends AbstractOpenemsModbusComponent
		implements EvcsAlfen, Evcs, ManagedEvcs, ModbusComponent, EventHandler, OpenemsComponent {

	private static final float DETECT_PHASE_ACTIVITY = 400; // mA

	private final Logger log = LoggerFactory.getLogger(EvcsAlfenImpl.class);

	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	/**
	 * Processes the controller's writes to this evcs component.
	 */
	private final WriteHandler writeHandler = new WriteHandler(this);

	private Config config;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private EvcsPower evcsPower;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Long energyAtSessionStart = 0L;

	/**
	 * The last three binary digits of phasePattern represent whether there is
	 * current on the corresponding phase. For example, if phasePattern = 6 = 0b110,
	 * there is current on L2 and L3.
	 */
	private int phasePattern = 0;

	public EvcsAlfenImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				EvcsAlfen.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		this._setPhaseRotation(config.phaseRotation());
		this._setPriority(config.priority());
		this._setFixedMaximumHardwarePower(Evcs.DEFAULT_MAXIMUM_HARDWARE_POWER);
		this._setFixedMinimumHardwarePower(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);

		Evcs.addCalculatePowerLimitListeners(this);
		this.installListeners();

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Installs listeners to set phases and detect when a new session starts.
	 */
	private void installListeners() {
		final var curL1 = this.channel(this.getPhaseRotation().getFirstPhase());
		final var curL2 = this.channel(this.getPhaseRotation().getSecondPhase());
		final var curL3 = this.channel(this.getPhaseRotation().getThirdPhase());

		this.channel(EvcsAlfen.ChannelId.RAW_CURRENT_L1).onUpdate(newValue -> {
			Integer current = 0;
			if (newValue.isDefined()) {
				current = TypeUtils.getAsType(OpenemsType.INTEGER, newValue.get());
				if (current.intValue() > DETECT_PHASE_ACTIVITY) {
					this.phasePattern |= 0x01;
				} else {
					this.phasePattern &= ~0x01;
				}
			} else {
				this.phasePattern &= ~0x01;
			}
			curL1.setNextValue(current);
			this.updatePhases();
		});
		this.channel(EvcsAlfen.ChannelId.RAW_CURRENT_L2).onUpdate(newValue -> {
			Integer current = 0;
			if (newValue.isDefined()) {
				current = TypeUtils.getAsType(OpenemsType.INTEGER, newValue.get());
				if (current.intValue() > DETECT_PHASE_ACTIVITY) {
					this.phasePattern |= 0x02;
				} else {
					this.phasePattern &= ~0x02;
				}
			} else {
				this.phasePattern &= ~0x02;
			}
			curL2.setNextValue(current);
			this.updatePhases();
		});
		this.channel(EvcsAlfen.ChannelId.RAW_CURRENT_L3).onUpdate(newValue -> {
			Integer current = 0;
			if (newValue.isDefined()) {
				current = TypeUtils.getAsType(OpenemsType.INTEGER, newValue.get());
				if (current.intValue() > DETECT_PHASE_ACTIVITY) {
					this.phasePattern |= 0x04;
				} else {
					this.phasePattern &= ~0x04;
				}
			} else {
				this.phasePattern &= ~0x04;
			}
			curL3.setNextValue(current);
			this.updatePhases();
		});

		this.channel(EvcsAlfen.ChannelId.MODE_3_STATE).onChange((oldValue, newValue) -> {
			if (!oldValue.isDefined() || !newValue.isDefined()) {
				return;
			}
			var oldState = (String) oldValue.get();
			var newState = (String) newValue.get();
			if (this.vehicleConnected(newState) && !this.vehicleConnected(oldState)) {
				this.energyAtSessionStart = this.getActiveConsumptionEnergy().get();
			}
		});

	}

	private void updatePhases() {
		var bitCount = Integer.bitCount(this.phasePattern);
		if (bitCount == 0) {
			this._setPhases(3);
		} else {
			this._setPhases(bitCount);
		}
	}

	private boolean vehicleConnected(String mode3State) {
		return mode3State.length() == 2;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.convertStatus();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateSessionEnergy();
			this.updateCommunicationState();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.writeHandler.run();
		}
	}

	private void convertStatus() {

		Value<?> mode3StateVal = this.channel(EvcsAlfen.ChannelId.MODE_3_STATE).getNextValue();
		if (!mode3StateVal.isDefined()) {
			this._setStatus(Status.UNDEFINED);
			return;
		}

		var mode3State = (String) mode3StateVal.get();

		if (mode3State.startsWith("A") || mode3State.startsWith("E")) {
			this._setStatus(Status.NOT_READY_FOR_CHARGING);
			return;
		}
		if (mode3State.startsWith("B") || (mode3State.equals("C1")) || (mode3State.equals("D1"))) {
			this._setStatus(Status.READY_FOR_CHARGING);
			return;
		}
		if (mode3State.endsWith("2")) {
			this._setStatus(Status.CHARGING);
			return;
		}
		if (mode3State.equals("F")) {
			this._setStatus(Status.ERROR);
			this.channel(EvcsAlfen.ChannelId.ERROR).setNextValue(Level.FAULT);
			return;
		}

		this._setStatus(Status.UNDEFINED);

	}

	private void calculateSessionEnergy() {
		var sessionEnergy = (int) (this.getActiveConsumptionEnergy().orElse(0L) - this.energyAtSessionStart);
		this._setEnergySession(sessionEnergy);
	}

	private void updateCommunicationState() {
		if (this.getModbusCommunicationFailed()) {
			this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(Level.FAULT);
		} else {
			this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(Level.OK);
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(300, Priority.HIGH, //
						this.m(EvcsAlfen.ChannelId.METER_STATE, new UnsignedWordElement(300)), //
						this.m(EvcsAlfen.ChannelId.METER_LAST_VALUE_TIMESTAMP, new UnsignedQuadruplewordElement(301)), //
						this.m(EvcsAlfen.ChannelId.METER_TYPE, new UnsignedWordElement(305)), //
						this.m(EvcsAlfen.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(306)), //
						this.m(EvcsAlfen.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(308)), //
						this.m(EvcsAlfen.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(310)), //
						new DummyRegisterElement(312, 317), //
						this.m(EvcsAlfen.ChannelId.CURRENT_N, new FloatDoublewordElement(318)), //
						this.m(EvcsAlfen.ChannelId.RAW_CURRENT_L1, new FloatDoublewordElement(320), //
								ElementToChannelConverter.SCALE_FACTOR_3), //
						this.m(EvcsAlfen.ChannelId.RAW_CURRENT_L2, new FloatDoublewordElement(322), //
								ElementToChannelConverter.SCALE_FACTOR_3), //
						this.m(EvcsAlfen.ChannelId.RAW_CURRENT_L3, new FloatDoublewordElement(324), //
								ElementToChannelConverter.SCALE_FACTOR_3), //
						this.m(Evcs.ChannelId.CURRENT, new FloatDoublewordElement(326)), //
						this.m(EvcsAlfen.ChannelId.POWER_FACTOR_L1, new FloatDoublewordElement(328)), //
						this.m(EvcsAlfen.ChannelId.POWER_FACTOR_L2, new FloatDoublewordElement(330)), //
						this.m(EvcsAlfen.ChannelId.POWER_FACTOR_L3, new FloatDoublewordElement(332)), //
						this.m(EvcsAlfen.ChannelId.POWER_FACTOR_SUM, new FloatDoublewordElement(334)), //
						this.m(EvcsAlfen.ChannelId.FREQUENCY, new FloatDoublewordElement(336)), //
						this.m(EvcsAlfen.ChannelId.CHARGE_POWER_L1, new FloatDoublewordElement(338)), //
						this.m(EvcsAlfen.ChannelId.CHARGE_POWER_L2, new FloatDoublewordElement(340)), //
						this.m(EvcsAlfen.ChannelId.CHARGE_POWER_L3, new FloatDoublewordElement(342)), //
						this.m(Evcs.ChannelId.CHARGE_POWER, new FloatDoublewordElement(344)), //
						new DummyRegisterElement(346, 351), //
						this.m(EvcsAlfen.ChannelId.APPARENT_POWER_SUM, new FloatDoublewordElement(352)), //
						new DummyRegisterElement(354, 359), //
						this.m(EvcsAlfen.ChannelId.REACTIVE_POWER_SUM, new FloatDoublewordElement(360))), //

				new FC3ReadRegistersTask(374, Priority.LOW, //
						this.m(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatQuadruplewordElement(374))), //

				new FC3ReadRegistersTask(1200, Priority.HIGH, //
						this.m(EvcsAlfen.ChannelId.AVAILABILITY, new UnsignedWordElement(1200)), //
						this.m(EvcsAlfen.ChannelId.MODE_3_STATE, new StringWordElement(1201, 5)), //
						this.m(EvcsAlfen.ChannelId.ACTUAL_APPLIED_MAX_CURRENT, new FloatDoublewordElement(1206)), //
						this.m(EvcsAlfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT_VALID_TIME,
								new UnsignedDoublewordElement(1208)), //
						this.m(EvcsAlfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT, new FloatDoublewordElement(1210)), //
						this.m(EvcsAlfen.ChannelId.ACTIVE_LOAD_BALANCING_SAFE_CURRENT,
								new FloatDoublewordElement(1212)), //
						this.m(EvcsAlfen.ChannelId.MODBUS_SLAVE_RECEIVED_SETPOINT_ACCOUNTED_FOR,
								new UnsignedWordElement(1214)), //
						this.m(EvcsAlfen.ChannelId.CHARGE_USING_1_OR_3_PHASES, new UnsignedWordElement(1215))), //

				new FC16WriteRegistersTask(1210, //
						this.m(EvcsAlfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT, new FloatDoublewordElement(1210))) //
		);

	}

	@Override
	public String debugLog() {
		return this.getState() + "," + this.getStatus() + "," + this.getChargePower();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return this.config.minHwPower();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return this.config.maxHwPower();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
		try {
			var phases = this.getPhases().getValue();
			var current = EvcsUtils.powerToCurrentInMilliampere(power, phases);
			var floatCurrent = (float) current / 1000;
			this.setModbusSlaveMaxCurrent(floatCurrent);
			this.setModbusSlaveMaxCurrentWriteValue(floatCurrent);
			return true;
		} catch (OpenemsNamedException e) {
			throw new OpenemsException("Could not apply charge power to " + this.id());
		}
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		return this.applyChargePowerLimit(0);
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		// TODO Needs to be tested
		return 10;
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

}
