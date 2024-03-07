package io.openems.edge.simulator.evcs;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.filter.RampFilter;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Evcs", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class SimulatorEvcsImpl extends AbstractManagedEvcsComponent
		implements SimulatorEvcs, ManagedEvcs, Evcs, OpenemsComponent, TimedataProvider, EventHandler {

	private static final int MAX_CHANGE_PER_CYCLE = 1200;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	private ComponentManager componentManager;
	// do not use OSGI mapping here. Reason: Want to change datasource without
	// deactivating/activating component.
	
	private SimulatorDatasource datasource;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config;

	private final RampFilter rampFilter = new RampFilter(0.0f);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private LocalDateTime lastUpdate = LocalDateTime.now();
	private double exactEnergySession = 0;
	private EventType oldEventType = EventType.UNDEFINED;
	private boolean disableCharging = false;

	// Workaround für E-World
	private boolean modified = false;

	public SimulatorEvcsImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				SimulatorEvcs.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws IOException, OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.modified = false;
		this.applyConfig(config);
		this._setPowerPrecision(1);
		this._setChargingstationCommunicationFailed(false);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.modified = true;
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}


	private void applyConfig(Config config) throws OpenemsNamedException {
		this.config = config;
		this.datasource = this.componentManager.getComponent(config.datasource_id());
		this._setPhaseRotation(config.phaseRotation());
		this._setPhases(3);
		this._setPriority(config.priority());
		this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());
		this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> this.updateChannels();
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> super.handleEvent(event);
		}
	}

	private void updateChannels() {

		var eventType = EventType
				.fromInt(this.datasource.getValue(OpenemsType.INTEGER, new ChannelAddress(this.id(), "EventType")));
		Integer requiredEnergy = this.datasource.getValue(OpenemsType.INTEGER,
				new ChannelAddress(this.id(), "SetEnergyLimit"));
		try {
			this.setEnergyLimit(requiredEnergy);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
		this._setSetEnergyLimit(requiredEnergy);

		if (eventType.equals(EventType.CAR_CONNECTED)) {
			if (this.getSetChargePowerLimit().orElse(0) == 0) {
				this._setStatus(Status.CHARGING_REJECTED);
				return;
			}

			if (this.getEnergySession().orElse(0) >= requiredEnergy) {
				eventType = EventType.ENERGY_LIMIT_REACHED;
			} else {
				this.disableCharging = false;
			}
		}

		this.handleEventType(eventType);
	}

	private void handleEventType(EventType eventType) {
		this.channel(SimulatorEvcs.ChannelId.EVENT_TYPE).setNextValue(eventType);
		switch (eventType) {
		case NO_CAR_CONNECTED -> {
			this._setStatus(Status.NOT_READY_FOR_CHARGING);
			this.disableCharging = true;
		}
		case CAR_CONNECTED -> {
			if (!this.oldEventType.equals(EventType.CAR_CONNECTED)) {
				this.resetEnergy();
			}
			this._setStatus(Status.CHARGING);
			this.disableCharging = false;
		}
		case ENERGY_LIMIT_REACHED -> {
			this._setStatus(Status.CHARGING_FINISHED);
			this.disableCharging = true;
		}
		case UNDEFINED -> {
			this._setStatus(Status.ERROR);
			this.disableCharging = true;
		}
		}

		this.oldEventType = eventType;

		if (this.modified) {
			this.modified = false;
			this._setStatus(Status.NOT_READY_FOR_CHARGING);
		}
	}

	private void calculateEnergy() {
		this.calculateConsumptionEnergy.update(this.getChargePower().get());
		this.calculateEnergySession();
	}

	private void calculateEnergySession() {

		var timeDiff = ChronoUnit.MILLIS.between(this.lastUpdate, LocalDateTime.now());
		var energyTransfered = timeDiff / 1000.0 / 60.0 / 60.0 * this.getChargePower().orElse(0);

		this.exactEnergySession = this.exactEnergySession + energyTransfered;
		this._setEnergySession((int) this.exactEnergySession);

		this.lastUpdate = LocalDateTime.now();
	}

	private void resetEnergy() {
		this.exactEnergySession = 0;
		this._setEnergySession(0);
		this.lastUpdate = LocalDateTime.now();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return this.config.maxHwPower();
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return this.config.minHwPower();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return false;
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsException {

		var powerPerPhase = power / 3;
		var chargePowerPerPhase = this.getChargePower().orElse(0) / this.getPhases().getValue();

		// increase chargePowerLimit slowly
		var request = Float.valueOf(powerPerPhase);
		chargePowerPerPhase = this.rampFilter.getFilteredValueAsInteger(Float.valueOf(chargePowerPerPhase), request,
				MAX_CHANGE_PER_CYCLE);

		if (this.disableCharging) {
			chargePowerPerPhase = 0;
		}

		// gets used phases from datasource
		int phases = this.datasource.getValue(OpenemsType.INTEGER, new ChannelAddress(this.id(), "Phases"));
		this._setPhases(phases);

		Integer maxEvPower = this.datasource.getValue(OpenemsType.INTEGER, new ChannelAddress(this.id(), "MaxEvPower"));
		this.channel(SimulatorEvcs.ChannelId.MAX_EV_POWER).setNextValue(maxEvPower);
		var sentPowerPerPhase = Math.min(chargePowerPerPhase, maxEvPower / phases);

		var simulatedCurrentPerPhase = EvcsUtils.powerToCurrentInMilliampere(sentPowerPerPhase, 1); // mA
		this._setChargePower(sentPowerPerPhase * phases);

		// Current is divided on the phases 1,...,phases

		switch (phases) {
		case 0 -> {
			this.channel(this.config.phaseRotation().getFirstPhase()).setNextValue(0);
			this.channel(this.config.phaseRotation().getSecondPhase()).setNextValue(0);
			this.channel(this.config.phaseRotation().getThirdPhase()).setNextValue(0);
		}
		case 1 -> {
			this.channel(this.config.phaseRotation().getFirstPhase()).setNextValue(simulatedCurrentPerPhase);
			this.channel(this.config.phaseRotation().getSecondPhase()).setNextValue(0);
			this.channel(this.config.phaseRotation().getThirdPhase()).setNextValue(0);
		}
		case 2 -> {
			this.channel(this.config.phaseRotation().getFirstPhase()).setNextValue(simulatedCurrentPerPhase);
			this.channel(this.config.phaseRotation().getSecondPhase()).setNextValue(simulatedCurrentPerPhase);
			this.channel(this.config.phaseRotation().getThirdPhase()).setNextValue(0);
		}
		case 3 -> {
			this.channel(this.config.phaseRotation().getFirstPhase()).setNextValue(simulatedCurrentPerPhase);
			this.channel(this.config.phaseRotation().getSecondPhase()).setNextValue(simulatedCurrentPerPhase);
			this.channel(this.config.phaseRotation().getThirdPhase()).setNextValue(simulatedCurrentPerPhase);
		}
		default -> {
		}
		}
		this._setCurrent(simulatedCurrentPerPhase * phases);

		this.calculateEnergy();
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws OpenemsException {
		return this.applyChargePowerLimit(0);
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 10;
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
	public Timedata getTimedata() {
		return this.timedata;
	}

	// Cluster.Chargemanagement needs this to be true.
	@Override
	public boolean ignoreChargeState() {
		return true;
	}

	@Override
	public String debugLog() {
		return "" + this.getChargePower() //
				+ ", " + this.getStatus() //
				+ ", Phases: " + this.getPhases() //
				+ ", MaxEvPower: " //
				+ this.channel(SimulatorEvcs.ChannelId.MAX_EV_POWER).value() //
				+ ", Required Energy: " + this.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT).value() //
		;
	}

}
