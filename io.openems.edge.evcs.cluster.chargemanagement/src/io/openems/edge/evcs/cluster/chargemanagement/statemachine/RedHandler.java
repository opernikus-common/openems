package io.openems.edge.evcs.cluster.chargemanagement.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.cluster.chargemanagement.State;

public class RedHandler extends BaseHandler {

	private boolean firstRun = true;

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		context.getRedHoldTimer().reset();
		context.getCluster().limitPower(0, true);
		context.getCluster().limitPowerPrio(0, true);
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var cluster = context.getParent();

		context.getCluster().limitPower(0, false);
		context.getCluster().limitPowerPrio(0, false);

		if (cluster.hasFaults()) {
			context.getRedHoldTimer().reset();
			return State.RED;
		}

		if (!context.getParent().getAllowCharging().orElse(false)) {
			context.getRedHoldTimer().reset();
			return State.RED;
		}
		if (!context.getCableConstraints().safeOperationMode()) {
			context.getRedHoldTimer().reset();
			return State.RED;
		}
		if (context.getCableConstraints().getMinFreeAvailablePower() < Evcs.DEFAULT_MINIMUM_HARDWARE_POWER) {
			context.getRedHoldTimer().reset();
			return State.RED;
		}

		if (this.firstRun) {
			this.firstRun = false;
			return State.GREEN;
		}

		if (!context.getRedHoldTimer().checkAndReset()) {
			// delay state switch until redHoldTime has passed
			return State.RED;
		}

		return State.YELLOW;
	}

}
