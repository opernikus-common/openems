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
		if (this.hasDurationPassed(context.getLimitsExceededCheckTimer())) {
			if (this.roundRobin.adoptMaxAllowedChargeSessions(true)) {
				return State.RED;
			}
			if (this.roundRobin.isUnlimited() && context.getCableConstraints()
					.getMinFreeAvailablePower() >= Evcs.DEFAULT_MINIMUM_HARDWARE_POWER) {
				return State.GREEN;
			}
		}
		if (!context.getCableConstraints().isAboveTargetLimit()) {
			context.getLimitsExceededTimer().reset();
		}
		if (this.hasDurationPassed(context.getLimitsExceededTimer())) {
			return State.RED;
		}

		// TODO
		//
		// -wenn nur noch eine charge evcss bereits auf minPower ist und residualPower
		// == 0 ist, sofort in rot umschalten
		//
		// -bei phasenschieflast intelligent und schnell handeln
		// 1. Phasenschieflast erkannen
		// 2. 5s konstante phasenschieflast detektieren
		// 3. eine ladesaeule ausschalten
		// 4. nach +10s eine weitere ladesaeule ausschalten
		// 5. nach +10s eine weitere ladesaeule ausschalten
		// 6. nach config.unbalancedHoldTime() seit Beginn der Phasenschieflast in
		// zustand rot wechseln
		//

		this.roundRobin.update();
		if (this.hasDurationPassed(context.getRoundRobinTimer())) {
			if (this.roundRobin.adoptMaxAllowedChargeSessions(false)) {
				return State.RED;
			}
			this.roundRobin.nextRound();
		}
		return State.YELLOW;
	}

}
