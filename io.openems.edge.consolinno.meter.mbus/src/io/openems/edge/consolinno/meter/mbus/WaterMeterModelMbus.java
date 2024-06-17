package io.openems.edge.consolinno.meter.mbus;

public enum WaterMeterModelMbus {
	// Water meter models with their record positions for Mbus.
	AUTOSEARCH(0), //
	PAD_PULS_M2(0), //
	ITRON_BM_M(1), //
	WZG_VERSION_3(0);

	private final int volumeCounterPosition;

	WaterMeterModelMbus(int volume) {
		this.volumeCounterPosition = volume;

	}

	public int getVolumeCounterPosition() {
		return this.volumeCounterPosition;
	}

}
