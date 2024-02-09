package io.openems.edge.simulator.meter.grid.reacting;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

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

import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.MetaEss;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.api.MetaEvcs;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.VirtualMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.GridMeter.Reacting", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=GRID" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class SimulatorGridMeterReactingImpl extends AbstractOpenemsComponent
		implements SimulatorGridMeterReacting, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private final CopyOnWriteArraySet<ManagedSymmetricEss> symmetricEsss = new CopyOnWriteArraySet<>();
	private final CopyOnWriteArraySet<Evcs> evcss = new CopyOnWriteArraySet<>();
	private final CopyOnWriteArraySet<ElectricityMeter> meters = new CopyOnWriteArraySet<>();

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;
	private boolean filterNotMeteredMeters;

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	private void addEss(ManagedSymmetricEss ess) {
		this.symmetricEsss.add(ess);
		ess.getActivePowerChannel().onSetNextValue(this.updateChannelsCallback);
	}

	protected void removeEss(ManagedSymmetricEss ess) {
		ess.getActivePowerChannel().removeOnSetNextValueCallback(this.updateChannelsCallback);
		this.symmetricEsss.remove(ess);
	}

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	protected void addEvcs(Evcs evcs) {
		if (evcs instanceof MetaEvcs || evcs.serviceFactoryPid().startsWith("Evcs.Cluster")) {
			return;
		}
		this.evcss.add(evcs);
		evcs.getChargePowerChannel().onSetNextValue(this.updateChannelsCallback);
	}

	protected void removeEvcs(Evcs evcs) {
		if (evcs instanceof MetaEvcs) {
			return;
		}
		evcs.getChargePowerChannel().removeOnSetNextValueCallback(this.updateChannelsCallback);
		this.evcss.remove(evcs);
	}

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(&(enabled=true)(!(service.factoryPid=Simulator.GridMeter.Reacting)))")
	private void addMeter(ElectricityMeter meter) {
		this.meters.add(meter);
		meter.getActivePowerChannel().onSetNextValue(this.updateChannelsCallback);
	}

	protected void removeMeter(ElectricityMeter meter) {
		meter.getActivePowerChannel().removeOnSetNextValueCallback(this.updateChannelsCallback);
		this.meters.remove(meter);
	}

	public SimulatorGridMeterReactingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SimulatorGridMeterReacting.ChannelId.values() //
		);

		// Automatically calculate L1/l2/L3 values from sum
		this._setVoltageL1(Evcs.DEFAULT_VOLTAGE * 1000);
		this._setVoltageL2(Evcs.DEFAULT_VOLTAGE * 1000);
		this._setVoltageL3(Evcs.DEFAULT_VOLTAGE * 1000);
		this._setFrequency(50_000);

	}

	@Activate
	private void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.filterNotMeteredMeters = config.filterNotMeteredMeters();
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
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			break;
		}
	}

	private final Consumer<Value<Integer>> updateChannelsCallback = value -> {
		Integer sum = 0;
		Integer sumL1 = 0;
		Integer sumL2 = 0;
		Integer sumL3 = 0;

		for (ManagedSymmetricEss ess : this.symmetricEsss) {
			if (ess instanceof MetaEss) {
				// ignore this Ess
				continue;
			}
			sum = subtract(sum, ess.getActivePowerChannel().getNextValue().get());
		}
		for (Evcs evcs : this.evcss) {
			if (evcs instanceof MetaEvcs) {
				// ignore this Evcs
				continue;
			}
			sum = add(sum, evcs.getChargePowerChannel().getNextValue().orElse(0));
			var currL1 = evcs.getCurrentL1Channel().getNextValue().orElse(0);
			var currL2 = evcs.getCurrentL2Channel().getNextValue().orElse(0);
			var currL3 = evcs.getCurrentL3Channel().getNextValue().orElse(0);
			if (currL1 == 0 && currL2 == 0 && currL3 == 0) {
				var power = evcs.getChargePowerChannel().getNextValue().orElse(0);
				currL1 = power * 1000 / (Evcs.DEFAULT_VOLTAGE * 3);
				currL2 = currL1;
				currL3 = currL1;
			}

			sumL1 = add(sumL1, EvcsUtils.currentInMilliampereToPower(currL1, 1));
			sumL2 = add(sumL2, EvcsUtils.currentInMilliampereToPower(currL2, 1));
			sumL3 = add(sumL3, EvcsUtils.currentInMilliampereToPower(currL3, 1));
		}
		for (var m : this.meters) {
			try {
				switch (m.getMeterType()) {
				case GRID:
					// ignore
					break;
				case CONSUMPTION_METERED:
				case CONSUMPTION_NOT_METERED:
					if (m instanceof VirtualMeter vm) {
						if (!vm.addToSum()) {
							// ignore
							break;
						}
					}
					// TODO brauchen wir diese Configoption jetzt noch?
					if (this.filterNotMeteredMeters) {
						break;
					}
					sum = add(sum, m.getActivePowerChannel().getNextValue().get());
					sumL1 = add(sumL1, m.getActivePowerL1Channel().getNextValue().get());
					sumL2 = add(sumL2, m.getActivePowerL2Channel().getNextValue().get());
					sumL3 = add(sumL3, m.getActivePowerL3Channel().getNextValue().get());
					break;
				case PRODUCTION:
				case PRODUCTION_AND_CONSUMPTION:
					sum = subtract(sum, m.getActivePowerChannel().getNextValue().get());
					sumL1 = subtract(sumL1, m.getActivePowerL1Channel().getNextValue().get());
					sumL2 = subtract(sumL2, m.getActivePowerL2Channel().getNextValue().get());
					sumL3 = subtract(sumL3, m.getActivePowerL3Channel().getNextValue().get());
					break;
				}
			} catch (NullPointerException e) {
				; // ignore
			}
		}

		this._setActivePower(sum);
		this._setActivePowerL1(sumL1);
		this._setActivePowerL2(sumL2);
		this._setActivePowerL3(sumL3);

		var current = TypeUtils.divide(sum * 1000, 230);
		var currentL1 = TypeUtils.divide(sumL1 * 1000, 230);
		var currentL2 = TypeUtils.divide(sumL2 * 1000, 230);
		var currentL3 = TypeUtils.divide(sumL3 * 1000, 230);

		this._setCurrent(current);
		this._setCurrentL1(currentL1);
		this._setCurrentL2(currentL2);
		this._setCurrentL3(currentL3);

	};

	private static Integer add(Integer sum, Integer activePower) {
		if (activePower == null && sum == null) {
			return null;
		}
		if (activePower == null) {
			return sum;
		} else if (sum == null) {
			return activePower;
		} else {
			return sum + activePower;
		}
	}

	private static Integer subtract(Integer sum, Integer activePower) {
		if (activePower == null && sum == null) {
			return null;
		}
		if (activePower == null) {
			return sum;
		} else if (sum == null) {
			return activePower * -1;
		} else {
			return sum - activePower;
		}
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

	@Override
	public String debugLog() {
		return this.getActivePower().asString();
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			// Not available
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
			// Buy-From-Grid
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			// Sell-To-Grid
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(activePower * -1);
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
