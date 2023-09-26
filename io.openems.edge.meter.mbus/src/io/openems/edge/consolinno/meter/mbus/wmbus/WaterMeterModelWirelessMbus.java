package io.openems.edge.consolinno.meter.mbus.wmbus;

public enum WaterMeterModelWirelessMbus {
	// Water meter models with their record positions for WMbus.
	AUTOSEARCH(0), //
	RELAY_PADPULS_M2W_CHANNEL1(0), //
	RELAY_PADPULS_M2W_CHANNEL2(0), //
	ENGELMANN_WATERSTAR_M(19) //
	;

	private final int volumeCounterPosition;

	WaterMeterModelWirelessMbus(int volume) {
		this.volumeCounterPosition = volume;
	}

	public int getVolumeCounterPosition() {
		return this.volumeCounterPosition;
	}

}
