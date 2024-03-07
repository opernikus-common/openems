package io.openems.edge.controller.evcs.cluster.chargemanagement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.function.Consumer;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.InvalidValueException;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Meter handles everything related to the electricity meter.
 *
 * <p>
 * Responsible for Channels
 * <ul>
 * <li>TRANSPORT_CAPACITY - max theoretically transport capacity of this cable
 * segment.
 * <li>MeterCurrentL1
 * <li>MeterCurrentL2
 * <li>MeterCurrentL3
 * <li>FreeAvailableCurrentL1
 * <li>FreeAvailableCurrentL2
 * <li>FreeAvailableCurrentL3
 * </ul>
 */
public class MeterHandler {

	private ElectricityMeter meter;
	// not nice, but makes junit tests easier
	private Config config;
	private EvcsClusterLimiterControllerImpl clusterLimiter;

	private Consumer<Value<Integer>> currentL1Consumer;
	private Consumer<Value<Integer>> currentL2Consumer;
	private Consumer<Value<Integer>> currentL3Consumer;
	private Consumer<Value<Integer>> activePowerConsumer;

	public MeterHandler() {
	}

	protected void activate(EvcsClusterLimiterControllerImpl clusterLimiter, ElectricityMeter meter, Config config) {
		this.meter = meter;
		this.config = config;
		this.clusterLimiter = clusterLimiter;
		this.installChannelHandler();
		this.clusterLimiter._setTransportCapacity(
				EvcsUtils.currentToPower(this.safeOperationLimit(), Phases.THREE_PHASE.getValue()));

	}

	protected void deactivate() {
		if (this.currentL1Consumer == null) {
			return;
		}
		this.meter.getCurrentL1Channel().removeOnSetNextValueCallback(this.currentL1Consumer);
		this.currentL1Consumer = null;
		this.meter.getCurrentL2Channel().removeOnSetNextValueCallback(this.currentL2Consumer);
		this.currentL2Consumer = null;
		this.meter.getCurrentL3Channel().removeOnSetNextValueCallback(this.currentL3Consumer);
		this.currentL3Consumer = null;
		this.meter.getActivePowerChannel().removeOnSetNextValueCallback(this.activePowerConsumer);
		this.activePowerConsumer = null;
	}

	protected void installChannelHandler() {

		var targetCurrentPerPhase = EvcsUtils.powerToCurrentInMilliampere(this.config.targetPower(),
				Phases.THREE_PHASE.getValue());

		this.currentL1Consumer = value -> {
			var average = this.getAverageValue(this.meter.getCurrentL1Channel(), value);
			this.clusterLimiter._setMeterCurrentL1(average);
			if (average != null) {
				this.clusterLimiter._setFreeAvailableCurrentL1(targetCurrentPerPhase - average);
				return;
			}
			this.clusterLimiter._setFreeAvailableCurrentL1(null);
		};
		this.meter.getCurrentL1Channel().onSetNextValue(this.currentL1Consumer);

		this.currentL2Consumer = value -> {
			var average = this.getAverageValue(this.meter.getCurrentL2Channel(), value);
			this.clusterLimiter._setMeterCurrentL2(average);
			if (average != null) {
				this.clusterLimiter._setFreeAvailableCurrentL2(targetCurrentPerPhase - average);
				return;
			}
			this.clusterLimiter._setFreeAvailableCurrentL2(null);
		};
		this.meter.getCurrentL2Channel().onSetNextValue(this.currentL2Consumer);

		this.currentL3Consumer = value -> {
			var average = this.getAverageValue(this.meter.getCurrentL3Channel(), value);
			this.clusterLimiter._setMeterCurrentL3(average);
			if (average != null) {
				this.clusterLimiter._setFreeAvailableCurrentL3(targetCurrentPerPhase - average);
				return;
			}
			this.clusterLimiter._setFreeAvailableCurrentL3(null);
		};
		this.meter.getCurrentL3Channel().onSetNextValue(this.currentL3Consumer);

		this.activePowerConsumer = value -> {
			var val = value.orElse(EvcsUtils.currentToPower(this.safeOperationLimit(), Phases.THREE_PHASE.getValue()));
			var map = this.meter.getActivePowerChannel().getPastValues()
					.tailMap(LocalDateTime.now().minusSeconds(this.config.meanFilterTime()));
			var pastValues = new ArrayList<Integer>();
			map.values().forEach(v -> {
				pastValues.add(
						v.orElse(EvcsUtils.currentToPower(this.safeOperationLimit(), Phases.THREE_PHASE.getValue())));
			});
			pastValues.add(val);
			var freePower = this.config.targetPower() - TypeUtils.averageInt(pastValues);
			this.clusterLimiter._setFreeAvailablePower(freePower);
		};
		this.meter.getActivePowerChannel().onSetNextValue(this.activePowerConsumer);

	}

