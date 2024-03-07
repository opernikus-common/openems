package io.openems.edge.evcs.cluster.chargemanagement;

import java.util.List;

import org.osgi.service.component.ComponentContext;

import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.channel.calculate.CalculateLongSum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;

/**
 * Functionality to behave like an EVCS.
 */
public abstract class AbstractManagedEvcs extends AbstractOpenemsComponent
		implements EvcsClusterChargeMgmt, Evcs, OpenemsComponent {

	protected Config config;

	protected abstract List<ClusterEvcs> getClusteredEvcss();

	protected abstract SupplyCableConstraints getCableConstraints();

	protected AbstractManagedEvcs(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	protected void deactivate() {
		super.deactivate();
	}

	protected void updateMinMaxChannels(Integer clusterMaxPower) {
		// set min max limits defined by power of connected evcss
		if (clusterMaxPower < Evcs.DEFAULT_MINIMUM_HARDWARE_POWER) {
			this.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER).setNextValue(clusterMaxPower);
		} else {
			this.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER).setNextValue(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
		}
		this.channel(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER).setNextValue(clusterMaxPower);
	}

	protected void selfEvcsResetChannels() {
		this._setChargePower(0);
		this._setPhases(Phases.THREE_PHASE);
		this._setPhaseRotation(PhaseRotation.L1_L2_L3);

		// set fixed min max limits defined within configuration
		this._setFixedMinimumHardwarePower(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
		this._setFixedMaximumHardwarePower(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
		this._setMaximumPower(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
		this._setMinimumPower(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
		this.updateMinMaxChannels(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
	}

	protected void selfEvcsInit(boolean runOnce) {

		this.selfEvcsResetChannels();

		if (runOnce) {
			Evcs.addCalculatePowerLimitListeners(this);
			this.selfEvcsInstallChargePowerLimitListener();
		}
	}

	private void selfEvcsInstallChargePowerLimitListener() {
		// this.getSetChargePowerLimitChannel().onSetNextWrite(value -> {
		// if (value != null) {
		//
		// // TODO high prio
		// // 1) safetyPower als temporaere variable activeSafetyPower anlegen und mit
		// // safetypower initialisieren.
		// // 2) alle Zugriffe auf safetyPower durch Zugriffe auf activeSafetyPower
		// // ersetzen.
		// // 3) activeSafetyPower auf value setzen.
		// // 4) targetPower auf calculateMinDistanceToSafetyPower() setzen
		// // var safetyValue = value;
		// // var targetValue = this.getCluster().minDistanceToSafetyPower();
		// //
		// // warum?
		// // wenn wir von einem anderen Cluster gesteuert werden, muessen wir Limits
		// // beachten
		// // der andere Cluster geht immer bis an safetyLimit heran
		// // wir allerdings haben einen kleinsten regelschritt
		// // dieser ist = 4 charger * 3phasig * 1A = 12A = 2.8kW * 2 wegen
		// Reaktionszeit
		// // cluster
		// // damit muessen wir soviel unter dem safetyLimit sein um nicht dauernd
		// // umschalten zu muessen
		// // dieser Wert "kleinster regelschritt" sollte in grafana visualisiert werden
		// // TODO sollten wir kalkulieren aus SUM (evcs.getPowerPrecision)
		// value -= 5400;
		// value = Math.max(0, value);
		// // safetyLimit muss !=
		//
		// this._setSetChargePowerLimit(Math.min(value,
		// this.getCableConstraints().getMaxPossiblePower()));
		// } else {
		// this._setSetChargePowerLimit(this.getCableConstraints().getMaxPossiblePower());
		// }
		// });
	}

	protected void selfEvcsUpdateChannels() {
		final var chargePower = new CalculateIntegerSum();
		final var currentL1 = new CalculateIntegerSum();
		final var currentL2 = new CalculateIntegerSum();
		final var currentL3 = new CalculateIntegerSum();
		final var activeConumptionEnergy = new CalculateLongSum();

		for (ClusterEvcs evcs : this.getClusteredEvcss()) {
			chargePower.addValue(evcs.getChargePowerChannel());
			currentL1.addValue(evcs.getCurrentL1Channel());
			currentL2.addValue(evcs.getCurrentL2Channel());
			currentL3.addValue(evcs.getCurrentL3Channel());
			activeConumptionEnergy.addValue(evcs.getActiveConsumptionEnergyChannel());
		}
		this._setChargePower(chargePower.calculate());
		this._setCurrentL1(currentL1.calculate());
		this._setCurrentL2(currentL2.calculate());
		this._setCurrentL3(currentL3.calculate());
		this._setActiveConsumptionEnergy(activeConumptionEnergy.calculate());

		if (this.getAllowCharging().orElse(false)) {
			this._setStatus(Status.CHARGING);
		} else {
			this._setStatus(Status.NOT_READY_FOR_CHARGING);
		}
		if (this.hasFaults()) {
			this._setStatus(Status.ERROR);
		}
	}

}
