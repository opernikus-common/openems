package io.openems.edge.evcs.abl;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface EvcsAbl extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CHARGE_POINT_STATE(Doc.of(OpenemsType.SHORT) //
				.accessMode(AccessMode.READ_ONLY).persistencePriority(PersistencePriority.HIGH)), //
		CHARGING_CURRENT(Doc.of(OpenemsType.SHORT) //
				.unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_WRITE).persistencePriority(PersistencePriority.HIGH)); //

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}

	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_POINT_STATE}.
	 *
	 * @return the Channel
	 */
	default Channel<Short> getChargePointStateChannel() {
		return this.channel(ChannelId.CHARGE_POINT_STATE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_POINT_STATE}.
	 *
	 * @return the Channel
	 */
	default short getChargePointState() {
		Channel<Short> channel = this.getChargePointStateChannel();
		return channel.value().orElse(channel.getNextValue().orElse((short) -1));
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGING_CURRENT}.
	 *
	 * @return the Channel
	 */
	default WriteChannel<Short> getCurrentLimitChannel() {
		return this.channel(ChannelId.CHARGING_CURRENT);
	}

	/**
	 * Gets the Value of {@link ChannelId#CHARGING_CURRENT}.
	 *
	 * @return the value
	 */
	default short getCurrentLimit() {
		WriteChannel<Short> channel = this.getCurrentLimitChannel();
		return channel.value().orElse(channel.getNextValue().orElse((short) 0));
	}

	/**
	 * Sets a value into the CurrentLimit register. See
	 * {@link ChannelId#CHARGING_CURRENT}.
	 *
	 * @param value the next write value
	 * @throws OpenemsError.OpenemsNamedException on error
	 */
	default void setCurrentLimit(int value) throws OpenemsError.OpenemsNamedException {
		WriteChannel<Short> channel = this.getCurrentLimitChannel();
		channel.setNextWriteValue((short) value);
	}

	/**
	 * Sets a value into the CurrentLimit Channel ReadBack. See
	 * {@link ChannelId#CHARGING_CURRENT}.
	 *
	 * @param value the next value
	 * @throws OpenemsError.OpenemsNamedException on error
	 */
	default void _setCurrentLimit(int value) throws OpenemsError.OpenemsNamedException {
		WriteChannel<Short> channel = this.getCurrentLimitChannel();
		channel.setNextValue((short) value);
	}
}
