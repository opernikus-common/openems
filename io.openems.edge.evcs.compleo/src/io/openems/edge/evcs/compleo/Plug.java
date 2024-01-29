package io.openems.edge.evcs.compleo;

import io.openems.common.types.OptionsEnum;

public enum Plug implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	UNPLUGGED(0, "Unplugged"), //
	PLUGGED(7, "Plugged");

	private final int value;
	private final String name;

	private Plug(int value, String name) {
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
		return UNDEFINED;
	}

}
