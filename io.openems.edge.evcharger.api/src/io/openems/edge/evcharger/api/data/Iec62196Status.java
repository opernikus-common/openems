package io.openems.edge.evcharger.api.data;

import io.openems.common.types.OptionsEnum;

/**
 * Charging states based on IEC 62196-2.
 * 
 * <p>
 * See https://de.wikipedia.org/wiki/IEC_62196#Typ_2:_EN_62196-2_(VDE-AR-E_2623-2-2) for more.
 */
public enum Iec62196Status implements OptionsEnum {
	UNDEFINED(64, "Undefined"), //
	/**
	 * 65 = no vehicle connected.
	 */
	NO_VEHICLE('A', "NO_VEHICLE"), //
	/**
	 * 66 = Vehicle detected, no charging process.
	 */
	VEHICLE_DETECTED('B', "Vehicle Detected"), //
	/**
	 * 67 = Charging.
	 */
	CHARGING('C', "Charging"), //
	/**
	 * 68 = Charging with ventilation.
	 */
	CHARGING_VENTILATION('D', "Ventilation charging "), //
	/**
	 * 69 = Charging station is deactivated.
	 */
	DEACTIVATED('E', "Deactivated"), //
	/**
	 * 70 = Error in the charging station.
	 */
	ERROR('F', "Error"), //
	;

	private final int value;
	private final String name;

	private Iec62196Status(int value, String name) {
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
