package io.openems.edge.controller.evcs.vehiclebatterysymmetric;

import io.openems.common.types.OptionsEnum;

public enum Mode implements OptionsEnum {
	OFF(0, "Off"), //
	CHARGE(1, "Charge"), //
	DISCHARGE(2, "Discharge"), //
	AUTOMATIC(3, "Automatic");

	private final int value;
	private final String name;

	private Mode(int value, String name) {
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
		return OFF;
	}
}
