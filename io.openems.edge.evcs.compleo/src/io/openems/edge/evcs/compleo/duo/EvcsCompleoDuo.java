package io.openems.edge.evcs.compleo.duo;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface EvcsCompleoDuo extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CHARGE_POINT_STATE(Doc.of(OpenemsType.SHORT) //
				.accessMode(AccessMode.READ_ONLY)), //
		POWER_LIMIT(Doc.of(OpenemsType.SHORT) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)); //

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
	public default Channel<Integer> getChargePointStateChannel() {
		return this.channel(ChannelId.CHARGE_POINT_STATE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_POINT_STATE}.
	 *
	 * @return the Channel
	 */
	public default Value<Integer> getChargePointState() {
		return this.getChargePointStateChannel().value();
	}


	/**
	 * Gets the Channel for {@link ChannelId#POWER_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<Integer> getPowerLimitChannel() {
		return this.channel(ChannelId.POWER_LIMIT);
	}


	/**
	 * Sets a value into the PowerLimit register. See
	 * {@link ChannelId#POWER_LIMIT}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setPowerLimit(int value) throws OpenemsNamedException {
		this.getPowerLimitChannel().setNextWriteValue(value);
	}
}
