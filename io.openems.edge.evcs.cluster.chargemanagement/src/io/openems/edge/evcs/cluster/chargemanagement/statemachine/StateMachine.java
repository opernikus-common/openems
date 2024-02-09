package io.openems.edge.evcs.cluster.chargemanagement.statemachine;

import io.openems.edge.common.statemachine.AbstractStateMachine;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.evcs.cluster.chargemanagement.State;

public class StateMachine extends AbstractStateMachine<State, Context> {

	public StateMachine() {
		super(State.RED);

	}

	@Override
	public StateHandler<State, Context> getStateHandler(State state) {
		return switch (state) {
			case GREEN -> new GreenHandler();
			case YELLOW -> new YellowHandler();
			case RED -> new RedHandler();
		};
	}

}
