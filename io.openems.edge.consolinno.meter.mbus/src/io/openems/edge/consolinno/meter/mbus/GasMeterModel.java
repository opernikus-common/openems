package io.openems.edge.consolinno.meter.mbus;

/**
 * Stored GasMeter types for easier configuration.
 */
public enum GasMeterModel {
	AUTOSEARCH(0), //
	PAD_PULS_M2(0), //
	ITRON_CYBLE(4);

	private final int totalConsumptionEnergyAddress;

	GasMeterModel(int totalConsumptionEnergy) {
		this.totalConsumptionEnergyAddress = totalConsumptionEnergy;
	}

	/**
	 * Gets the address for the TotalConsumptionEnergyAddress. It's not important if
	 * Channel Measured in m³ or kW.
	 *
	 * @return the address.
	 */

	public int getTotalConsumptionEnergyAddress() {
		return this.totalConsumptionEnergyAddress;
	}

}
