package io.openems.edge.meter.gmc.em2389;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

public interface MeterGmcEm2389 extends ElectricityMeter, OpenemsComponent {

	/**
	 * Returns 1 if value is above or equals 0, negative 1 if not.
	 */
	public static final ElementToChannelConverter SIGNUM_NON_ZERO = new ElementToChannelConverter(value -> {
		if (value != null) {
			if ((double) value >= 0) {
				return 1;
			}
			return -1;
		}
		return 1;
	});

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		VOLTAGE_L1_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		VOLTAGE_L2_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		VOLTAGE_L3_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		VOLTAGE_BETWEEN_PHASES_MEAN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		VOLTAGE_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).text("Scale factor") //
		), //
		MEAN_PHASE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		CURRENT_N(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		CURRENT_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).text("Scale factor") //
		), //
		POWER_FACTOR_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		POWER_FACTOR_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		POWER_FACTOR_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		SIGN_L1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		SIGN_L2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		SIGN_L3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		POWER_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).text("Scale factor") //
		), //
		SECONDARY_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		REACTIVE_CONSUMPTION_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		REACTIVE_PRODUCTION_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		ENERGY_SF(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE).text("Scale factor") //
		), //
		PRIMARY_ENERGY_FACTOR(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		OPERATING_HOURS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HOUR) //
				.accessMode(AccessMode.READ_ONLY) //
		), //
		READ_ONCE_ERROR(Doc.of(Level.FAULT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Could not read Scale Factor. Restart the component or check the meter for errors.") //
		);

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#SIGN_L1}.
	 *
	 * @return returns the channel
	 *
	 */
	private IntegerReadChannel getSignL1Channel() {
		return this.channel(ChannelId.SIGN_L1);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SIGN_L2}.
	 *
	 * @return returns the channel
	 *
	 */
	private IntegerReadChannel getSignL2Channel() {
		return this.channel(ChannelId.SIGN_L2);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SIGN_L3}.
	 *
	 * @return returns the channel
	 *
	 */
	private IntegerReadChannel getSignL3Channel() {
		return this.channel(ChannelId.SIGN_L3);
	}

	/**
	 * Internal method to get the sign of {@link ChannelId#SIGN_L1} Channel.
	 *
	 * @return value of firmware version value
	 */
	public default Value<Integer> getSignL1() {
		return this.getSignL1Channel().value();
	}

	/**
	 * Internal method to get the sign of {@link ChannelId#SIGN_L2} Channel.
	 *
	 * @return value of firmware version value
	 */
	public default Value<Integer> getSignL2() {
		return this.getSignL2Channel().value();
	}

	/**
	 * Internal method to get the sign of {@link ChannelId#SIGN_L3} Channel.
	 *
	 * @return value of firmware version value
	 */
	public default Value<Integer> getSignL3() {
		return this.getSignL3Channel().value();
	}

}
