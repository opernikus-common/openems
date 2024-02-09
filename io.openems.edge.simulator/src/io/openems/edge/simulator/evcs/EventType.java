package io.openems.edge.simulator.evcs;

import io.openems.common.types.OptionsEnum;

public enum EventType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_CAR_CONNECTED(0, "No car connected"), //
	CAR_CONNECTED(1, "Car connected"), //
	ENERGY_LIMIT_REACHED(2, "Energy limit reached"); //

	private final int value;
	private final String name;

	private EventType(int value, String name) {
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

	/**
	 * Gets the event type to a corresponding int.
	 *
	 * @param value the int
	 * @return the corresponding EventType
	 */
	public static EventType fromInt(int value) {
		switch (value) {
		case 0:
			return NO_CAR_CONNECTED;
		case 1:
			return CAR_CONNECTED;
		case 2:
			return ENERGY_LIMIT_REACHED;
		default:
			return UNDEFINED;
		}
	}

}