	private Integer getAverageValue(IntegerReadChannel channel, Value<Integer> value) {
		var map = channel.getPastValues().tailMap(LocalDateTime.now().minusSeconds(this.config.meanFilterTime()));
		var pastValues = new ArrayList<>(map.values());
		pastValues.add(value);
		return average(pastValues);
	}

	private static Integer average(ArrayList<Value<Integer>> pastValues) {
		var intValues = new ArrayList<Integer>();
		pastValues.forEach(value -> {
			intValues.add(value.get());
		});
		return TypeUtils.averageInt(intValues);
	}

	protected Config config() {
		return this.config;
	}

	/**
	 * Get SafeOperationMode.
	 *
	 * @return true, if meter current for any of L1,L2,L3 is below (config.fuseLimit
	 *         - config.fuseSafetyOffset), false else
	 */
	public boolean getSafeOperationMode() {
		if (!this.safeOperationModeL1()) {
			return false;
		}
		if (!this.safeOperationModeL2()) {
			return false;
		}
		if (!this.safeOperationModeL3()) {
			return false;
		}
		return true;
	}

	/**
	 * Get SafeOperationMode for phase L1.
	 *
	 * @return true, if meter current for L1 is below (config.fuseLimit -
	 *         config.fuseSafetyOffset), false else
	 */
	public boolean safeOperationModeL1() {
		var safeOperationCurrent = this.safeOperationLimit() * 1_000;
		return this.meter.getCurrentL1().orElse(safeOperationCurrent) < safeOperationCurrent;
	}

	/**
	 * Get SafeOperationMode for phase L2.
	 *
	 * @return true, if meter current for L2 is below (config.fuseLimit -
	 *         config.fuseSafetyOffset), false else
	 */
	public boolean safeOperationModeL2() {
		var safeOperationCurrent = this.safeOperationLimit() * 1_000;
		return this.meter.getCurrentL2().orElse(safeOperationCurrent) < safeOperationCurrent;
	}

	/**
	 * Get SafeOperationMode for phase L3.
	 *
	 * @return true, if meter current for L3 is below (config.fuseLimit -
	 *         config.fuseSafetyOffset), false else
	 */
	public boolean safeOperationModeL3() {
		var safeOperationCurrent = this.safeOperationLimit() * 1_000;
		return this.meter.getCurrentL3().orElse(safeOperationCurrent) < safeOperationCurrent;
	}

	private int safeOperationLimit() {
		return this.config().fuseLimit() - this.config().fuseSafetyOffset();
	}

	/**
	 * Checks that there is enough power above targetPower before switching to
	 * SAFE_OPERATION_MODE = false.
	 *
	 * @return true if distance between targetPower and safeOperationPower is too
	 *         low.
	 */
	public boolean isStateConfigurationWarning() {
		var targetPower = this.config.targetPower();
		var safetyPower = EvcsUtils.currentToPower(this.safeOperationLimit(), Phases.THREE_PHASE.getValue());
		var safeDistance = EvcsUtils.currentToPower(this.config().fuseSafetyOffset(), Phases.THREE_PHASE.getValue());

		if (targetPower <= safetyPower - safeDistance) {
			return false;
		}
		return true;
	}

	/**
	 * Signed Current on a all phases. Method returns the current of the highest
	 * phase.
	 *
	 * @return when returning a positive integer it represents the current of the
	 *         phase with the highest consumption flow. When returning a negative
	 *         integer it represents the current of the phase with the lowes
	 *         production flow.
	 */
	private int maxSignedCurrentOnPhasesInMilliAmpere() {
		try {
			var currentL1 = this.meter.getCurrentL1().getOrError();
			var currentL2 = this.meter.getCurrentL2().getOrError();
			var currentL3 = this.meter.getCurrentL3().getOrError();

			// ignore PV pushing energy to grid
			if (this.meter.getActivePowerL1().getOrError() < 0) {
				// TODO It is unspecified if ElectricityMeter.CURRENT_Lx is positive or
				// negative, we make sure it fits well
				if (currentL1 >= 0) {
					currentL1 *= -1;
				}
			}
			if (this.meter.getActivePowerL2().getOrError() < 0) {
				// TODO It is unspecified if ElectricityMeter.CURRENT_Lx is positive or
				// negative, we make sure it fits well
				if (currentL2 >= 0) {
					currentL2 *= -1;
				}
			}
			if (this.meter.getActivePowerL3().getOrError() < 0) {
				// TODO It is unspecified if ElectricityMeter.CURRENT_Lx is positive or
				// negative, we make sure it fits well
				if (currentL3 >= 0) {
					currentL3 *= -1;
				}
			}

			return Math.max(Math.max(currentL1, currentL2), currentL3);
		} catch (InvalidValueException e) {
			return this.config().fuseLimit() * 1000 + 1;
		}
	}

