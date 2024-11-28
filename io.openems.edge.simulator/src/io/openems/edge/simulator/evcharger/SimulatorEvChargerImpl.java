package io.openems.edge.simulator.evcharger;

import static io.openems.edge.common.cycle.Cycle.DEFAULT_CYCLE_TIME;

import java.io.IOException;
import java.util.function.Consumer;

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
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.Triple;
import io.openems.edge.evcharger.api.EvCharger;
import io.openems.edge.evcharger.api.EvChargerTools;
import io.openems.edge.evcharger.api.ManageableEvCharger;
import io.openems.edge.evcharger.api.data.ChargingType;
import io.openems.edge.evcharger.api.data.Iec62196Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.EvCharger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class SimulatorEvChargerImpl extends AbstractOpenemsComponent implements SimulatorEvCharger, ManageableEvCharger,
		EvCharger, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Cycle cycle;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private SimulatorDatasource datasource;
	private EvChargerSimulation simulation;
	private Config config;
	private Triple<Consumer<Value<Integer>>, Consumer<Value<Integer>>, Consumer<Value<Integer>>> rawCurrentListener;

	public SimulatorEvChargerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvCharger.ConfigChannelId.values(), //
				EvCharger.RawChannelId.values(), //
				EvCharger.ChannelId.values(), //
				ManageableEvCharger.ChannelId.values(), //
				SimulatorEvCharger.ChannelId.values() //
		);
		this.simulation = new EvChargerSimulation();
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws IOException, OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);

		ElectricityMeter.calculateSumCurrentFromPhases(this);
		EvChargerTools.addActiveConsumptionEnergyListener(this);
		EvChargerTools.addRawChargePowerListener(this);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);

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
		default -> {
		}
		}
	}

	private void applyConfig(Config config) throws OpenemsNamedException {
		this.config = config;
		this.datasource = this.componentManager.getComponent(config.datasource_id());
		this.simulation.reconfigure(this.getCycleTime(), this.config.responsiveness(),
				this.config.phaseLimitationAvailable());

		// following values will not change over time
		this._setChargingType(ChargingType.AC);
		this._setMaxCurrent(this.config.maxCurrent());
		this._setMinCurrent(this.config.minCurrent());
		this._setRawConsumptionEnergySession(0L);
		this._setIec62196Status(Iec62196Status.NO_VEHICLE);

		// Readd RawCurrent-Listeners as phaseRotation may have been modified.
		EvChargerTools.removeRawCurrentListener(this, this.rawCurrentListener);
		this.rawCurrentListener = EvChargerTools.addRawCurrentListener(this, this.config.phaseRotation());
		this._setPhaseRotation(this.config.phaseRotation());

	}

	@Override
	public void applyCurrent(int current, int phasesLimit) throws Exception {
		this.simulation.applyCurrent(current, phasesLimit);

		this._setRawCurrentL1(this.simulation.getRawCurrentL1());
		this._setRawCurrentL2(this.simulation.getRawCurrentL2());
		this._setRawCurrentL3(this.simulation.getRawCurrentL3());
		this._setRawChargePower(this.simulation.getRawChargePower());
		this._setIec62196Status(this.simulation.getRawStatus());
		this._setChargingstationCommunicationFailed(this.simulation.getChargingCommunicationFailed());

	}

	@Override
	public boolean isNumberOfPhasesLimitable() {
		return this.config.phaseLimitationAvailable();
	}

	private void updateChannels() {
		// simulate charge behavior
		var eventType = DataSourceEventType
				.fromInt(this.datasource.getValue(OpenemsType.INTEGER, new ChannelAddress(this.id(), "EventType")));
		Integer phases = this.datasource.getValue(OpenemsType.INTEGER, new ChannelAddress(this.id(), "Phases"));
		Integer maxCurrent = (Integer) this.datasource.getValue(OpenemsType.INTEGER,
				new ChannelAddress(this.id(), "MaxCurrent"));
		maxCurrent = maxCurrent == null ? null : maxCurrent * 1000;
		// Integer energyLimit = this.datasource.getValue(OpenemsType.INTEGER,
		// new ChannelAddress(this.id(), "EnergyLimit"));
		// try {
		// TODO this must be passed to genericmanagedEvChargerImpl
		// this.setEnergyLimit(energyLimit);
		// this._setSetEnergyLimit(energyLimit);
		// } catch (OpenemsNamedException e) {
		// TODO set data invalid state
		// e.printStackTrace();
		// }

		this.simulation.update(eventType, phases, maxCurrent);

		// TODO remove
		// try {
		// this.applyCurrent(0, 3);
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		// TODO wer ruft applyCurrent auf
	}

	private int getCycleTime() {
		return this.cycle != null ? this.cycle.getCycleTime() : DEFAULT_CYCLE_TIME;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		return "" + this.getRawChargePower() //
				+ ", " + this.getIec62196Status() //
		;
	}

}
