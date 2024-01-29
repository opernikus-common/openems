package io.openems.edge.evcs.api;

import io.openems.common.types.OptionsEnum;

public enum Priority implements OptionsEnum {
	/**
	 * EVCS should only be charged if there is excess power.
	 */
	EXCESS_POWER_ONLY(0, "Excess Power Only"),
	/**
	 * Only when High EVCS charging let some energy space for other things.
	 */
	LOW(1, "Low"), //
	/**
	 * EVCS should be charged fastly.
	 */
	HIGH(2, "High") //
	;

	private final int value;
	private final String name;

	private Priority(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return LOW;
	}
	
	/**
	 * Is this Priority at least as high as the other Level.
	 *
	 * @param otherLevel the other level
	 * @return true if it as at least as high
	 */
	public boolean isAtLeast(Priority otherLevel) {
		return this.value >= otherLevel.value;
	}

}
