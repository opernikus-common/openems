package io.openems.edge.evcharger.api.data;

import io.openems.common.types.OptionsEnum;

/**
 * OperationMode of a EvChargerController.
 * 
 * <p>
 * Describes the general behavior of an EvCharger.
 */
public enum OperationMode implements OptionsEnum {

	/* energy management decides automatically when to charge the car. */
	Automatic(0, "Automatic"), //
	/* charge the car as fast as possible */
	FastCharge(1, "FastCharge"), //
	/* EvCharger is off */
	Off(2, "Off"), //
	/*
	 * Only charge on pv excess. Note that the charge session may be suspended for
	 * several hours. Check if your EvCharger/Car combination is able to handle
	 * that.
	 */
	PvOnly(3, "PvOnly"), //
	/*
	 * mainly charge on pv excess, but charge session will always run with min
	 * possible power. This is for EvCharger/Car charge sessions, which can't be
	 * restarted.
	 */
	PvOptimized(4, "PvOptimized"), //
	/*
	 * Only charge on min tariff. Note that the charge session may be suspended for
	 * several hours. Check if your EvCharger/Car combination is able to handle
	 * that.
	 */
	TariffOnly(5, "TariffOnly"), //
	/*
	 * mainly charge on min tariff, but charge session will always run with min
	 * possible power. This is for EvCharger/Car charge sessions, which can't be
	 * restarted.
	 */
	TariffOptimized(6, "TariffOptimized"), //

	;

	private final int value;
	private final String name;

	private OperationMode(int value, String name) {
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
		return Automatic;
	}
}
