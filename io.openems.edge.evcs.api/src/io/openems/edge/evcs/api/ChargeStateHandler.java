package io.openems.edge.evcs.api;

import java.time.Instant;

/**
 * Set the ChargeState from increasing and reducing to charging after minimum
 * time until the charge limit is taken.
 */
public class ChargeStateHandler {

	private final ManagedEvcs parent;
	private Instant lastChargeStateTime = Instant.MIN;
	private ChargeState chargeState = ChargeState.CHARGING;

	public ChargeStateHandler(ManagedEvcs parent) {
		this.parent = parent;
	}

	/**
	 * Apply a new ChargeState.
	 * 
	 * <p>
	 * Set the ChargeState, if there is no pause active (for increasing and
	 * reducing). The Pause will be ignored and reset, if the given ChargeState is
	 * changing from INCREASING to DECREASING.
	 * 
	 * @param reqChargeState ChargeState
	 * @return true when the requiredChargeState is taken
	 */
	public boolean applyNewChargeState(ChargeState reqChargeState) {

		// Ignore and reset the pause if there is the need to reduce (Should reduce
		// immediately)
		if (this.chargeState == ChargeState.INCREASING && reqChargeState == ChargeState.DECREASING) {
			this.setChargeState(reqChargeState);
			this.startTimer();
			return true;
		}

		// Set the charge state - Start the pause when the power is increasing or
		// reducing
		if (!this.isTimerActive()) {
			switch (reqChargeState) {
			case INCREASING:
			case DECREASING:
				this.startTimer();
				break;
			default:
				break;
			}
			this.setChargeState(reqChargeState);
			return true;
		} else {
			// Pause active
			return reqChargeState == this.chargeState;
		}
	}

	private void startTimer() {
		this.lastChargeStateTime = Instant.now();
	}

	/**
	 * Is pause active.
	 * 
	 * @return boolean
	 */
	private boolean isTimerActive() {
		// Pause not started
		if (Instant.MIN.equals(this.lastChargeStateTime)) {
			return false;
		}

		// Pause active for a minimum time till the charging limit has been taken
		long timeTillLimitTaken = this.parent.getMinimumTimeTillChargingLimitTaken();
		if (Instant.now().minusSeconds(timeTillLimitTaken).isAfter(this.lastChargeStateTime)) {
			this.lastChargeStateTime = Instant.MIN;
			return false;
		}
		return true;
	}

	/**
	 * Get ChargeState.
	 * 
	 * @return current ChargeState
	 */
	public ChargeState getChargeState() {
		return this.chargeState;
	}

	/**
	 * Set local ChargeState and parent channel.
	 * 
	 * @param chargeState current ChargeState
	 */
	private void setChargeState(ChargeState chargeState) {
		this.chargeState = chargeState;
		this.parent.channel(ManagedEvcs.ChannelId.CHARGE_STATE).setNextValue(chargeState);
	}
}
