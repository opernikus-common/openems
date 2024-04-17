package io.openems.edge.io.acthor;

import java.util.Optional;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface AcThor extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Power")),
		TEMP_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Temp 1")),
		H_W_1_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Hot Water 1 max")),
		STATUS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Status")),
		POWER_TIMEOUT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Power Timeout")),
		BOOST_MODE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Boost Mode")),
		H_W_1_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Hot Water 1 min")),
		BOOST_TIME_1_START(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Boost time 1 start")),
		BOOST_TIME_1_STOP(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Boost time 1 stop")),
		BOOST_ACTIVATE(Doc.of(OpenemsType.BOOLEAN) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Boost activate")),
		AC_THOR_NUMBER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("AC Thor number")),
		MAX_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("max Power")),
		TEMPCHIP(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("tempchip")),
		CONTROL_FIRMWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Control Firmware version")),
		PS_FIRMWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("PS Firmware version")),
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Serial number")),
		BOOST_TIME_2_START(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Boost time 2 start")),
		BOOST_TIME_2_STOP(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Boost time 2 stop")),
		CONTROL_FIRMWARE_SUB_VERSION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Control Firmware sub version")),
		TEMP_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Temp 2")),
		TEMP_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Temp 3")),
		TEMP_4(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Temp 4")),
		H_W_2_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Hot Water 2 max")),
		H_W_3_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Hot Water 3 max")),
		H_W_2_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Hot Water 2 min")),
		H_W_3_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Hot Water 3 min")),
		RH_1_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Room heating 1 max")),
		RH_2_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Room heating 2 max")),
		RH_3_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Room heating 3 max")),
		RH_1_DAY_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Room heating 1 day min")),
		RH_2_DAY_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Room heating 2 day min")),
		RH_3_DAY_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Room heating 3 day min")),
		RH_1_NIGHT_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Room heating 1 night min")),
		RH_2_NIGHT_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Room heating 2 night min")),
		RH_3_NIGHT_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Room heating 3 night min")),
		NIGHT_FLAG(Doc.of(OpenemsType.BOOLEAN) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Night flag")),
		STRATIFACTION_FLAG(Doc.of(OpenemsType.BOOLEAN) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Stratifaction flag")),
		RELAY_1_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Relay 1 status")),
		LOAD_NOMINAL_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("load nominal power")),
		VOLTAGE_OUT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Voltage out")),
		METER_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Meter Power")),
		PMAX_ABS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Max power currently possible")),
		P_OUT_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("P out 1")),
		P_OUT_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("P out 2")),
		P_OUT_3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("P out 3")),
		OPERATION_STATE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Operation state")),
		DEVICE_STATE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Device state")),
		POWER_OF_QUERIED_DEVICE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Power of the queried device")),
		SOLAR_PART_OF_DEVICE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Solar part of device power")),
		GRID_PART_OF_DEVICE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW) //
				.text("Grid part of device power")),

		// Dummy channels to make it IO
		OUT_1(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE)),
		OUT_2(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE)),
		OUT_3(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE)),;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public default IntegerWriteChannel getPowerWriteChannel() {
		return this.channel(ChannelId.POWER);
	}

	public default Optional<Integer> getWritePower() {
		return this.getPowerWriteChannel().value().asOptional();
	}

	public default void setWritePower(Integer value) throws OpenemsNamedException {
		this.getPowerWriteChannel().setNextWriteValue(value);
	}

	public default BooleanWriteChannel getOut1WriteChannel() {
		return this.channel(ChannelId.OUT_1);
	}

	public default Optional<Boolean> getOut1() {
		return this.getOut1WriteChannel().value().asOptional();
	}

	public default BooleanWriteChannel getOut2WriteChannel() {
		return this.channel(ChannelId.OUT_2);
	}

	public default Optional<Boolean> getOut2() {
		return this.getOut2WriteChannel().value().asOptional();
	}

	public default BooleanWriteChannel getOut3WriteChannel() {
		return this.channel(ChannelId.OUT_3);
	}

	public default Optional<Boolean> getOut3() {
		return this.getOut3WriteChannel().value().asOptional();
	}

}
