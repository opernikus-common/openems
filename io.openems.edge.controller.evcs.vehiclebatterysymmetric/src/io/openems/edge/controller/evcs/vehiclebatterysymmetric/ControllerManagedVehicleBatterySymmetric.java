package io.openems.edge.controller.evcs.vehiclebatterysymmetric;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerManagedVehicleBatterySymmetric extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * The mode of this controller.
		 *
		 * <ul>
		 * <li>Read-only
		 * <li>Type: Mode
		 * </ul>
		 */
		MODE(Doc.of(Mode.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //

		DEBUG_REQ_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		DISCHARGING_BLOCKED(Doc.of(Level.INFO) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Entladen verzögert")), //
		CHARGING_BLOCKED(Doc.of(Level.INFO) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Laden verzögert")), //
		CHARGING_CYCLE_COUNTER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)), //
		DISCHARGING_CYCLE_COUNTER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)) //

		;

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
	 * Gets the Channel for {@link ChannelId#DEBUG_REQ_ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDebugReqActivePowerChannel() {
		return this.channel(ChannelId.DEBUG_REQ_ACTIVE_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MODE}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getModeChannel() {
		return this.channel(ChannelId.MODE);
	}

	/**
	 * Gets the Mode of the controller. See {@link ChannelId#MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Mode getMode() {
		return this.getModeChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MODE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMode(Mode value) {
		this.getModeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGING_BLOCKED}.
	 *
	 * @return the channel
	 */
	public default StateChannel getChargingBlockedChannel() {
		return this.channel(ChannelId.CHARGING_BLOCKED);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CHARGING_BLOCKED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargingBlocked(boolean value) {
		this.getChargingBlockedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DISCHARGING_BLOCKED}.
	 *
	 * @return the channel
	 */
	public default StateChannel getDischargingBlockedChannel() {
		return this.channel(ChannelId.DISCHARGING_BLOCKED);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DISCHARGING_BLOCKED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDischargingBlocked(boolean value) {
		this.getDischargingBlockedChannel().setNextValue(value);
	}

}
