package io.openems.edge.evcs.cluster.chargemanagement.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.common.timer.Timer;
import io.openems.edge.evcs.cluster.chargemanagement.State;

public abstract class BaseHandler extends StateHandler<State, Context> {

	public BaseHandler() {
		super();
	}

	protected boolean hasDurationPassed(Timer timer) {
		if (timer.check()) {
			timer.reset();
			return true;
		}
		return false;
	}

}
