package io.openems.edge.evcs.cluster.chargemanagement.utils;

import io.openems.edge.evcs.cluster.chargemanagement.statemachine.Context;

/**
 * Dynamically adoptable definition of a power zone around the target power.
 * 
 * <p>
 * Reason: In a constant situation the GreenHandler "jitters" around the target
 * power. This is a bad experience for users looking at the UI. Green handler
 * will stay within this zone for a given time without "jittering" when it stays
 * in this Zone.
 */
public class TargetPowerZone {
	private Integer higherBorder = null;
	private Integer lowerBorder = null;
	private boolean disableZoneTemporarily = false;

	public TargetPowerZone() {

	}

	/**
	 * Sets the lower border of the targetpower zone.
	 * 
	 * @param freePower power still available
	 */
	public void setLowerBorder(int freePower) {
		if (this.higherBorder == null) {
			this.lowerBorder = freePower;
		}
	}

	/**
	 * Sets the higher border of the targetpower zone.
	 * 
	 * @param context   cluster attribute storage
	 * @param freePower power still available
	 */
	public void setHigherBorder(Context context, int freePower) {
		if (this.lowerBorder != null && this.higherBorder == null) {

			var offset = 0;
			if (this.lowerBorder < 0) {
				// readjust higher/lower soft border, because residual power should be > 0
				offset = -this.lowerBorder;
				this.lowerBorder = 0;
			}
			this.higherBorder = freePower + offset;
			context.getParent()
					.logInfo("-----------  Updated Soft Limits " + this.lowerBorder + " - " + this.higherBorder);
			context.getTargetPowerZoneTimer().reset();
		}
	}

	/**
	 * Checks if the cluster operates close to the targetPower (softborder
	 * above/below target power).
	 * 
	 * @param context cluster attribute storage
	 * 
	 * @return true if currentPower is close to targetPower.
	 */
	public boolean fitsTargetPowerZone(Context context) {

		if (this.isTemporarilyDisabled(context)) {
			return false;
		}
		var freePower = context.getCableConstraints().getMinFreeAvailablePower();
		if (this.lowerBorder == null || this.higherBorder == null) {
			return false;
		}
		if (context.getTargetPowerZoneTimer().checkAndReset()) {
			this.lowerBorder = null;
			this.higherBorder = null;
			return false;
		}
		if (freePower >= this.lowerBorder.intValue() && freePower <= this.higherBorder.intValue()) {
			return true;
		}
		return false;
	}

	private boolean isTemporarilyDisabled(Context context) {
		if (this.disableZoneTemporarily) {
			if (context.getTargetPowerZoneTimer().checkAndReset()) {
				this.lowerBorder = null;
				this.higherBorder = null;
				this.disableZoneTemporarily = false;
				context.getParent().logInfo("Activate TargetPowerZone");

			}
			return true;
		}
		return false;
	}

	/**
	 * Disables the TargetPower zone for a few seconds.
	 * 
	 * @param context cluster attribute storage
	 */
	public void disableTemporarily(Context context) {
		context.getParent().logInfo("Deactivate TargetPowerZone");
		context.getTargetPowerZoneTimer().reset();
		this.disableZoneTemporarily = true;
	}

}
