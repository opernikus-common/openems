package io.openems.edge.evcs.cluster.chargemanagement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.openems.edge.controller.evcs.cluster.chargemanagement.EvcsClusterLimiter;
import io.openems.edge.controller.evcs.cluster.chargemanagement.PhaseImbalance;
import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmt.ChannelId;

/**
 * SupplyCableConstraints handles everything related to the cable limit. This
 * information will be set provided by one ore several EvcsClusterLimiter. The
 * SupplyCableConstraints will continuously update
 * <ul>
 * <li>MaxPowerLimit - the max possible limit on all cables
 * <li>ResidualPower - the max possible freepower on all cables
 * <li>SafeOperationMode - true - cluster runs in safe operation mode, false if
 * cluster has switched to state RED.
 * <li>PhaseImbalance - phase imbalance information
 * <li>PhaseImbalanceCurrent - phase imbalance current
 * <li>InfoPhaseImbalance - user phase imbalance information
 * </ul>
 */
public class SupplyCableConstraints {

	private final EvcsClusterChargeMgmtImpl parent;
	private final List<EvcsClusterLimiter> tmpEvcsClusterLimiters = new ArrayList<>();

	private final AtomicReference<List<EvcsClusterLimiter>> evcsClusterLimiters = new AtomicReference<>(
			new ArrayList<>());// Limiters may be removed during runtime
	private Integer responsibleLimiterId;

	public SupplyCableConstraints(EvcsClusterChargeMgmtImpl parent) {
		this.parent = parent;
	}

	protected void add(EvcsClusterLimiter evcsClusterLimiter) {
		this.tmpEvcsClusterLimiters.add(evcsClusterLimiter);
		this.evcsClusterLimiters.set(Collections.unmodifiableList(this.tmpEvcsClusterLimiters));
		var tc = this.parent.getTransportCapacityChannel().getNextValue();
		var limiterTc = evcsClusterLimiter.getTransportCapacity();
		if (tc.isDefined()) {
			if (limiterTc.isDefined() && limiterTc.get() < tc.get()) {
				this.parent._setTransportCapacity(limiterTc.get());
			}
		} else {
			this.parent._setTransportCapacity(limiterTc.get());
		}
	}

	protected void remove(EvcsClusterLimiter evcsClusterLimiter) {
		this.tmpEvcsClusterLimiters.remove(evcsClusterLimiter);
		this.evcsClusterLimiters.set(Collections.unmodifiableList(this.tmpEvcsClusterLimiters));
	}

	/**
	 * Checks if any of the cable limits is exceeded.
	 * 
	 * @return if true cluster can be operated safe, if false cluster needs to
	 *         immediately switch to state RED.
	 */
	public boolean safeOperationMode() {
		if (this.hasMeterError()) {
			return false;
		}
		var safeOpMode = this.evcsClusterLimiters.get().stream() //
				.allMatch(clusterLimiter -> clusterLimiter.getSafeOperationMode().orElse(false));
		return safeOpMode;
	}

	private boolean hasMeterError() {
		return this.evcsClusterLimiters.get().stream() //
				.allMatch(clusterLimiter -> clusterLimiter.getMeterError().orElse(false));
	}

	private int diff(int p1, int p2) {
		// TODO pruefen, dass der richtige limiter zurückgegeben wird.
		if (p1 < p2) {
			return (p1 - p2);
		} else if (p1 > p2) {
			return (p1 - p2);
		}
		return 0;
	}

	/**
	 * Indicates how much min free power is available on the cable with the highest
	 * restrictions.
	 * 
	 * @return the power still available to use for evcss.
	 */
	public int getMinFreeAvailablePower() {
		var relLimiter = this.evcsClusterLimiters.get().stream().min((limiter1, limiter2) -> {
			var p1 = limiter1.getFreeAvailablePower().orElse(0);
			var p2 = limiter2.getFreeAvailablePower().orElse(0);
			return this.diff(p1, p2);
		});
		this.responsibleLimiterId = relLimiter.get().getLimiterId();
		var freePower = relLimiter.get().getFreeAvailablePower().orElse(0);
		return freePower;
	}

	/**
	 * Gets the min free available current on L1.
	 * 
	 * @return current in mA
	 */
	public Integer getMinFreeAvailableCurrentL1() {
		var relLimiter = this.evcsClusterLimiters.get().stream().min((limiter1, limiter2) -> {
			var p1 = limiter1.getFreeAvailableCurrentL1().orElse(0);
			var p2 = limiter2.getFreeAvailableCurrentL1().orElse(0);
			return this.diff(p1, p2);
		});
		var freeCurrent = relLimiter.get().getFreeAvailableCurrentL1().orElse(0);
		return freeCurrent;
	}

