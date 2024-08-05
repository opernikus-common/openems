package io.openems.backend.common.oemprovider;

import java.util.Arrays;

public enum ChannelType {

	AVG, //
	MAX, //
	UNDEFINED, //
	;

	/**
	 * Fetch the ChannelType from the given String.
	 * 
	 * @param type the type as a String
	 * @return the correlating {@link ChannelType}.
	 */
	public static ChannelType fromString(String type) {
		return Arrays.stream(ChannelType.values()).filter(eType -> //
		eType.name().equalsIgnoreCase(type)).findFirst().orElse(UNDEFINED);
	}
}
