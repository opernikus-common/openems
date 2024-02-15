package io.openems.edge.evcs.cluster.chargemanagement.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.evcs.cluster.chargemanagement.State;

public abstract class BaseHandler extends StateHandler<State, Context> {

	public BaseHandler() {
		super();
	}

	protected void logInfo(Context context, String txt) {
		if (context.getConfig().verboseDebug()) {
			context.getParent().logInfo(txt);
		}

	}
}
