package io.openems.edge.evcs.stoehr.designtower;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.evcs.api.Phases;
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
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Stoehr.Designtower", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class EvcsDesigntowerImpl extends AbstractOpenemsModbusComponent
		implements EvcsDesigntower, ManagedEvcs, Evcs, ModbusComponent, EventHandler, OpenemsComponent {

	private static final float DETECT_PHASE_ACTIVITY = 400; // mA

	private final Logger log = LoggerFactory.getLogger(EvcsDesigntowerImpl.class);

	private Config config;

	private final ElementToChannelConverter statusConverter = new ElementToChannelConverter(value -> {
		var intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
		if (intValue != null) {
			// Status A-F corresponds to hexadecimal 0x0A-0x0F
			switch (intValue) {
				case 0x0A -> { // A - Device is in status A, no vehicle connected.
					this.isCharging = false;
					this.lastEvcsStatus = Status.NOT_READY_FOR_CHARGING;
					return Status.NOT_READY_FOR_CHARGING;
				}
				case 0x0B -> { // B - Device is in status B, vehicle connected, no charging process.
					if (this.isCharging) {
						this.isCharging = false;
						this.lastEvcsStatus = Status.CHARGING_FINISHED;
						return this.lastEvcsStatus;
					}
					if (this.lastEvcsStatus == Status.CHARGING_FINISHED) {
						return this.lastEvcsStatus;
					}
					return Status.READY_FOR_CHARGING;
				} // C - Device is in status C, charging process can take place.
				case 0x0C, 0x0D -> { // D - Device is in status D, charging process can take place.
					this.isCharging = true;
					this.lastEvcsStatus = Status.CHARGING;
					return Status.CHARGING;
				} // E - Device is in status E, error or charging station not ready.
				case 0x0E, 0x0F -> { // F - Device is in status F, charging station not available for charging
					// processes.
					this.isCharging = false;
					this.lastEvcsStatus = Status.ERROR;
					return Status.ERROR;
				}
			}
		}
		this.isCharging = false;
		this.lastEvcsStatus = Status.UNDEFINED;
		return Status.UNDEFINED;
	});

	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	/**
	 * Processes the controller's writes to this evcs component.
	 */
	private final WriteHandler writeHandler = new WriteHandler(this);

	private int phasePattern = 0;
	private boolean isCharging = false;
	private Status lastEvcsStatus = Status.UNDEFINED;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private EvcsPower evcsPower;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvcsDesigntowerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				EvcsDesigntower.ChannelId.values() //
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
		this.setPowerLimits();
		this._setIsClustered(false);
		this._setSetEnergyLimit(null);
		this.installEvcsHandlers();

		/*
		 * Calculates the maximum and minimum hardware power dynamically by listening on
		 * the fixed hardware limit and the phases used for charging
		 */
		Evcs.addCalculatePowerLimitListeners(this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void setPowerLimits() {
		this._setChargingType(ChargingType.AC);
		this._setPowerPrecision(230); // Designtower can limit power in 1 A steps, 1 A * 230V = 230W
		this._setFixedMinimumHardwarePower(
				EvcsUtils.currentInMilliampereToPower(this.config.minHwCurrent(), Phases.THREE_PHASE.getValue()));
		this._setFixedMaximumHardwarePower(
				EvcsUtils.currentInMilliampereToPower(this.config.maxHwCurrent(), Phases.THREE_PHASE.getValue()));
	}

	private void installEvcsHandlers() {
		this.channel(Evcs.ChannelId.CURRENT_L1).onUpdate(newValue -> {
			if (newValue.isDefined()) {
				var curr = (Integer) newValue.get();
				if (curr > DETECT_PHASE_ACTIVITY) {
					this.phasePattern |= 0x01;
				} else {
					this.phasePattern &= ~0x01;
				}
			} else {
				this.phasePattern &= ~0x01;
			}
			this.updatePhases();
		});
		this.channel(Evcs.ChannelId.CURRENT_L2).onUpdate(newValue -> {
			if (newValue.isDefined()) {
				var curr = (Integer) newValue.get();
				if (curr > DETECT_PHASE_ACTIVITY) {
					this.phasePattern |= 0x02;
				} else {
					this.phasePattern &= ~0x02;
				}
			} else {
				this.phasePattern &= ~0x02;
			}
			this.updatePhases();
		});
		this.channel(Evcs.ChannelId.CURRENT_L3).onUpdate(newValue -> {
			if (newValue.isDefined()) {
				var curr = (Integer) newValue.get();
				if (curr > DETECT_PHASE_ACTIVITY) {
					this.phasePattern |= 0x04;
				} else {
					this.phasePattern &= ~0x04;
				}
			} else {
				this.phasePattern &= ~0x04;
			}
			this.updatePhases();
		});

	}

	private void updatePhases() {
		var bitCount = Integer.bitCount(this.phasePattern);
		try {
			this._setPhases(bitCount);
		} catch (IllegalArgumentException e) {
			this._setPhases(3);
		}
	}

	private void updatePowerAndEnergy() {
		IntegerReadChannel l1 = this.channel(EvcsDesigntower.ChannelId.CHARGE_POWER_L1);
		IntegerReadChannel l2 = this.channel(EvcsDesigntower.ChannelId.CHARGE_POWER_L2);
		IntegerReadChannel l3 = this.channel(EvcsDesigntower.ChannelId.CHARGE_POWER_L3);
		var chargepower = (l1.getNextValue().orElse(0) + l2.getNextValue().orElse(0) + l3.getNextValue().orElse(0));
		this._setChargePower(chargepower);
		LongReadChannel energyL1 = this.channel(EvcsDesigntower.ChannelId.ENERGY_L1);
		LongReadChannel energyL2 = this.channel(EvcsDesigntower.ChannelId.ENERGY_L2);
		LongReadChannel energyL3 = this.channel(EvcsDesigntower.ChannelId.ENERGY_L3);
		var energy = (energyL1.getNextValue().orElse(0L) + energyL2.getNextValue().orElse(0L)
				+ energyL3.getNextValue().orElse(0L));
		this._setActiveConsumptionEnergy(energy);
	}

	private void setCurrentFromPower(int power) throws OpenemsNamedException {
		var phases = this.getPhases().getValue();
		var current = EvcsUtils.powerToCurrent(power, phases);
		this.setChargingCurrent(current);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
				this.updateCommunicationState();
				this.updatePowerAndEnergy();
			}
			case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> this.writeHandler.run();
		}
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
				/* USING BENDER/OMCCI */
				new FC3ReadRegistersTask(100, Priority.LOW, //
						this.m(EvcsDesigntower.ChannelId.FIRMWARE_VERSION, new StringWordElement(100, 2)) //
				), //

				new FC3ReadRegistersTask(104, Priority.LOW, //
						this.m(EvcsDesigntower.ChannelId.OCPP_CP_STATUS, new UnsignedWordElement(104)) //
				), //

				new FC3ReadRegistersTask(123, Priority.HIGH, //
						this.m(Evcs.ChannelId.STATUS, new UnsignedWordElement(123), this.statusConverter) //
				), //

				new FC3ReadRegistersTask(131, Priority.LOW, //
						this.m(EvcsDesigntower.ChannelId.SAFE_CURRENT, new UnsignedWordElement(131)) //
				), //

				new FC3ReadRegistersTask(200, Priority.HIGH, //
						this.m(EvcsDesigntower.ChannelId.ENERGY_L1, new UnsignedDoublewordElement(200)), //
						this.m(EvcsDesigntower.ChannelId.ENERGY_L2, new UnsignedDoublewordElement(202)), //
						this.m(EvcsDesigntower.ChannelId.ENERGY_L3, new UnsignedDoublewordElement(204)), //
						this.m(EvcsDesigntower.ChannelId.CHARGE_POWER_L1, new SignedDoublewordElement(206)), //
						this.m(EvcsDesigntower.ChannelId.CHARGE_POWER_L2, new SignedDoublewordElement(208)), //
						this.m(EvcsDesigntower.ChannelId.CHARGE_POWER_L3, new SignedDoublewordElement(210)), //
						this.m(Evcs.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(212)), //
						this.m(Evcs.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(214)), //
						this.m(Evcs.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(216)) //
				), //

				new FC3ReadRegistersTask(713, Priority.HIGH, //
						this.m(EvcsDesigntower.ChannelId.REQUIRED_ENERGY, new UnsignedDoublewordElement(713)), //
						this.m(EvcsDesigntower.ChannelId.MAX_EV_CURRENT, new UnsignedWordElement(715)), //
						this.m(Evcs.ChannelId.ENERGY_SESSION, new UnsignedDoublewordElement(716)), //
						this.m(EvcsDesigntower.ChannelId.CHARGING_DURATION, new UnsignedDoublewordElement(718)) //
				), //

				new FC3ReadRegistersTask(1000, Priority.HIGH, //
						this.m(EvcsDesigntower.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(1000)) //
				), //

				new FC6WriteRegisterTask(1000, //
						this.m(EvcsDesigntower.ChannelId.SET_CHARGING_CURRENT, new UnsignedWordElement(1000)) //
				) //
		); //
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
			this.setCurrentFromPower(power);
			return true;
	}


	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return EvcsUtils.currentToPower(this.config.minHwCurrent(), this.getPhases().getValue());
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return EvcsUtils.currentToPower(this.config.maxHwCurrent(), this.getPhases().getValue());
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public String debugLog() {
		return this.getState() + "," + this.getStatus() + "," + this.getChargePower();
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