package io.openems.edge.evcs.cluster.chargemanagement;

import io.openems.common.types.OptionsEnum;

public enum State implements io.openems.edge.common.statemachine.State<State>, OptionsEnum {
	GREEN(0), //
	YELLOW(1), //
	RED(2), //
	;

	private final int value;

	private State(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public OptionsEnum getUndefined() {
		return RED;
	}

	@Override
	public State[] getStates() {
		return State.values();
	}
}
