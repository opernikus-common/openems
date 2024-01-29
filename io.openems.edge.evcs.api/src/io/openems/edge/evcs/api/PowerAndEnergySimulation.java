package io.openems.edge.evcs.api;

import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

/**
 * This class is used to simulate the power values and subsequently the energy
 * values, if no meter for the Evcs is provided. It assumes that the set charge
 * power is the actual charge power.
 * 
 * <p>
 * Call the {@link #update()} method on each cycle - e.g. via an
 * {@link EventHandler}.
 * 
 * <p>
 * Ideally, the Evcs implements the {@link TimedataProvider}. Otherwise, the
 * energy values will not be set.
 */
public class PowerAndEnergySimulation {

	private final ManagedEvcs parent;

	private final CalculateEnergyFromPower calculateConsumptionEnergy;

	public PowerAndEnergySimulation(ManagedEvcs parent) {
		this.parent = parent;
		if (parent instanceof TimedataProvider td) {
			this.calculateConsumptionEnergy = new CalculateEnergyFromPower(td,
					Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY);
		} else {
			this.calculateConsumptionEnergy = null;
		}
	}

	/**
	 * Updates the channels of the Evcs assuming the set charge power is the charge
	 * power.
	 */
	public void update() {
		if (this.parent.getStatus().equals(Status.CHARGING)) {
			var power = this.parent.getSetChargePowerLimit().orElse(0);
			this.parent._setChargePower(power);
			var current = EvcsUtils.powerToCurrentInMilliampere(power, 1);
			this.parent._setCurrent(current);
			var phases = this.parent.getPhases().getValue();
			this.setPhaseCurrents(phases, current);
			if (this.calculateConsumptionEnergy != null) {
				this.calculateConsumptionEnergy.update(power);
			}
		} else {
			this.parent._setChargePower(0);
			this.parent._setCurrent(0);
			this.setPhaseCurrents(3, 0);
			if (this.calculateConsumptionEnergy != null) {
				this.calculateConsumptionEnergy.update(0);
			}
		}
	}

	/**
	 * Sets the phase currents from the current and the phases, taking into account
	 * the phase rotation.
	 * 
	 * @param phases  the number of phases
	 * @param current the total current
	 */
	private void setPhaseCurrents(int phases, int current) {
		var currentPerPhase = TypeUtils.divide(current, phases);
		var rotation = this.parent.getPhaseRotation();
		switch (phases) {
		case 1 -> {
			this.parent.channel(rotation.getFirstPhase()).setNextValue(currentPerPhase);
			this.parent.channel(rotation.getSecondPhase()).setNextValue(0);
			this.parent.channel(rotation.getThirdPhase()).setNextValue(0);
		}
		case 2 -> {
			this.parent.channel(rotation.getFirstPhase()).setNextValue(currentPerPhase);
			this.parent.channel(rotation.getSecondPhase()).setNextValue(currentPerPhase);
			this.parent.channel(rotation.getThirdPhase()).setNextValue(0);
		}
		case 3 -> {
			this.parent.channel(rotation.getFirstPhase()).setNextValue(currentPerPhase);
			this.parent.channel(rotation.getSecondPhase()).setNextValue(currentPerPhase);
			this.parent.channel(rotation.getThirdPhase()).setNextValue(currentPerPhase);
		}
		}
	}

}
