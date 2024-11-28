package io.openems.edge.evcharger.api.data;

import io.openems.common.types.OptionsEnum;

/**
 * Plug charging type of the chargepoint.
 */
public enum ChargingType implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	CCS(0, "CCS"), //
	CHADEMO(1, "Chademo"), //
	AC(2, "AC"), //
	// TODO add BIDIRECTIONAL(3, "Bidirectional"), //
	;

	private final int value;
	private final String name;

	private ChargingType(int value, String name) {
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
