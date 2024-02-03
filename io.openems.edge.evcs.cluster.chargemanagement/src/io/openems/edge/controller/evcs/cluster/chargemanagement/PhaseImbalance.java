package io.openems.edge.controller.evcs.cluster.chargemanagement;

import io.openems.common.types.OptionsEnum;

public enum PhaseImbalance implements OptionsEnum {

	NO_IMBALANCE(0, "No Imbalance"), //
	L1_TOO_HIGH(1, "Phase L1 too high"), //
	L1_TOO_LOW(2, "Phase L1 too low"), //
	L2_TOO_HIGH(3, "Phase L2 too high"), //
	L2_TOO_LOW(4, "Phase L2 too low"), //
	L3_TOO_HIGH(5, "Phase L3 too high"), //
	L3_TOO_LOW(6, "Phase L3 too low"), //
	;

	private final int value;
	private final String name;

	private PhaseImbalance(int value, String name) {
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
		return NO_IMBALANCE;
	}
}