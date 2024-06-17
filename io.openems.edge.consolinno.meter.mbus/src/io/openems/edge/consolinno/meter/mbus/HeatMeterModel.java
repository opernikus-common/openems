package io.openems.edge.consolinno.meter.mbus;

public enum HeatMeterModel {
	// HeatMeter Types with their address for Mbus
	ITRON_CF_51(3, 4, 1, 5, 6), //
	QUNDIS_Q_HEAT_55(14, 10, 0, 11, 12), //
	LANDIS_UH50(4, 5, 2, 6, 7), //
	SHARKY_775(4, 5, 0, 6, 7), //
	ZELSIUS_C5_CMF(0, 0, 0, 0, 0), //
	ELSTER_SENSOSTAR_2(3, 5, 1, 7, 8), //
	AUTOSEARCH(0, 0, 0, 0, 0);

	private final int powerAddress;
	private final int flowRateAddress;
	private final int totalConsumptionEnergyAddress;
	private final int flowTempAddress;
	private final int returnTempAddress;

	HeatMeterModel(int power, int flowRate, int totalConsumptionEnergy, int flowTemp, int returnTemp) {
		this.powerAddress = power;
		this.flowRateAddress = flowRate;
		this.totalConsumptionEnergyAddress = totalConsumptionEnergy;

		this.flowTempAddress = flowTemp;
		this.returnTempAddress = returnTemp;
	}

	public int getPowerAddress() {
		return this.powerAddress;
	}

	public int getFlowRateAddress() {
		return this.flowRateAddress;
	}

	public int getTotalConsumptionEnergyAddress() {
		return this.totalConsumptionEnergyAddress;
	}

	public int getFlowTempAddress() {
		return this.flowTempAddress;
	}

	public int getReturnTempAddress() {
		return this.returnTempAddress;
	}
}
