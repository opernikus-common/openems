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
		switch (state) {
		case GREEN:
			return new GreenHandler();
		case YELLOW:
			return new YellowHandler();
		case RED:
			return new RedHandler();
		}
		throw new IllegalArgumentException("Unknown State [" + state + "]");
	}
}
