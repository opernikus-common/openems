package io.openems.edge.evcs.stoehr.designtower;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface EvcsDesigntower extends OpenemsComponent {
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)), //
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)), //
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)), //
		CHARGE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.LOW)), //
		CHARGE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.LOW)), //
		CHARGE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.LOW)), //
		ENERGY_L1(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)), //
		ENERGY_L2(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)), //
		ENERGY_L3(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)), //
		REQUIRED_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)), //
		FIRMWARE_VERSION(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		MAX_EV_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)), //
		OCPP_CP_STATUS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.LOW)), //
		CHARGING_DURATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.persistencePriority(PersistencePriority.LOW)), //
		SAFE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)), //
		SET_CHARGING_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH)); //

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
	 * Gets the channel for {@link ChannelId#SET_CHARGING_CURRENT}.
	 * 
	 * @return the Channel
	 */
	public default WriteChannel<Integer> getSetChargingCurrentChannel() {
		return this.channel(ChannelId.SET_CHARGING_CURRENT);
	}

	/**
	 * Sets the charging current of the Evcs in [A]. See
	 * {@link ChannelId#SET_CHARGING_CURRENT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setChargingCurrent(Integer value) throws OpenemsNamedException {
		this.getSetChargingCurrentChannel().setNextWriteValue(value);
	}

}
