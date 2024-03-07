package io.openems.edge.evcs.compleo;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ElementToChannelScaleFactorConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.timer.Timer;
import io.openems.edge.common.timer.TimerManager;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.PowerAndEnergySimulation;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Compleo.Eco20", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class EvcsCompleoImpl extends AbstractOpenemsModbusComponent
		implements EvcsCompleo, ManagedEvcs, Evcs, ModbusComponent, TimedataProvider, EventHandler, OpenemsComponent {

	private static final int PILOTSIGNAL_DEACTIVATION_TIME = 10; // s

	private final Logger log = LoggerFactory.getLogger(EvcsCompleoImpl.class);

	// TODO Rausziehen, wird auch in HardyBarth, Stöhr,... benutzt
	private final ElementToChannelConverter statusElementToChannelConverter = new ElementToChannelConverter(value -> {
		var intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
		if (intValue != null) {
			// see Table 8-3 of attached documentation
			return switch (intValue) {
			case 65 -> {
				// A - Device is in status A, no vehicle connected.
				this._setPlug(Plug.UNPLUGGED);
				this.isCharging = false;
				this.lastEvcsStatus = Status.NOT_READY_FOR_CHARGING;
				yield Status.NOT_READY_FOR_CHARGING;
			}
			case 66 -> {
				// B - Device is in status B, vehicle connected, no charging process.
				this._setPlug(Plug.PLUGGED);
				if (this.isCharging) {
					this.isCharging = false;
					this.lastEvcsStatus = Status.CHARGING_FINISHED;
					yield this.lastEvcsStatus;
				}
				if (this.lastEvcsStatus == Status.CHARGING_FINISHED) {
					yield this.lastEvcsStatus;
				}
				this.isCharging = false;
				yield Status.READY_FOR_CHARGING;
			}
			case 67, 68 -> {
				// C - Device is in status C, charging process can take place.
				// D - Device is in status D, charging process can take place.
				this._setPlug(Plug.PLUGGED);
				this.isCharging = true;
				this.lastEvcsStatus = Status.CHARGING;
				yield Status.CHARGING;
			}
			case 69, 70 -> {
				// E - Device is in status E, error or charging station not ready.
				// F - Device is in status F, charging station not available for charging
				// processes.
				this._setPlug(Plug.UNDEFINED);
				this.isCharging = false;
				this.lastEvcsStatus = Status.ERROR;
				yield Status.ERROR;
			}
			default -> {
				this._setPlug(Plug.UNDEFINED);
				yield Status.UNDEFINED;
			}
			};
		}
		this._setPlug(Plug.UNDEFINED);
		this.isCharging = false;
		this.lastEvcsStatus = Status.UNDEFINED;
		return Status.UNDEFINED;
	});

	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	/**
	 * Processes the controller's writes to this evcs component.
	 */
	private final WriteHandler writeHandler = new WriteHandler(this);

	private final PowerAndEnergySimulation simulation = new PowerAndEnergySimulation(this);
	private final CalculateEnergySession calculateEnergySession = new CalculateEnergySession(this);

	private Config config;
	private Timer startStopTimer;
	private Timer pilotSignalDeactivationTimer;

	private Status lastEvcsStatus = Status.UNDEFINED;
	private boolean isCharging = false;
	private boolean stopRequested = true;
	private boolean oldStopRequested = true;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private ElectricityMeter meter;

	@Reference
	private EvcsPower evcsPower;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private TimerManager timerManager;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvcsCompleoImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				EvcsCompleo.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		this.isCharging = false;
		this._setPriority(config.priority());
		this.stopRequested = true;
		this.oldStopRequested = true;
		this.startStopTimer = this.timerManager.getTimerByTime(this.channel(EvcsCompleo.ChannelId.START_STOP_TIMER),
				config.commandStartStopDelay() + (int) (30.0 * Math.random()));
		this.pilotSignalDeactivationTimer = this.timerManager.getTimerByTime(
				this.channel(EvcsCompleo.ChannelId.PILOT_SIGNAL_DEACTIVATION_TIMER), PILOTSIGNAL_DEACTIVATION_TIME);
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		if (config.meteringType() == MeteringType.WITH_EXTERNAL_METER) {
			if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", config.meter_id())) {
				return;
			}
		}
		this.installListeners();
		this.setPowerLimits();
		try {
			this.enableCharging();
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Could not start charging station.");
			e.printStackTrace();
		}
		this.getModbusCommunicationFailedChannel().onSetNextValue(
				value -> this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(value));
	}

	@Override
	@Deactivate
	protected void deactivate() {
		try {
			this.disableCharging();
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Could not stop charging station.");
			e.printStackTrace();
		}
		super.deactivate();
	}

	private void enableCharging() throws OpenemsNamedException {
		this.setEnableCharging(true);
	}

	private void disableCharging() throws OpenemsNamedException {
		this.setEnableCharging(false);
	}

	private void setPowerLimits() {
		this._setChargingType(ChargingType.AC);
		this._setPowerPrecision(230);
		// TODO update phases continouosly from meter
		var phases = 3;
		this._setPhases(phases);
		this._setFixedMinimumHardwarePower(EvcsUtils.currentInMilliampereToPower(this.config.minHwCurrent(), phases));
		this._setFixedMaximumHardwarePower(EvcsUtils.currentInMilliampereToPower(this.config.maxHwCurrent(), phases));
	}

	private void installListeners() {
		Evcs.addCalculatePowerLimitListeners(this);
		this.getCableCurrentLimitChannel().onUpdate(newValue -> {
			if (!newValue.isDefined() || newValue.get() < this.config.minHwCurrent() / 1000) {
				return;
			}
			if (newValue.get() * 1000 < this.config.maxHwCurrent()) {
				this._setMaximumPower(newValue.get() * this.getPhases().getValue() * 230);
			}
		});
		this.getStatusChannel().onUpdate(status -> {
			this.channel(EvcsCompleo.ChannelId.CHARGINGSTATION_STATE_ERROR) //
					.setNextValue(status.get() == Status.ERROR);
		});
		if (this.config.meteringType() == MeteringType.WITH_EXTERNAL_METER) {
			this.addMeterListeners();
		}

	}

	private void addMeterListeners() {
		if (this.meter != null) {
			Evcs.addMeterListeners(this, this.meter);
			this.addCopyListener(this.meter.getVoltageL1Channel(), //
					this.channel(EvcsCompleo.ChannelId.VOLTAGE_L1));
			this.addCopyListener(this.meter.getVoltageL2Channel(), //
					this.channel(EvcsCompleo.ChannelId.VOLTAGE_L2));
			this.addCopyListener(this.meter.getVoltageL3Channel(), //
					this.channel(EvcsCompleo.ChannelId.VOLTAGE_L3));
			this.addCopyListener(this.meter.getReactivePowerChannel(), //
					this.channel(EvcsCompleo.ChannelId.REACTIVE_POWER));
		}
	}

	private void addCopyListener(Channel<?> source, Channel<?> target) {
		source.onSetNextValue(value -> {
			target.setNextValue(value);
		});
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			if (this.config.meteringType() == MeteringType.WITHOUT_METER) {
				this.simulation.update();
			}
			if (this.config.meteringType() != MeteringType.WITH_INTEGRATED_METER) {
				this.calculateEnergySession.update(this.isCharging);
			}
			this.evcsNotStartingWorkaround();
		}
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> {
			this.writeHandler.run();
		}
		}

	}

	/**
	 * Corsa E does not start charging after stopRequested = true ... stopRequested
	 * = false cyclce. This workaround disables pilotsignal for some time. This
	 * forces Corsa E to wake up and start charging
	 */
	private void evcsNotStartingWorkaround() {
		if (!this.config.restartPilotSignal()) {
			return;
		}
		try {
			// detect if charging station should come up again
			if (this.oldStopRequested && !this.stopRequested) {
				// disable pilotsignal
				this.setModifyChargingStationAvailability(false);
				this.pilotSignalDeactivationTimer.reset();
			}
			if (this.pilotSignalDeactivationTimer.checkAndReset()) {
				// activate pilotsignal
				this.setModifyChargingStationAvailability(true);
			}
			this.oldStopRequested = this.stopRequested;
		} catch (Exception e) {
			this.logError(this.log, "Ex in evcsNotStartingWorkaround: " + e.getMessage());
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		AbstractModbusElement<?, ?, ?> activePower;
		AbstractModbusElement<?, ?, ?> sessionEnergy;
		AbstractModbusElement<?, ?, ?> totalEnergy;

		AbstractModbusElement<?, ?, ?> currentL1;
		AbstractModbusElement<?, ?, ?> currentL2;
		AbstractModbusElement<?, ?, ?> currentL3;

		ElementToChannelConverter integratedMeterConverter;
		if (this.config.meteringType() == MeteringType.WITH_INTEGRATED_METER) {
			integratedMeterConverter = ElementToChannelConverter.SCALE_FACTOR_MINUS_3;

			currentL1 = this.m(Evcs.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(114), integratedMeterConverter);
			currentL2 = this.m(Evcs.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(116), integratedMeterConverter);
			currentL3 = this.m(Evcs.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(118), integratedMeterConverter);
			activePower = this.m(Evcs.ChannelId.CHARGE_POWER, new SignedDoublewordElement(120), //
					ElementToChannelConverter.SCALE_FACTOR_MINUS_3);
			totalEnergy = this.m(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(128), //
					ElementToChannelConverter.SCALE_FACTOR_3);
			sessionEnergy = this.m(Evcs.ChannelId.ENERGY_SESSION, new UnsignedDoublewordElement(132), //
					ElementToChannelConverter.SCALE_FACTOR_3);

		} else {

			currentL1 = new DummyRegisterElement(114, 115);
			currentL2 = new DummyRegisterElement(116, 117);
			currentL3 = new DummyRegisterElement(118, 119);
			activePower = new DummyRegisterElement(120, 121);
			totalEnergy = new DummyRegisterElement(128, 129);
			sessionEnergy = new DummyRegisterElement(132, 133);
			integratedMeterConverter = ElementToChannelConverter.DIRECT_1_TO_1;

		}

		return new ModbusProtocol(this,
				// see "Installing and starting up the EV Charge Control charging controller
				// user manual", phoenix contact, 2020-09-17
				// chapter 9.2 table 9.2

				new FC4ReadInputRegistersTask(100, Priority.LOW, //
						this.m(Evcs.ChannelId.STATUS, new UnsignedWordElement(100),
								this.statusElementToChannelConverter), //
						// Current carrying capacity of charging cable (Proximity) --> One of the
						// limiting factors for charge power
						this.m(EvcsCompleo.ChannelId.CABLE_CURRENT_LIMIT, new UnsignedWordElement(101)), //
						new DummyRegisterElement(102, 104), //
						this.m(EvcsCompleo.ChannelId.FIRMWARE_VERSION, new UnsignedDoublewordElement(105)), //
						new DummyRegisterElement(107), //
						this.m(EvcsCompleo.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(108), //
								integratedMeterConverter), //
						this.m(EvcsCompleo.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(110), //
								integratedMeterConverter), //
						this.m(EvcsCompleo.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(112), //
								integratedMeterConverter), //
						currentL1, //
						currentL2, //
						currentL3, //
						activePower, //
						this.m(EvcsCompleo.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(122), //
								integratedMeterConverter), //
						this.m(EvcsCompleo.ChannelId.APPARENT_POWER, new SignedDoublewordElement(124), //
								integratedMeterConverter), //
						new DummyRegisterElement(126, 127), // power factor, does not work propoerly
						totalEnergy, //
						new DummyRegisterElement(130, 131), // max power during measurement
						sessionEnergy, //
						this.m(EvcsCompleo.ChannelId.FREQUENCY, new UnsignedDoublewordElement(134), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_2), //
						this.m(EvcsCompleo.ChannelId.MAX_CURRENT_L1, new UnsignedDoublewordElement(136)), //
						this.m(EvcsCompleo.ChannelId.MAX_CURRENT_L2, new UnsignedDoublewordElement(138)), //
						this.m(EvcsCompleo.ChannelId.MAX_CURRENT_L3, new UnsignedDoublewordElement(140)) //

				), //

				new FC3ReadRegistersTask(300, Priority.HIGH, //
						this.m(EvcsCompleo.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(300), //
								new ElementToChannelScaleFactorConverter(this.config.model().scaleFactor))), //
				new FC3ReadRegistersTask(528, Priority.LOW,
						this.m(EvcsCompleo.ChannelId.DEFAULT_CHARGING_CURRENT, new UnsignedWordElement(528), //
								new ElementToChannelScaleFactorConverter(this.config.model().scaleFactor))),
				new FC5WriteCoilTask(400, //
						this.m(EvcsCompleo.ChannelId.ENABLE_CHARGING, new CoilElement(400))), //
				new FC5WriteCoilTask(402, //
						this.m(EvcsCompleo.ChannelId.MODIFY_CHARGING_STATION_AVAILABILTY, new CoilElement(402))), //
				// requires at least Wallbe FW version 1.12
				new FC6WriteRegisterTask(528, //
						this.m(EvcsCompleo.ChannelId.DEFAULT_CHARGING_CURRENT, new UnsignedWordElement(528), //
								new ElementToChannelScaleFactorConverter(this.config.model().scaleFactor))) //
		); //

		// FW Phonix Contact: V1.27 -> FIRMWARE_VERSION = 774977330
		// FW Compleo : SL-01.04.21 -> FIRMWARE_VERSION = 1280520237

	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		return this.getState() + "," + this.getStatus() + "," + this.getChargePower();
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return EvcsUtils.currentInMilliampereToPower(this.config.minHwCurrent(), 1);
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return EvcsUtils.currentInMilliampereToPower(this.config.maxHwCurrent(), 3);
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {

		if (power == 0) {
			if (!this.stopRequested) {
				// hasChanged
				this.stopRequested = true;
				this.startStopTimer.reset();
			}
		} else if (this.stopRequested) {
			if (this.startStopTimer.check()) {
				this.stopRequested = false;
			}
		}
		try {
			this.setCurrentFromPower(this.stopRequested, power);
			return true;
		} catch (OpenemsNamedException e) {
			throw new OpenemsException("Could not apply charge power to " + this.id());
		}

	}

	private void setCurrentFromPower(boolean stopRequested, int power) throws OpenemsNamedException {

		// convert power to ampere
		var phases = this.getPhases().getValue();
		var currentInMilliAmpere = EvcsUtils.powerToCurrentInMilliampere(power, phases);
		if (stopRequested) {
			currentInMilliAmpere = 0;
			this.disableCharging();
		} else {
			this.enableCharging();
		}
		this.setDefaultChargingCurrent(currentInMilliAmpere);
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
	public int getWriteInterval() {
		return 1;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 10;
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
