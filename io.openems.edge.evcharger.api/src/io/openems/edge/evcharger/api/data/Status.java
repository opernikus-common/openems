package io.openems.edge.evcharger.api.data;

import io.openems.common.types.OptionsEnum;

/**
 * Charging states how they are handled by OpenEMS. 
 */
public enum Status implements OptionsEnum {
	/**
	 * No vehicle connected.
	 */
	NO_VEHICLE(1, "NO_VEHICLE"), //
	/**
	 * Vehicle detected, no charging process.
	 */
	VEHICLE_DETECTED(2, "Vehicle Detected"), //
	/**
	 * Charging.
	 */
	CHARGING(3, "Charging"), //
	/**
	 * Charging finished.
	 */
	CHARGING_FINISHED(4, "Charging finished"), //
	/**
	 * Charging station is deactivated.
	 */
	DEACTIVATED(5, "Deactivated"), //
	/**
	 * Error in the charging station.
	 */
	ERROR(6, "Error"), //
	;

	private final int value;
	private final String name;

	private Status(int value, String name) {
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
		return NO_VEHICLE;
	}

}
