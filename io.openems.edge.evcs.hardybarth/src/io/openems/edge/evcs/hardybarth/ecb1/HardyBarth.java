package io.openems.edge.evcs.hardybarth.ecb1;

import java.util.Optional;
import java.util.function.Function;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.OpenemsComponent;


public interface HardyBarth extends OpenemsComponent{
	
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
//		// EVSE
//		RAW_EVSE_GRID_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE), "secc", "port0", "ci", "evse", //
//				"basic", "grid_current_limit", "actual"), //
//		RAW_PHASE_COUNT(Doc.of(OpenemsType.INTEGER), "secc", "port0", "ci", "evse", "basic", "phase_count"), //
//
//		// CHARGE
		RAW_CHARGE_CONTROL_ID(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.LOW), "chargecontrol", "id"),
		RAW_CHARGE_CONTROL_STATE(Doc.of(OpenemsType.STRING).persistencePriority(PersistencePriority.LOW), "chargecontrol", "state"), //
		/**
		 * modes of the Hardy Barth.
		 * 
		 * <ul>
		 * <li>quick
		 * <li>eco
		 * <li>manual
		 * </ul>
		 */
		RAW_CHARGE_CONTROL_MODE(Doc.of(OpenemsType.STRING).persistencePriority(PersistencePriority.LOW), "chargecontrol", "mode"), //
		RAW_CHARGE_CONTROL_MODE_ID(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.LOW), "chargecontrol", "modeid"), //
		RAW_CHARGE_CONTROL_STATE_ID(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.LOW), "chargecontrol", "stateid"), //
		RAW_CHARGE_CONTROL_CONNECTED(Doc.of(OpenemsType.BOOLEAN).persistencePriority(PersistencePriority.MEDIUM), "chargecontrol", "connected"), //
		RAW_CHARGE_CONTROL_VERSION(Doc.of(OpenemsType.STRING).persistencePriority(PersistencePriority.LOW), "chargecontrol", "version"), //

		// METERING - METER
		RAW_DEVICE_ID(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.LOW), "meter", "id"), //
		RAW_METER_SERIALNUMBER(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.LOW), "meter", "serial"), //
		RAW_METER_TYPE(Doc.of(OpenemsType.STRING).persistencePriority(PersistencePriority.LOW), "meter", "type"), //
		RAW_METER_VENDOR(Doc.of(OpenemsType.STRING).persistencePriority(PersistencePriority.LOW), "meter", "vendor"), //
		RAW_METER_NAME(Doc.of(OpenemsType.STRING).persistencePriority(PersistencePriority.LOW), "meter", "name"), //
//
		// METERING - POWER +
		RAW_ACTIVE_POWER_PLUS(   Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:1.4.0"), //
		RAW_ACTIVE_POWER_PLUS_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:21.4.0"), //
		RAW_ACTIVE_POWER_PLUS_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:41.4.0"), //
		RAW_ACTIVE_POWER_PLUS_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:61.4.0"), //
		RAW_ACTIVE_POWER_MINUS(   Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:2.4.0"), //
		RAW_ACTIVE_POWER_MINUS_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:22.4.0"), //
		RAW_ACTIVE_POWER_MINUS_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:42.4.0"), //
		RAW_ACTIVE_POWER_MINUS_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:62.4.0"), //
		RAW_ACTIVE_POWER_FACTOR(   Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:13.4.0"), //
		RAW_ACTIVE_POWER_FACTOR_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:33.4.0"), //
		RAW_ACTIVE_POWER_FACTOR_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:53.4.0"), //
		RAW_ACTIVE_POWER_FACTOR_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:73.4.0"), //

		// METERING - CURRENT
		RAW_ACTIVE_CURRENT_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:31.4.0"), //
		RAW_ACTIVE_CURRENT_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:51.4.0"), //
		RAW_ACTIVE_CURRENT_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:71.4.0"), //
		
		// METERING - VOLTAGE
		RAW_ACTIVE_VOLTAGE_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.MILLIVOLT).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:32.4.0"), //
		RAW_ACTIVE_VOLTAGE_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.MILLIVOLT).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:52.4.0"), //
		RAW_ACTIVE_VOLTAGE_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.MILLIVOLT).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:72.4.0"), //

		// METERING - ENERGY
		RAW_ACTIVE_ENERGY_PLUS(    Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS).persistencePriority(PersistencePriority.MEDIUM), "meter", "data", "1-0:1.8.0"), //
		RAW_ACTIVE_ENERGY_PLUS_L1 (Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:21.8.0"), //
		RAW_ACTIVE_ENERGY_PLUS_L2 (Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:41.8.0"), //
		RAW_ACTIVE_ENERGY_PLUS_L3 (Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:61.8.0"), //
		RAW_ACTIVE_ENERGY_MINUS(   Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:2.8.0"), //		
		RAW_ACTIVE_ENERGY_MINUS_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:22.8.0"), //		
		RAW_ACTIVE_ENERGY_MINUS_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:42.8.0"), //		
		RAW_ACTIVE_ENERGY_MINUS_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS).persistencePriority(PersistencePriority.LOW), "meter", "data", "1-0:62.8.0"), //
		
		// METERING - SALDO Active Power +/-
		RAW_ACTIVE_POWER_SALDO(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.LOW), "meter", "data", "lgwb"), //
		
		RAW_METER_PROTOCOL_VERSION(Doc.of(OpenemsType.STRING).persistencePriority(PersistencePriority.LOW), "protocol-version"), //

		//holds values written to the charging station
		RAW_OUT_CHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.MEDIUM), "unused"), //
		RAW_OUT_MIN_CURRENT(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.MEDIUM), "unused"), //
		TRACEPOINT_WRITE_HANDLER(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.VERY_LOW), "unused"), //
		TRACE_COMMAND_START_STOP(Doc.of(OpenemsType.BOOLEAN).persistencePriority(PersistencePriority.VERY_LOW), "unused"), //
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
	
	public default Optional<String> _getChargeControlMode(){
		StringReadChannel chargeCtrlMode = this.channel(HardyBarth.ChannelId.RAW_CHARGE_CONTROL_MODE);
		Optional<String> val = chargeCtrlMode.value().asOptional();
		return val;
	}
	
//	public default LongReadChannel getEnergyTotalChannel() {
//		return this.channel(ChannelId.ENERGY_TOTAL);
//	}
//
//	public default Value<Long> getEnergyTotal() {
//		return this.getEnergyTotalChannel().value();
//	}
//
//	public default void _setEnergyTotal(Long value) {
//		this.getEnergyTotalChannel().setNextValue(value);
//	}
	
	
	
}