package io.openems.edge.evcs.cluster.chargemanagement.statemachine;

import io.openems.edge.controller.evcs.cluster.chargemanagement.PhaseImbalance;
import io.openems.edge.evcs.cluster.chargemanagement.ClusterEvcs;

/**
 * This Object contains information about an EVCS handled by Round Robin.
 */
public class RoundRobinEvcs {

	public static final int MIN_DETECTION_CURRENT = 1_000; // mA

	private final ClusterEvcs evcs;
	private boolean locked;
	private Boolean lockRequested;
	private int unlockCount;

	private boolean usesPhaseL1;
	private boolean usesPhaseL2;
	private boolean usesPhaseL3;
	private boolean threePhase;

	/**
	 * This Object contains the information to Identify an EVCS handled by
	 * RoundRobin.
	 *
	 * @param managedEvcs Evcs object
	 */
	public RoundRobinEvcs(ClusterEvcs managedEvcs) {
		this.evcs = managedEvcs;
		this.locked = true;
		this.lockRequested = null;
		this.resetUnlockCount();
	}

	/**
	 * Gets the evcs.
	 *
	 * @return the evcs.
	 */
	public ClusterEvcs evcs() {
		return this.evcs;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	public void setLockRequested(Boolean lockRequested) {
		if (lockRequested != null && !lockRequested && this.lockRequested != lockRequested) {
			this.unlockCount++;
		}
		this.lockRequested = lockRequested;
	}

	/**
	 * Checks if the lockRequested attribute is set to true.
	 *
	 * @return true if lockRequested is set.
	 */
	public boolean isLockRequestedTrue() {
		if (this.lockRequested == null) {
			return false;
		}
		return this.lockRequested;
	}

	/**
	 * Checks if the lockRequested attribute is set to false.
	 *
	 * @return true if lockRequested is unset.
	 */
	public boolean isLockRequestedFalse() {
		if (this.lockRequested == null) {
			return false;
		}
		return !this.lockRequested;
	}

	public boolean isLocked() {
		return this.locked;
	}

	public boolean isUnlocked() {
		return !this.locked;
	}

	/**
	 * Set the unlock count to zero.
	 *
	 * <p>
	 * This also (hopefully) indicates Chargepoint switched state to not charging
	 * anymore.
	 */
	public void resetUnlockCount() {
		this.lockRequested = null;
		this.unlockCount = 0;
		this.usesPhaseL1 = true;
		this.usesPhaseL2 = true;
		this.usesPhaseL3 = true;
		this.threePhase = true;

	}

	public int getUnlockCount() {
		return this.unlockCount;
	}

	/**
	 * Update Used Phases on chargepoint.
	 *
	 * <p>
	 * Should be called whenever a car is charging on this chargepoint.
	 */
	public void updateUsedPhases() {
		this.usesPhaseL1 = (this.evcs.getCurrentL1() >= MIN_DETECTION_CURRENT);
		this.usesPhaseL2 = (this.evcs.getCurrentL2() >= MIN_DETECTION_CURRENT);
		this.usesPhaseL3 = (this.evcs.getCurrentL3() >= MIN_DETECTION_CURRENT);
		this.threePhase = (this.usesPhaseL1 == this.usesPhaseL2 && this.usesPhaseL1 == this.usesPhaseL3);
	}

	/**
	 * Checks if this evcs helps fixing phase imbalance or at least does not make it
	 * worse.
	 *
	 * @param phaseImbalance the phase imbalance detected.
	 * @return true if it does not make worse phase imbalance or if it makes it less
	 *         critical, false else.
	 *
	 */
	public boolean mayFixPhaseImbalance(PhaseImbalance phaseImbalance) {
		if (this.threePhase) {
			return true;
		}
		return switch (phaseImbalance) {
		case NO_IMBALANCE -> {
			yield true;
		}
		case L1_TOO_HIGH -> {
			if (this.usesPhaseL1) {
				yield false;
			}
			yield true;
		}
		case L1_TOO_LOW -> {
			if (this.usesPhaseL1) {
				yield true;
			}
			yield false;
		}
		case L2_TOO_HIGH -> {
			if (this.usesPhaseL2) {
				yield false;
			}
			yield true;
		}
		case L2_TOO_LOW -> {
			if (this.usesPhaseL2) {
				yield true;
			}
			yield false;
		}
		case L3_TOO_HIGH -> {
			if (this.usesPhaseL3) {
				yield false;
			}
			yield true;
		}
		case L3_TOO_LOW -> {
			if (this.usesPhaseL3) {
				yield true;
			}
			yield false;
		}
		default -> {
			yield false;
		}
		};
	}

	/**
	 * To String method.
	 *
	 * @return the objects representation as a string.
	 */
	@Override
	public String toString() {
		return "[" + this.evcs.id() //
				+ ", lockReq=" + this.lockRequested + (this.locked ? ", -locked-" : "") //
				+ ", cnt " + this.unlockCount //
				+ "]";
	}

}
