package io.openems.edge.simulator.evcharger;

import io.openems.common.types.OptionsEnum;

/**
 * definition of the following datasource CSV File format.
 *
 * <p>
 * EventTimeHour,EventTimeMin,EventType(1=NoVehicle..2=Charging..3=Error..4=CommunicationError),Phases,MaxCurrent,EnergyLimit.
 */
public enum DataSourceEventType implements OptionsEnum {

	UNDEFINED(0, "Undefined"), //
	NO_VEHICLE(1, "No Vehicle"), //
	CHARGING(2, "Charging"), //
	ERROR(3, "Error"), //
	COM_ERROR(4, "CommunicationError"), //
	;

	private final int value;
	private final String name;

	private DataSourceEventType(int value, String name) {
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

	/**
	 * Gets the event type to a corresponding int.
	 *
	 * @param value the int
	 * @return the corresponding EventType
	 */
	public static DataSourceEventType fromInt(int value) {
		return switch (value) {
		case 1 -> NO_VEHICLE;
		case 2 -> CHARGING;
		case 3 -> ERROR;
		case 4 -> COM_ERROR;
		default -> UNDEFINED;
		};
	}

}
