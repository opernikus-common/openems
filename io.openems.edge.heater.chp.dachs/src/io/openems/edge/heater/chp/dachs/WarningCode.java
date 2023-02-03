package io.openems.edge.heater.chp.dachs;

import io.openems.common.types.OptionsEnum;

public enum WarningCode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NONE(0, "none"), //

	BURNER_DOES_NOT_START(610,
			"Burner does not start - SEplus switched off? (Brenner startet nicht - SEplus ausgeschaltet?)"), //
	BURNER_LOCKS(620, "Burner locks - fault clearance necessary (Brenner verriegelt - Entstoerung notwendig)"), //
	BURNER_RUNNING_WITHOUT_DEMAND(630, "Burner running without demand - internal fault SEplus or false enable "
			+ "wiring at MSR2 (Brennerlauf ohne Anforderung - Interner Fehler SEplus oder falsche Verdrahtung der Freigabe am MSR2)"), //
	CHECK_HYDRAULIC(650,
			"Check hydraulic / Check MSR2-settings (Hydraulik ueberpruefen / " + "MSR2-Einstellungen pruefen)"), //
	EXHAUST_HEAT_EXCHANGER(652,
			"Exhaust heat exchanger/soot filter contaminated, check injection "
					+ "(Abgaswaermetausche/Russfilter verschutzt, Einspritzung ueberpruefen)"), //
	EXHAUST_HEAT_EXCHANGER_DIRTY(653, "Exhaust heat exchanger contaminated (Abgaswaermetausche verschutzt)"), //
	FAULT_IN_COOLING_WATER(654,
			"Fault in cooling water flow / cooling water low / calcification "
					+ "(Durchflussstoerung Kuehlwasser / zu wenig Kuehlwasser / Kalkablagerungen)"), //
	THERMOSTAT_DEFECT(655,
			"Thermostat defect / ext. pump pushes cooling water into the system "
					+ "(Thermostat defekt / ext. Pumpe druekt Kuehlwasser in die Anlage)"), //
	EXHAUST_SENSOR(661,
			"Exhaust sensor error, only Dachs G/F (Abgasfuehler Motor-Austritt "
					+ "Unterbrechung/Kurzschluss, nur Dachs G/F)"), //
	EXHAUST_TEMP_TOO_HIGH(662,
			"Exhaust temperature too high, only Dachs G/F (Abgastemperatur "
					+ "Motor-Austritt zu hoch, nur Dachs G/F)"), //
	FUEL_PRESSURE_ABSENT(698, "Fuel pressure absent, only Dachs RS (Kraftstoffdruck fehlt, nur bei Dachs RS)"), //
	EEPROM_DATA_FAULTY_AFTER_UPDATE(699,
			"EEPROM data faulty upon initialization (Bei Initialisierung EEPROM Daten fehlerhaft)"), //
	CANNOT_START_GRID_ERROR(700,
			"Dachs cannot start because of power grid errors (voltage, frequency), average U (Brenner verriegelt - Entstoerung notwendig)"), //
	NO_RELEASE_OF_FLAG(711,
			"Only with multi module: No enabling of Flags ’max return temp’ and "
					+ "respective ’temperature’ in spite of pump running (nur MehrModul: keine Freigabe des Flags "
					+ "’Max. Ruecklauftemp’ bzw. ’Temperatur’ trotz Pumpenlauf)"), //
	FLOW_TEMP_NOT_REACHED(726, "Flow temperature not reached after 3h, check sensor placement "
			+ "(Vorlauftemp nach 3h nicht erreicht, Platzierung Vorlauffuehler pruefen)") //

	;

	private final int value;
	private final String name;

	private WarningCode(int value, String name) {
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
}