	/**
	 * Signed Current on a all phases. Method returns the current of the lowest
	 * phase.
	 *
	 * @return when returning a positive integer it represents the current of the
	 *         phase with the lowest consumption flow. When returning a negative
	 *         integer it represents the current of the phase with the hightest
	 *         production flow.
	 */
	private int minSignedCurrentOnPhasesInMilliAmpere() {
		try {
			var currentL1 = this.meter.getCurrentL1().getOrError();
			var currentL2 = this.meter.getCurrentL2().getOrError();
			var currentL3 = this.meter.getCurrentL3().getOrError();

			// ignore PV pushing energy to grid
			if (this.meter.getActivePowerL1().getOrError() < 0) {
				// TODO It is unspecified if ElectricityMeter.CURRENT_Lx is positive or
				// negative, we make sure itfits well
				if (currentL1 >= 0) {
					currentL1 *= -1;
				}
			}
			if (this.meter.getActivePowerL2().getOrError() < 0) {
				// TODO It is unspecified if ElectricityMeter.CURRENT_Lx is positive or
				// negative, we make sure itfits well
				if (currentL2 >= 0) {
					currentL2 *= -1;
				}
			}
			if (this.meter.getActivePowerL3().getOrError() < 0) {
				// TODO It is unspecified if ElectricityMeter.CURRENT_Lx is positive or
				// negative, we make sure it fits well
				if (currentL3 >= 0) {
					currentL3 *= -1;
				}
			}

			return Math.min(Math.min(currentL1, currentL2), currentL3);
		} catch (InvalidValueException e) {
			return -(this.config().fuseLimit() * 1000 + 1);
		}
	}

	/**
	 * Update the meter channel value.
	 *
	 * @return true if the meter is ok.
	 */
	public boolean isMeterOk() {
		if (this.meter.getState().isAtLeast(Level.INFO)) {
			return false;
		}
		if (!this.meter.getCurrentL1().isDefined() || !this.meter.getCurrentL2().isDefined()
				|| !this.meter.getCurrentL3().isDefined() || !this.meter.getActivePower().isDefined()) {
			return false;
		}
		return true;
	}

	// TODO PhaseImbalance could be moved to ElectricityMeter

	/**
	 * The Phase imbalance State.
	 *
	 * @return the current PhaseImbalance state.
	 */
	public PhaseImbalance getPhaseImbalance() {

		if (!this.isUnbalanced()) {
			return PhaseImbalance.NO_IMBALANCE;
		}
		var min = this.minSignedCurrentOnPhasesInMilliAmpere();
		var max = this.maxSignedCurrentOnPhasesInMilliAmpere();
		try {

			return this.phaseImbalancePureProductionOrConsumtion(min, max);

		} catch (Exception e) {
			return PhaseImbalance.NO_IMBALANCE;
		}
	}

	/**
	 * The imbalance current.
	 *
	 * @return the imbalance current in mA.
	 */
	public int getImbalanceCurrent() {
		var min = this.minSignedCurrentOnPhasesInMilliAmpere();
		var max = this.maxSignedCurrentOnPhasesInMilliAmpere();
		return Math.abs(max - min);
	}

	/**
	 * Balance State.
	 *
	 * @return true, if the meter is unbalanced.
	 */
	public boolean isUnbalanced() {
		return this.getImbalanceCurrent() > this.config().phaseImbalanceCurrent() * 1_000;
	}

	private PhaseImbalance phaseImbalancePureProductionOrConsumtion(int min, int max) throws InvalidValueException {
		var currentL1 = this.meter.getCurrentL1().getOrError();
		var currentL2 = this.meter.getCurrentL2().getOrError();
		var currentL3 = this.meter.getCurrentL3().getOrError();
		var med = (max + min) / 2;

		if (currentL1 == max) {
			if (currentL2 <= med && currentL3 <= med) {
				return PhaseImbalance.L1_TOO_HIGH;
			}
			if (currentL2 > med) {
				return PhaseImbalance.L3_TOO_LOW;
			}
			if (currentL3 > med) {
				return PhaseImbalance.L2_TOO_LOW;
			}
		}
		if (currentL2 == max) {
			if (currentL1 <= med && currentL3 <= med) {
				return PhaseImbalance.L2_TOO_HIGH;
			}
			if (currentL1 > med) {
				return PhaseImbalance.L3_TOO_LOW;
			}
			if (currentL3 > med) {
				return PhaseImbalance.L1_TOO_LOW;
			}
		}
		if (currentL3 == max) {
			if (currentL1 <= med && currentL2 <= med) {
				return PhaseImbalance.L3_TOO_HIGH;
			}
			if (currentL1 > med) {
				return PhaseImbalance.L2_TOO_LOW;
			}
			if (currentL2 > med) {
				return PhaseImbalance.L1_TOO_LOW;
			}
		}
		return PhaseImbalance.NO_IMBALANCE;
	}

}
