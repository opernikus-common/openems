package io.openems.backend.common.oemprovider;

import java.util.Arrays;

public enum DataType {
	LONG, //
	DOUBLE, //
	UNDEFINED //
	;

	/**
	 * Fetch the {@link DataType} from the given String.
	 * 
	 * @param type the type as a String
	 * @return the correlating {@link DataType}.
	 */
	public static DataType fromString(String type) {

		return Arrays.stream(DataType.values()).filter(eType -> //
		eType.name().equalsIgnoreCase(type)).findFirst().orElse(null);
	}
}
