package io.openems.edge.evcs.cluster.chargemanagement.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.cluster.chargemanagement.State;

public class YellowHandler extends BaseHandler {

	private RoundRobin roundRobin;

	public YellowHandler() {
		super();
	}

	protected void onEntry(Context context) throws OpenemsNamedException {
		context.getCluster().limitPowerAll(context.getCluster().getEvcsMinPowerLimit());
		context.getRoundRobinTimer().reset();
		context.getImbalanceHoldTimer().reset();
		context.getLimitsExceededTimer().reset();
		context.getLimitsExceededCheckTimer().reset();
		var cntChargingEvcs = context.getCluster().countAllChargingReadyEvcs();
		this.roundRobin = new RoundRobin(context, cntChargingEvcs);

		if (cntChargingEvcs > 1) {
			this.roundRobin.setMaxAllowedChargeSessions(cntChargingEvcs - 1);
			this.roundRobin.nextRound();
		} else {
			this.roundRobin.adoptMaxAllowedChargeSessions(false);
		}
	}

	protected void onExit(Context context) throws OpenemsNamedException {
		context.getParent()._setRoundRobinAllowedChargeSessions(0);
	}

	@Override
	public State runAndGetNextState(Context context) {
		var cluster = context.getParent();

		if (cluster.hasFaults()) {
			return State.RED;
		}

		if (!context.getParent().getAllowCharging().orElse(false)) {
			return State.RED;
		}
		if (!context.getCableConstraints().safeOperationMode()) {
			return State.RED;
		}
		if (context.getLimitsExceededCheckTimer().checkAndReset()) {
			if (this.roundRobin.adoptMaxAllowedChargeSessions(true)) {
				return State.RED;
			}
			if (this.roundRobin.isUnlimited() && context.getCableConstraints()
					.getMinFreeAvailablePower() >= Evcs.DEFAULT_MINIMUM_HARDWARE_POWER) {
				return State.GREEN;
			}
		}
		if (!context.getCableConstraints().exceedsTargetLimit()) {
			context.getLimitsExceededTimer().reset();
		}
		if (context.getLimitsExceededTimer().checkAndReset()) {
			return State.RED;
		}

		// TODO
		//
		// switch to red immediately when all charging evcss are at minPower and
		// MIN_FREE_AVAILABLE_POWER < 0
		//
		// improve phase imbalance handling
		// 1. detect phase imbalance
		// 2. switch off one chargepoint
		// 3. switch off another chargepoint every 10s
		// 4. switch to red when config.unbalancedHoldTime() is exceeded.

		this.roundRobin.update();
		if (context.getRoundRobinTimer().checkAndReset()) {
			if (this.roundRobin.adoptMaxAllowedChargeSessions(false)) {
				return State.RED;
			}
			this.roundRobin.nextRound();
		}
		return State.YELLOW;
	}

}
