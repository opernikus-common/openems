package io.openems.edge.evcharger.api;

import static io.openems.edge.evcharger.api.data.Status.CHARGING;
import static io.openems.edge.evcharger.api.data.Status.DEACTIVATED;
import static io.openems.edge.evcharger.api.data.Status.ERROR;
import static io.openems.edge.evcharger.api.data.Status.NO_VEHICLE;
import static io.openems.edge.evcharger.api.data.Status.VEHICLE_DETECTED;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.Triple;
import io.openems.edge.evcharger.api.data.PhaseRotation;
import io.openems.edge.evcharger.api.data.Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

public class EvChargerTools {

	/**
	 * Adds EvCharger raw current listeners.
	 * 
	 * @param charger       the concrete hardware implementation of the charger.
	 * @param phaseRotation the new phase rotation to apply.
	 * @return a triple including the three current callbacks.
	 */
	public static Triple<Consumer<Value<Integer>>, Consumer<Value<Integer>>, Consumer<Value<Integer>>> addRawCurrentListener(
			ManageableEvCharger charger, PhaseRotation phaseRotation) {
		var listener = new Triple<Consumer<Value<Integer>>, Consumer<Value<Integer>>, Consumer<Value<Integer>>>(val -> {
			charger.channel(phaseRotation.getOutPhase1()).setNextValue(val.get());
		}, val -> {
			charger.channel(phaseRotation.getOutPhase2()).setNextValue(val.get());
		}, val -> {
			charger.channel(phaseRotation.getOutPhase3()).setNextValue(val.get());
		});
		((IntegerReadChannel) charger.channel(phaseRotation.getInPhase1())).onSetNextValue(listener.a());
		((IntegerReadChannel) charger.channel(phaseRotation.getInPhase2())).onSetNextValue(listener.b());
		((IntegerReadChannel) charger.channel(phaseRotation.getInPhase3())).onSetNextValue(listener.c());
		return listener;
	}

	/**
	 * Removes EvCharger raw current listeners.
	 * 
	 * @param charger         the concrete hardware implementation of the charger.
	 * @param currentListener the listeners to be removed.
	 */
	public static void removeRawCurrentListener(ManageableEvCharger charger,
			Triple<Consumer<Value<Integer>>, Consumer<Value<Integer>>, Consumer<Value<Integer>>> currentListener) {
		if (currentListener == null) {
			return;
		}
		// this uses the old phase rotation from the phaseRotation channel
		((IntegerReadChannel) charger.channel(charger.getPhaseRotation().getInPhase1()))
				.removeOnSetNextValueCallback(currentListener.a());
		((IntegerReadChannel) charger.channel(charger.getPhaseRotation().getInPhase2()))
				.removeOnSetNextValueCallback(currentListener.b());
		((IntegerReadChannel) charger.channel(charger.getPhaseRotation().getInPhase3()))
				.removeOnSetNextValueCallback(currentListener.c());
	}

	/**
	 * Adds raw charge power EvCharger listeners.
	 * 
	 * @param charger the concrete hardware implementation of the charger.
	 */
	public static void addRawChargePowerListener(ManageableEvCharger charger) {
		charger.getRawChargePowerChannel().onSetNextValue(val -> {
			charger._setActivePower(val.get());
		});
		// TODO genCharger.activePowerL1,L2,L3 berechnen
	}

	/**
	 * Adds EvCharger active consumption energy listener.
	 * 
	 * @param charger the concrete evCharger.
	 */
	public static void addActiveConsumptionEnergyListener(ManageableEvCharger charger) {
		final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower((TimedataProvider) charger,
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);
		charger.getRawChargePowerChannel().onSetNextValue(val -> {
			calculateEnergy.update(charger.getRawChargePower().orElse(0));
		});

		final AtomicLong sessionStartEnergy = new AtomicLong(0L);
		final AtomicLong sessionStopEnergy = new AtomicLong(0L);
		charger.getStatusChannel().onChange((oldVal, newVal) -> {
			Status oldStat = oldVal.asEnum();
			Status newStat = newVal.asEnum();
			/*
			 * possible states NO_VEHICLE, VEHICLE_DETECTED CHARGING, CHARGING_FINISHED
			 * DEACTIVATED, ERROR
			 */
			if (newStat == VEHICLE_DETECTED //
					|| newStat == CHARGING) {
				// start session
				if (oldStat == NO_VEHICLE //
						|| oldStat == VEHICLE_DETECTED) {
					// start energy session
					sessionStartEnergy.set(charger.getActiveConsumptionEnergy().orElse(0L));
					sessionStopEnergy.set(0);
				}

			} else if (newStat == NO_VEHICLE //
					|| newStat == ERROR //
					|| newStat == DEACTIVATED) {

				// stop session
				sessionStopEnergy.set(charger.getActiveConsumptionEnergy().orElse(0L));
			}
		});
		charger.getActiveConsumptionEnergyChannel().onChange((oldVal, newVal) -> {
			var start = sessionStartEnergy.get();
			var stop = sessionStopEnergy.get();

			if (start == 0 && stop == 0) {
				charger._setConsumptionEnergySession(0);

			} else if (start != 0 && stop == 0) {
				var sessionEnergy = (int) (newVal.get() - sessionStartEnergy.get());
				charger._setConsumptionEnergySession(sessionEnergy);

			} else if (start != 0 && stop != 0) {
				charger._setConsumptionEnergySession((int) (stop - start));
			}
		});
	}

	/**
	 * Converts a given power to current in Milliampere (assuming the voltage is 230
	 * V).
	 * 
	 * @param power  in Watt
	 * @param phases active phases
	 * @return current in Milliampere
	 */
	public static int convertWattToMilliampere(int power, int phases) {
		if (phases == 0) {
			phases = 3;
		}
		return Long.valueOf(Math.round(power * 1000 / EvCharger.DEFAULT_VOLTAGE / phases)).intValue();
	}

	/**
	 * Converts a given power to current in Ampere (assuming the voltage is 230 V).
	 * 
	 * @param power  in Watt
	 * @param phases active phases
	 * @return current in Ampere
	 */
	public static int convertWattToAmpere(int power, int phases) {
		return convertWattToMilliampere(power, phases) / 1000;
	}

	/**
	 * Converts a given current in Ampere to power (assuming the voltage is 230V).
	 * 
	 * @param current in Ampere
	 * @param phases  active phases
	 * @return power in Watt
	 */
	public static int convertAmpereToWatt(int current, int phases) {
		return convertMilliampereToWatt(current * 1000, phases);
	}

	/**
	 * Converts a given current in Milliampere to power (assuming the voltage is
	 * 230V).
	 * 
	 * @param current in Milliampere
	 * @param phases  active phases
	 * @return power in Watt
	 */
	public static int convertMilliampereToWatt(int current, int phases) {
		if (phases == 0) {
			phases = 3;
		}
		var power = current * phases * EvCharger.DEFAULT_VOLTAGE;
		return power / 1000;

	}

}
