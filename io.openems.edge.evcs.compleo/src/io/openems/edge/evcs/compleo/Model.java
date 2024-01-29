package io.openems.edge.evcs.compleo;

public enum Model {
	/**
	 * Without meter.
	 */
	COMPLEO_ECO_20(2), //
	/**
	 * With integrated meter.
	 */
	WALLBE_PRO(2), // 
	/**
	 * Without meter.
	 */
	WALLBE_ECO_20(3); //

	protected final int scaleFactor;


	Model(int scaleFactor) {
		this.scaleFactor = scaleFactor;
	}

}