	/**
	 * Gets the min free available current on L2.
	 * 
	 * @return current in mA
	 */
	public Integer getMinFreeAvailableCurrentL2() {
		var relLimiter = this.evcsClusterLimiters.get().stream().min((limiter1, limiter2) -> {
			var p1 = limiter1.getFreeAvailableCurrentL2().orElse(0);
			var p2 = limiter2.getFreeAvailableCurrentL2().orElse(0);
			return this.diff(p1, p2);
		});
		var freeCurrent = relLimiter.get().getFreeAvailableCurrentL2().orElse(0);
		return freeCurrent;
	}

	/**
	 * Gets the min free available current on L3.
	 * 
	 * @return current in mA
	 */
	public Integer getMinFreeAvailableCurrentL3() {
		var relLimiter = this.evcsClusterLimiters.get().stream().min((limiter1, limiter2) -> {
			var p1 = limiter1.getFreeAvailableCurrentL3().orElse(0);
			var p2 = limiter2.getFreeAvailableCurrentL3().orElse(0);
			return this.diff(p1, p2);
		});
		var freeCurrent = relLimiter.get().getFreeAvailableCurrentL3().orElse(0);
		return freeCurrent;
	}

	/**
	 * Indicates how much power is theoretically available for the cluster.
	 * 
	 * @return the theoretically available power of the weakest cable segment (the
	 *         one with min max power).
	 */
	public int getTransportCapacity() {
		var relLimiter = this.evcsClusterLimiters.get().stream().min((limiter1, limiter2) -> {
			var p1 = limiter1.getTransportCapacity().orElse(0);
			var p2 = limiter2.getTransportCapacity().orElse(0);
			if (p1 < p2) {
				return (p1 - p2);
			} else if (p1 > p2) {
				return (p1 - p2);
			}
			return 0;
		});

		// TODO pruefen, dass der richtige limiter zurückgegeben wird.

		var maxCapacity = relLimiter.get().getTransportCapacity().orElse(0);
		return maxCapacity;
	}

	/**
	 * Checks if we have enough residual power or better if we are above
	 * targetPower.
	 * 
	 * @return true if residualPower is less than 100W, false else.
	 */
	public boolean isAboveTargetLimit() {
		if (this.getMinFreeAvailablePower() < 0) {
			return true;
		}
		return false;
	}

	/**
	 * Checks if this cable has an unbalanced load.
	 * 
	 * @return true if the imbalance limit is reached.
	 */
	public boolean isUnbalanced() {
		if (this.getPhaseImbalance().getValue() != PhaseImbalance.NO_IMBALANCE.getValue()) {
			return true;
		}
		return false;
	}

	/**
	 * Returns the PhaseImbalance.
	 * 
	 * @return the current PhaseImabalance.
	 */
	public PhaseImbalance getPhaseImbalance() {
		var count = this.evcsClusterLimiters.get().stream() //
				.filter(item -> item.getPhaseImbalanceCurrent().isDefined()) //
				.count();
		if (count != 1) {
			this.parent.channel(ChannelId.WARN_NO_PHASE_IMBALANCE).setNextValue(true);
			return PhaseImbalance.NO_IMBALANCE;
		} else {
			this.parent.channel(ChannelId.WARN_NO_PHASE_IMBALANCE).setNextValue(false);
		}

		var clusterLimiter = this.evcsClusterLimiters.get().stream() //
				.filter(item -> item.getPhaseImbalance() != null) //
				.findAny();
		// clusterLimiter is also responsible for phaseImbalanceCurrent
		this.parent._setPhaseImbalanceCurrent(clusterLimiter.get().getPhaseImbalanceCurrent().get());

		return clusterLimiter.get().getPhaseImbalance();
	}

	/**
	 * Update the meter channel value.
	 */
	public void updateChannelValues() {
		var imbalance = this.getPhaseImbalance();
		this.parent._setPhaseImbalance(imbalance);
		this.parent.channel(EvcsClusterChargeMgmt.ChannelId.INFO_PHASE_IMBALANCE)
				.setNextValue(imbalance.getValue() != PhaseImbalance.NO_IMBALANCE.getValue());

		this.parent._setMinFreeAvailablePower(this.getMinFreeAvailablePower());
		this.parent._setSafeOperationMode(this.safeOperationMode());

		this.parent._setMinFreeAvailableCurrentL1(this.getMinFreeAvailableCurrentL1());
		this.parent._setMinFreeAvailableCurrentL2(this.getMinFreeAvailableCurrentL2());
		this.parent._setMinFreeAvailableCurrentL3(this.getMinFreeAvailableCurrentL3());
		this.parent._setTransportCapacity(this.getTransportCapacity());

		this.parent._setResponsibleLimiter(this.responsibleLimiterId);
	}

}
