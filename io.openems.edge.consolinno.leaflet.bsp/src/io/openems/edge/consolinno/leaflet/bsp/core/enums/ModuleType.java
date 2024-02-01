package io.openems.edge.consolinno.leaflet.bsp.core.enums;

public enum ModuleType {
	TMP, //
	REL, //
	PWM, //
	LEAFLET, //
	GPIO, //
	AIO, //
	ERROR;

	/**
	 * Checks if the given string is a ModuleType enum.
	 * 
	 * @param manage the string to test
	 * @return true, if the string is of the given ModuleType
	 */
	public static boolean contains(String manage) {
		for (ModuleType moduleType : ModuleType.values()) {
			if (moduleType.name().equals(manage)) {
				return true;
			}
		}
		return false;
	}
}