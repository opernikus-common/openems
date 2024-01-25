package io.openems.edge.evcs.hardybarth.meter;

import java.util.function.Function;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;


public interface HardyBarth {
	
//	public static final Function<Object, Object> floatToIntConverter = new Function<Object, Object> (){
//		public Object apply(Object x) {
//			return 0;
//		}
//	};

	
	/*
	  http://apidoc.ecb1.de/
	  meter data mapping
		lgwb        Saldo Active power+/-
		1-0:1.4.0   Active power+      in W
		1-0:1.8.0 	Active energy+     in kWh
		1-0:2.4.0 	Active power-
		1-0:2.8.0 	Active energy-
		1-0:13.4.0 	Power factor
		1-0:21.4.0 	Active power+ (L1)
		1-0:21.8.0 	Active energy+ (L1)
		1-0:22.4.0 	Active power- (L1)
		1-0:22.8.0 	Active energy- (L1)
		1-0:31.4.0 	Current (L1)
		1-0:32.4.0 	Voltage (L1)
		1-0:33.4.0 	Power factor (L1)
		1-0:41.4.0 	Active power+ (L2)
		1-0:41.8.0 	Active energy+ (L2)
		1-0:42.4.0 	Active power- (L2)
		1-0:42.8.0 	Active energy- (L2)
		1-0:51.4.0 	Current (L2)
		1-0:52.4.0 	Voltage (L2)
		1-0:53.4.0 	Power factor (L2)
		1-0:61.4.0 	Active power+ (L3)
		1-0:61.8.0 	Active energy+ (L3)
		1-0:62.4.0 	Active power- (L3)
		1-0:62.8.0 	Active energy- (L3)
		1-0:71.4.0 	Current (L3)
		1-0:72.4.0 	Voltage (L3)
		1-0:73.4.0 	Power factor (L3)   
*/

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

//		// TODO: Correct Type & Unit (Waiting for Manufacturer instructions)
//

		// METERING - METER
		RAW_DEVICE_ID(Doc.of(OpenemsType.INTEGER), "id"), //
		RAW_METER_SERIALNUMBER(Doc.of(OpenemsType.INTEGER), "serial"), //
		RAW_METER_TYPE(Doc.of(OpenemsType.STRING), "type"), //
		RAW_METER_VENDOR(Doc.of(OpenemsType.STRING), "vendor"), //
		RAW_METER_NAME(Doc.of(OpenemsType.STRING), "name"), //
//
		// METERING - POWER +
		RAW_ACTIVE_POWER_PLUS(   Doc.of(OpenemsType.FLOAT).unit(Unit.WATT), "data", "1-0:1.4.0"), //
		RAW_ACTIVE_POWER_PLUS_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT), "data", "1-0:21.4.0"), //
		RAW_ACTIVE_POWER_PLUS_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT), "data", "1-0:41.4.0"), //
		RAW_ACTIVE_POWER_PLUS_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT), "data", "1-0:61.4.0"), //
		RAW_ACTIVE_POWER_MINUS(   Doc.of(OpenemsType.FLOAT).unit(Unit.WATT), "data", "1-0:2.4.0"), //
		RAW_ACTIVE_POWER_MINUS_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT), "data", "1-0:22.4.0"), //
		RAW_ACTIVE_POWER_MINUS_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT), "data", "1-0:42.4.0"), //
		RAW_ACTIVE_POWER_MINUS_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT), "data", "1-0:62.4.0"), //
		RAW_ACTIVE_POWER_FACTOR(   Doc.of(OpenemsType.FLOAT).unit(Unit.NONE), "data", "1-0:13.4.0"), //
		RAW_ACTIVE_POWER_FACTOR_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE), "data", "1-0:33.4.0"), //
		RAW_ACTIVE_POWER_FACTOR_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE), "data", "1-0:53.4.0"), //
		RAW_ACTIVE_POWER_FACTOR_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE), "data", "1-0:73.4.0"), //

		// METERING - CURRENT
		RAW_ACTIVE_CURRENT_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE), "data", "1-0:31.4.0"), //
		RAW_ACTIVE_CURRENT_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE), "data", "1-0:51.4.0"), //
		RAW_ACTIVE_CURRENT_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE), "data", "1-0:71.4.0"), //
		
		// METERING - VOLTAGE
		RAW_ACTIVE_VOLTAGE_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.MILLIVOLT), "data", "1-0:32.4.0"), //
		RAW_ACTIVE_VOLTAGE_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.MILLIVOLT), "data", "1-0:52.4.0"), //
		RAW_ACTIVE_VOLTAGE_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.MILLIVOLT), "data", "1-0:72.4.0"), //

		// METERING - ENERGY
		RAW_ACTIVE_ENERGY_PLUS(    Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS), "data", "1-0:1.8.0"), //
		RAW_ACTIVE_ENERGY_PLUS_L1 (Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS), "data", "1-0:21.8.0"), //
		RAW_ACTIVE_ENERGY_PLUS_L2 (Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS), "data", "1-0:41.8.0"), //
		RAW_ACTIVE_ENERGY_PLUS_L3 (Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS), "data", "1-0:61.8.0"), //
		RAW_ACTIVE_ENERGY_MINUS(   Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS), "data", "1-0:2.8.0"), //		
		RAW_ACTIVE_ENERGY_MINUS_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS), "data", "1-0:22.8.0"), //		
		RAW_ACTIVE_ENERGY_MINUS_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS), "data", "1-0:42.8.0"), //		
		RAW_ACTIVE_ENERGY_MINUS_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS), "data", "1-0:62.8.0"), //
		
		// METERING - SALDO Active Power +/-
		RAW_ACTIVE_POWER_SALDO(Doc.of(OpenemsType.INTEGER), "data", "lgwb"), //
		
		RAW_METER_PROTOCOL_VERSION(Doc.of(OpenemsType.STRING), "protocol-version"), //
		;

		private final Doc doc;
		private final String[] jsonPaths;



		

		
		public final Function<Object, Object> converter;

		private ChannelId(Doc doc, String... jsonPaths) {
			this(doc, (value) -> value, jsonPaths);
		}

		private ChannelId(Doc doc, Function<Object, Object> converter, String... jsonPaths) {
			this.doc = doc;
			this.converter = converter;
			this.jsonPaths = jsonPaths;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

		/**
		 * Get the whole JSON path.
		 * 
		 * @return Whole path.
		 */
		public String[] getJsonPaths() {
			return this.jsonPaths;
		}
	}
}