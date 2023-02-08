package io.openems.edge.heater.chp.dachs;

import io.openems.common.types.OptionsEnum;

public enum ErrorCode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NONE(0, "none"), //
	OUTLET_EXHAUST_SENSOR(101,
			"Dachs outlet exhaust sensor - interruption/short-circuit (Abgasfuehler "
					+ "HKA-Austritt - Unterbrechung/Kurzschluss"), //
	ENGINE_WATER_TEMP_SENSOR(102,
			"Engine water temperature sensor - interruption/short-circuit "
					+ "(Kuehlwasserfuehler Motor - Unterbrechung/Kurzschluss"), //
	GENERATOR_WATER_TEMP_SENSOR(103,
			"Generator water temperature sensor - interruption/short-circuit "
					+ "(Kuehlwasserfuehler Generator - Unterbrechung/Kurzschluss"), //
	ENGINE_OUTLET_EXHAUST_SENSOR(104,
			"Engine outlet exhaust sensor - interruption/short-circuit (Abgasfuehler "
					+ "Motor-Austritt - Unterbrechung/Kurzschluss"), //
	FLOW_TEMPERATURE_SENSOR(105,
			"Flow temperature sensor - interruption/short-circuit (Vorlauftemperatur " + "- Unterbrechung/Kurzschluss"), //
	RETURN_TEMPERATURE_SENSOR(106,
			"Return temperature sensor - interruption/short-circuit (Ruecklauftemperatur "
					+ "- Unterbrechung/Kurzschluss"), //
	SENSOR1(107, "Sensor 1 - interruption/short-circuit (Fuehler 1 - Unterbrechung/Kurzschluss"), //
	SENSOR2(108, "Sensor 2 - interruption/short-circuit (Fuehler 2 - Unterbrechung/Kurzschluss"), //
	OUTSIDE_SENSOR(109, "Outside sensor - interruption/short-circuit (Außenfuehler - Unterbrechung/Kurzschluss"), //
	ENCLOSURE_SENSOR(110, "Enclosure sensor - interruption/short-circuit (Kapselfuehler - Unterbrechung/Kurzschluss"), //
	CONTROLLER_INTERNAL_SENSOR(111,
			"Controller internal sensor - interruption/short-circuit (Fuehler Regler "
					+ "intern - Unterbrechung/Kurzschluss"), //
	ENGINE_OUTLET_EXHAUST_TEMP_HIGH(120,
			"Engine outlet exhaust temperature too high (Abgastemperatur Motor-Austritt "
					+ "- zu hoch;G:>620°C,HR:>520°C"), //
	ENCLOSURE_TEMP_HIGH(121, "Enclosure temperature too high (Kapseltemperatur - zu hoch; > 120°C"), //
	ENGINE_WATER_TEMP_HIGH(122,
			"Engine water temperature too high (Kuehlwassertemperatur Motor (Austritt) - zu hoch; > 95°C"), //
	OUTLET_EXHAUST_TEMP_HIGH(123,
			"Dachs outlet exhaust temperature too high (Abgastemperatur HKA-Austritt - zu hoch; > 210°C"), //
	GENERATOR_WATER_TEMP_HIGH(124,
			"Generator water temperature (inlet) too high (Kuehlwassertemperatur "
					+ "Generator (Eintritt) - zu hoch; > 77°C"), //
	FUEL_SUPPLY(129,
			"Reverse power - fuel supply or ignition faulty (Rueckleistung - "
					+ "Brennstoffversorgung oder Zuendung fehlerhaft"), //
	UNALLOWED_RPM(130,
			"Rotation speed in spite of switched-off starter after false start "
					+ "(Drehzahl nach Anlasser AUS - Drehzahl trotz ausgeschaltetem Anlasser bei Fehlstart"), //
	ENGINE_RPM_TOO_LOW(131, "Engine RPM too low (HKA-Anlauf < 100 U/min - 1 sek nach Anlasser ein: n < 100 U/min"), //
	ENGINE_RPM_TOO_LOW2(133,
			"Engine RPM too low (HKA-Lauf < 2300 U/min - n<2300 U/min fuer 30 sek " + "nach Erreichen 800 U/min"), //
	GENERATOR_RPM_TOO_HIGH(139,
			"Generator not connected because engine RPM too high (Generatorzuschaltung - "
					+ "keine Zuschaltung bei Start Drehzahl > 2600 U/Min"), //
	GENERATOR_RPM_NOT_STEADY(140,
			"Generator shutdown because engine RPM not steady (Generatorabschaltung - "
					+ "Drehzahl nicht im Drehzahlfenster laenger als 1 Sek"), //
	START_RELEASE_MONITORING(151, "Start release be monitoring system missing (Startfreigabe von Ueberwachung fehlt"), //
	NO_UC_DATA(152, "No UC Data at initialise - internal fault (NO UC_Daten b. Ini - interner Fehler"), //
	NO_FUEL_INFO(154, "No fuel info. - fuel type not identified (Kraftstofftyp nicht erkannt"), //
	DIFFERENT_FUEL_TYPES_DETECTED(155, "Different fuel types identified (Unterschiedliche Kraftstofftypen erkannt"), //
	VOLTAGE_FAULT(159,
			"Voltage at start - voltage fault before start (Spannung b. Start - " + "Spannungsfehler vor Start"), //
	VOLTAGE_FAULT2(160,
			"Voltage fault after generator connection (Spannung - Spannungsfehler " + "nach Generatorzuschaltung"), //
	OUTPUT_TOO_HIGH(162, "Output too high by more than 500 W (Leistung um mehr als 500 Watt zu hoch"), //
	OUTPUT_TOO_LOW(163, "Output too low by more than 500 W (Leistung um mehr als 500 Watt zuniedrig"), //
	POWER_AT_STANDSTILL(164, "Stand - Mehr als +- 200 Watt bei stehender Anlage"), //
	FREQ_AT_START(167,
			"Frequency at start - frequency fault before start (Frequenz bei Start " + "- Frequenzfehler vor Start"), //
	FREQ_FAULT(168, "Frequency fault after generator connection (Frequenzfehler nach " + "Generatorzuschaltung"), //
	OIL_PRESSURE_SWITCH(171,
			"Oil pressure switch closed for longer than 2.6 s at standstill "
					+ "(Oeldruckschalter im Stillstand laenger als 2.6s geschlossen"), //
	OIL_PRESSURE_SWITCH2(172,
			"Check oil level - oil pressure switch open for longer than 12 s during "
					+ "operation (Oelstand pruefen! - Oeldruckschalter waehrend des Laufes laenger als 12s offen"), //
	GAS1_SOLENOID_VALVE(173,
			"Gas 1 solenoid valve - leaking, shut-down takes longer than 5 s "
					+ "(MV Gas 1 / Hubmagnet - undicht, Abschaltung dauert laenger als 5 s"), //
	GAS2_SOLENOID_VALVE(174,
			"Gas 2 solenoid valve - leaking, shut-down takes longer than 5 s "
					+ "(MV Gas 2 - undicht, Abschaltung dauert laenger als 5 s"), //
	MAINTENANCE_NEEDED(177, "Maintenance needed - fault can be cleared, not anymore after +300h "
			+ "(Wartung notwendig - 1*taeglich entstoerbar; +300h=>nicht entstoerbar (Wartungsbestaetigung erf.)"), //
	TOO_MANY_START_ATTEMPTS(179,
			"4 unsuccessful start attempts - speed < 2300 rpm after 1 min (4 erfolglose "
					+ "Startversuche Drehzahl < 2300 U/min nach 1 Minute"), //
	INTERRUPTIONS_SOOT_FILTER(180,
			"Interruptions during soot filter regeneration > 4 (Unterbrechung "
					+ "RF-Abbrand > 4 - nur bei Oel: 5 Abschaltungen bei Russfilterregeneration"), //
	ROTATING_MAGNETIC_FIELD(184, "Rotating magnetic field error (Drehfeld falsch - Drehfeld pruefen"), //
	ONLY_WITH_OIL_SWITCH_(185,
			"Only with oil: Switch open (detecs fluid) (Fluessigkeitsschalter - "
					+ "nur bei Oel: Schalter geoeffnet (erkennt Fluessigkeit)"), //
	OVERSPEED(187, "Overspeed - speed > 3000 rpm (Ueberdrehzahl - Drehzahl > 3000 U/min"), //
	STARTUP_FAILED_RPM_LOW(188,
			"Startup unsuccessful, RPM not reached (4 erfolglose Startversuche " + "400 U/min < Drehzahl < 800 U/min"), //
	STARTUP_FAILED_RPM_LOW2(189,
			"Startup unsuccessful, RPM not reached (4 erfolglose Startversuche " + "Drehzahl < 400 U/min"), //
	RPM_BEFORE_START(190,
			"Speed > 15 rpm before start / oil pressure before start (Drehzahl vor "
					+ "Start > 15 U/min / Oeldruck vor Start"), //
	ENGINE_RPM_TOO_HIGH(191, "Engine RPM too high (Drehzahl > 3500 U/min - Ueberdrehzahl"), //
	LOCKED_BY_MONITORING_SW(192,
			"Dachs locked by monitoring software (UC verriegelt - Dachs von " + "Ueberwachungssoftware verriegelt"), //
	POWER_GRID_FAULT(200, "Power grid fault (Fehler Stromnetz - keine genaue Spezifikation moeglich"), //
	INTERNAL_FAULT_MSR2(201,
			"Internal fault MSR2 controller (Fehler MSR2 intern - keine genaue Spezifikation moeglich"), //
	MONITOR_CONTROL_SYNC(202,
			"Monitoring controller synchronisation fault - switch Dachs ON and OFF "
					+ "at the engine protection switch (Synchronisierung - Ueberwachungscontroller asynchron, Dachs am "
					+ "Motorschutzschalter aus- und einschalten"), //
	EEPROM_ERROR(203, "Eeprom error (Eeprom defekt - interner Fehler"), //
	DIFFERENT_RESULT(204, "Different result - internal error (Ergebnis ungleich - interner Fehler"), //
	DIFFERENCE_ON_MEASURING(205, "Difference on measuring channel (Dif auf Messkanal - interner Fehler"), //
	MULTIPLEX_ERROR(206, "Multiplexer error (Multiplexer - interner Fehler"), //
	MAIN_RELAY_ERROR(207, "Main relay error (Hauptrelais - interner Fehler"), //
	AD_ERROR(208, "A/D converter error (AD-Wandler - interner Fehler"), //
	MC_SUPPLY(209, "MC supply (Versorgung MCs - interner Fehler"), //
	SHUTDOWN_24H(210,
			"Prog. operation times - 24h shut-down via monitoring (Prog.-laufzeit - "
					+ "24h Abschaltung durch Ueberwachung"), //
	CONTROLLER_RECIPROCAL(212,
			"Reciprocal identification of the controller faulty (Gegenseitige "
					+ "Identifizierung der Controller fehlerhaft"), //
	INTERNAL_ERROR(213, "Prog. throughput internal fault (Prog.-durchlauf - interner Fehler"), //
	INTERNAL_CAN(214, "Internal CAN bus fault (Busfehler intern - Stoerung auf dem internen CAN-Bus"), //
	LINE_BREAK(215,
			"Line break between the generator contactor and generator (Leitungsunterbrechung "
					+ "zwischen Generatorschuetz und Generator"), //
	OVERVOLTAGE(216, "At least one voltage > 280 V (>40ms) (Mindestens eine Spannung > 280 V (>40ms)"), //
	IMPEDANCE_GAP(217,
			"An impedance gap > ENS threshold was measured (Impedanz- es wurde ein "
					+ "Impedanzsprung > ENS-Grenzwert gemessen"), //
	NO_VOLTAGE_X22_15(218, "No voltage present at X22/15 (U-Si am X22 fehlt - an X22/15 liegt keine Spannung an"), //
	NO_VOLTAGE_X5(219, "No voltage present at X5/2 (U-Si Kette fehlt - an X5/2 liegt keine Spannung an"), //
	NO_VOLTAGE_X22_13(220, "No voltage present at X22/13 (Gasdruck fehlt - an X22/13 liegt keine Spannung an"), //
	ACK_FAULT(221, "Acknowledgements - internal fault (Rueckmeldungen - interner Fehler"), //
	GENERATOR_ACK(222, "Generator ack. - signal at X21/7 (Rueckm Generator - Signal an X21/7"), //
	SOFTSTART_ACK(223, "Soft start ack. - signal at X21/5 (Rueckm Sanftanlauf - Signal an X21/5"), //
	SOLENOID_VALVE_ACK(224, "Solenoid valve ack. - check fuse F21 (Rueckm Magnetv. - Sicherung F21 pruefen"), //
	STARTER_ACK(225, "Starter ack. - signal at X21/8 (Rueckm Anlasser - Signal an X21/8"), //
	SOLENOID_ACK(226, "Solenoid ack. - check fuse F18 (Rueckm Hubmagnet - Sicherung F18 pruefen"), //
	FLOW_TEMP_SENS1(250,
			"Flow temperature sensor error heating circuit 1 (Vorlauffuehler Heizkreis 1 - Unterbrechung/Kurzschluss"), //
	FLOW_TEMP_SENS2(251,
			"Flow temperature sensor error heating circuit 1 (Vorlauffuehler Heizkreis 2 - Unterbrechung/Kurzschluss"), //
	HOT_WATER_TEMP_SENS(252,
			"Temperature sensor error domestic hot water (Warmwasserfuehler - Unterbrechung/Kurzschluss"), //
	TEMP_SENS_3(253, "Temperature sensor 3 error (Fuehler 3 - Unterbrechung/Kurzschluss"), //
	TEMP_SENS_4(254, "Temperature sensor 4 error (Fuehler 4 - Unterbrechung/Kurzschluss"), //
	ROOM_TEMP_1(255, "Room temperature sensor 1 error (Raumtemp. Fuehler 1 - Unterbrechung/Kurzschluss"), //
	ROOM_TEMP_2(256, "Room temperature sensor 2 error (Raumtemp. Fuehler 2 - Unterbrechung/Kurzschluss"), //
	ONLY_MULTI_MODULE(270,
			"Only with multi module - more than one master controller set (Nur bei "
					+ "MehrModul und LR: mehr als ein Leitregler eingestellt"), //
	ONLY_MULTI_MODULE2(271,
			"Only with multi module - duplicate controller address (Nur bei MehrModul "
					+ "und LR: Regler-Adresse mehrfach belegt"), //
	EEP_DATA(350, "EEP data RP not OK"), //
	USER_STACK(354, "User stack > nominal (User Stack > Soll - interner Fehler"), //
	INTERNAL_STACK(355, "Internal stack > nominal (Int. Stack > Soll - interner Fehler") //

	;

	private final int value;
	private final String name;

	private ErrorCode(int value, String name) {
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
	 * Gets the Enum Constant by its value.
	 * 
	 * @param value the value
	 * @return the Enum Constant
	 */
	public static OptionsEnum getByValue(int value) {
		for (ErrorCode ec : ErrorCode.values()) {
			if (ec.getValue() == value) {
				return ec;
			}
		}
		return UNDEFINED;
	}

}
