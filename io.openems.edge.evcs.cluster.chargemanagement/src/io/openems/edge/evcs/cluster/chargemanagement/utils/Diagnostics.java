package io.openems.edge.evcs.cluster.chargemanagement.utils;

import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmtImpl;

public class Diagnostics {
	private final EvcsClusterChargeMgmtImpl parent;

	public Diagnostics(EvcsClusterChargeMgmtImpl parent) {
		super();
		this.parent = parent;
	}

	/**
	 * Check if a max warning should be raised.
	 * 
	 * @param id             of the evcs to raise the warning for.
	 * @param shouldBeRaised true if warning should be raised.
	 */
	public void raiseMaxWarning(String id, boolean shouldBeRaised) {
		if (shouldBeRaised) {
			this.parent.logWarn("EVCS " + id + " MaxPower Limits undefined");
			this.parent._setEvcsNoMaxPowerWarn(shouldBeRaised);
		}
		// TODO Warnung zuruecknehmen ueber alle EVCSs.
	}

	/**
	 * Check if a min warning should be raised.
	 * 
	 * @param id             of the evcs to raise the warning for.
	 * @param shouldBeRaised true if warning should be raised.
	 */
	public void raiseMinWarning(String id, boolean shouldBeRaised) {
		if (shouldBeRaised) {
			this.parent.logWarn("EVCS " + id + " MinPower Limits undefined");
			this.parent._setEvcsNoMinPowerWarn(shouldBeRaised);
		}
	}

	/**
	 * Check if a warning should be raised.
	 * 
	 * @param id             of the evcs to raise the warning for.
	 * @param shouldBeRaised true if warning should be raised.
	 */
	public void raiseSetChargePowerLimit(String id, boolean shouldBeRaised) {
		if (shouldBeRaised) {
			this.parent.logWarn("EVCS " + id + " SetChargePowerLimit warning");
			this.parent._setSetChargePowerLimitWarn(shouldBeRaised);
		}
	}

}
