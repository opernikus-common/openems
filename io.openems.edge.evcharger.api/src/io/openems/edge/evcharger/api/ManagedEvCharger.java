package io.openems.edge.evcharger.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcharger.api.data.OperationMode;
import io.openems.edge.evcharger.api.data.Phases;
import io.openems.edge.evcharger.api.data.Priority;

/**
 * ManagedEvCharger represents a fully operational and manageable
 * EvCharger chargepoint.
 */
public interface ManagedEvCharger extends OpenemsComponent {

	public enum ConfigChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * OperationMode of the underlying EvCharger.
		 * 
		 * <p>
		 * Initially configured by this controller. May be changed via UI by a User
		 * during runtime.
		 * 
		 * <p>
		 * Note: Best for dynamic tariffs is operation mode AUTO.
		 *
		 * <ul>
		 * <li>Interface: ManagedEvCharger
		 * <li>Writable
		 * <li>Type: OperationMode @see {@link OperationMode}
		 * </ul>
		 */
		OPERATION_MODE(Doc.of(OperationMode.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Priority of the underlying EvCharger.
		 *
		 * <ul>
		 * <li>Interface: ManagedEvCharger
		 * <li>Writable
		 * <li>Type: Priority @see {@link Priority}
		 * </ul>
		 */
		PRIORITY(Doc.of(Priority.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Sets the energy limit for the current or next session in [Wh].
		 *
		 * <ul>
		 * <li>Interface: ManagedEvCharger
		 * <li>Writable
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#WATTHOUR}
		 * </ul>
		 */
		SESSION_ENERGY_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH)), //

		; //

		private final Doc doc;

		private ConfigChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		WARN_CONFIG_ENERGY_LIMIT(Doc.of(Level.WARNING) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Configured EnergyLimit is invalid.")), //

		/**
		 * Sets a fixed Active Power per phase (valid for all phases).
		 * 
		 * <p>
		 * 
		 *
		 * <ul>
		 * <li>Interface: ManagedEvCharger
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive value
		 * </ul>
		 */
		ACTIVE_POWER_PER_PHASE(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE) //
		), //

		/**
		 * Limits the number of used phases of the chargepoint (when hardware is capable
		 * of limitation).
		 * 
		 * <p>
		 * Typical charge sessions take at least 6A per phase. On a car capable of
		 * charging with 3 phases, this means 4140W (=230V*6A*3). In case of PV excess
		 * charging, this means, that car will be charged only when at least 4140W
		 * excess is available. Before reaching this limit, PV excess will be feed to
		 * grid (and be lost for the customer). Charging with only one phase reduces
		 * this to 1380W.
		 *
		 * <ul>
		 * <li>Interface: ManagedEvCharger
		 * <li>Type: Integer
		 * <li>Unit: None
		 * <li>Range: 1-3
		 * </ul>
		 */
		LIMIT_PHASES(new IntegerDoc() //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
		), //

		/**
		 * Gets the smallest power steps that can be set (given in W).
		 *
		 * <p>
		 * Example:
		 * <ul>
		 * <li>KEBA-series allows setting of milli Ampere. It should return 0.23 W
		 * (0.001A * 230V). As this channel is an INTEGER it should return 1 W
		 * <li>Hardy Barth allows setting in Ampere. It should return 230 W (1A * 230V).
		 * </ul>
		 *
		 * <p>
		 * <ul>
		 * <li>Interface: ManagedEvCharger
		 * <li>Readable
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#WATT}
		 * </ul>
		 */
		POWER_PRECISION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Is true if the EVCS is in a EVCS-Cluster.
		 *
		 * <ul>
		 * <li>Interface: ManagedEvCharger
		 * <li>Readable
		 * <li>Type: Boolean
		 * </ul>
		 */
		SESSION_SUSPENDABLE(Doc.of(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Count of phases, the EvCharger is charging with.
		 *
		 * <p>
		 * This value is derived from the charging station or calculated during the
		 * charging.
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvCharger
		 * <li>Readable
		 * <li>Type: Integer
		 * </ul>
		 */
		SESSION_PHASES(Doc.of(Phases.values()) //
				// .debounce(5) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //

		; //

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
	 * Gets the Channel for {@link ConfigChannelId#ACTIVE_POWER_PER_PHASE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getActivePowerPerPhaseChannel() {
		return this.channel(ChannelId.ACTIVE_POWER_PER_PHASE);
	}

	/**
	 * Gets the active power per channel read value in [W]. See
	 * {@link ConfigChannelId#ACTIVE_POWER_PER_PHASE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerPerPhase() {
		return this.getActivePowerPerPhaseChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ConfigChannelId#ACTIVE_POWER_PER_PHASE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActivePowerPerPhase(Integer value) {
		this.getActivePowerPerPhaseChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ConfigChannelId#ACTIVE_POWER_PER_PHASE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setActivePowerPerPhase(int value) {
		this.getActivePowerPerPhaseChannel().setNextValue(value);
	}

	/**
	 * Sets the active power per phase read value in [W]. See
	 * {@link ChannelId#ACTIVE_POWER_PER_PHASE}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setActivePowerPerPhase(Integer value) throws OpenemsNamedException {
		this.getActivePowerPerPhaseChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ConfigChannelId#LIMIT_PHASES}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getLimitPhasesChannel() {
		return this.channel(ChannelId.LIMIT_PHASES);
	}

	/**
	 * Gets the limit phases channel. See {@link ConfigChannelId#LIMIT_PHASES}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getLimitPhases() {
		return this.getLimitPhasesChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ConfigChannelId#LIMIT_PHASES} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setLimitPhases(Integer value) {
		this.getLimitPhasesChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ConfigChannelId#LIMIT_PHASES} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setLimitPhases(int value) {
		this.getLimitPhasesChannel().setNextValue(value);
	}

	/**
	 * Sets the active power per phase read value in [W]. See
	 * {@link ChannelId#LIMIT_PHASES}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setLimitPhases(Integer value) throws OpenemsNamedException {
		this.getLimitPhasesChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ConfigChannelId#OPERATION_MODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<OperationMode> getOperationModeChannel() {
		return this.channel(ConfigChannelId.OPERATION_MODE);
	}

	/**
	 * Gets the OperationMode of the underlying evcharger. See
	 * {@link ConfigChannelId#OPERATION_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default OperationMode getChargeMode() {
		return this.getOperationModeChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ConfigChannelId#OPERATION_MODE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setOperationMode(OperationMode value) {
		this.getOperationModeChannel().setNextValue(value);
	}

	/**
	 * Sets the OperationMode. See {@link ConfigChannelId#OPERATION_MODE} Channel.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOperationMode(OperationMode value) throws OpenemsNamedException {
		EnumWriteChannel rc = (EnumWriteChannel) this.channel(ConfigChannelId.OPERATION_MODE);
		rc.setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#POWER_PRECISION}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPowerPrecisionChannel() {
		return this.channel(ChannelId.POWER_PRECISION);
	}

	/**
	 * Gets the power precision value of the EVCS in [W]. See
	 * {@link ChannelId#POWER_PRECISION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPowerPrecision() {
		return this.getPowerPrecisionChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#POWER_PRECISION}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPowerPrecision(Integer value) {
		this.getPowerPrecisionChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#POWER_PRECISION}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPowerPrecision(double value) {
		this.getPowerPrecisionChannel().setNextValue(value);
	}



	/**
	 * Gets the Channel for {@link ConfigChannelId#PRIORITY}.
	 *
	 * @return the Channel
	 */
	public default Channel<Priority> getPriorityChannel() {
		return this.channel(ConfigChannelId.PRIORITY);
	}

	/**
	 * Gets the Priority of the Managed EVCS charging station. See
	 * {@link ConfigChannelId#PRIORITY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Priority getPriority() {
		return this.getPriorityChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ConfigChannelId#PRIORITY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPriority(Priority value) {
		this.getPriorityChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SESSION_SUSPENDABLE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSessionSuspendableChannel() {
		return this.channel(ChannelId.SESSION_SUSPENDABLE);
	}

	/**
	 * Gets true if the current EvCharger session is suspendable. See
	 * {@link ChannelId#SESSION_SUSPENDABLE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getIsSessionSuspendable() {
		return this.getSessionSuspendableChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SESSION_SUSPENDABLE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setIsSessionSuspendable(boolean value) {
		this.getSessionSuspendableChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SESSION_PHASES}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getSessionPhasesChannel() {
		return this.channel(ChannelId.SESSION_PHASES);
	}

	/**
	 * Gets the current Phases definition. See {@link ChannelId#SESSION_PHASES}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Phases getSessionPhases() {
		return this.getSessionPhasesChannel().value().asEnum();
	}

	/**
	 * Gets the Count of phases, the EvCharger is charging with. See
	 * {@link ChannelId#SESSION_PHASES}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default int getSessionPhasesAsInt() {
		return this.getSessionPhasesChannel().value().asEnum().getValue();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SESSION_PHASES}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSessionPhases(Phases value) {
		this.getSessionPhasesChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SESSION_PHASES}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSessionPhases(Integer value) {
		if (value == null || value == 0) {
			this._setSessionPhases(Phases.THREE_PHASE);
			return;
		}
		switch (value) {
		case 1:
			this._setSessionPhases(Phases.ONE_PHASE);
			break;
		case 2:
			this._setSessionPhases(Phases.TWO_PHASE);
			break;
		case 3:
			this._setSessionPhases(Phases.THREE_PHASE);
			break;
		default:
			throw new IllegalArgumentException("Value [" + value + "] for _setPhases is invalid");
		}
	}

	/**
	 * Gets the Channel for {@link ConfigChannelId#SESSION_ENERGY_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSessionEnergyLimitChannel() {
		return this.channel(ConfigChannelId.SESSION_ENERGY_LIMIT);
	}

	/**
	 * Gets the energy limit for the current or next session in [Wh]. See
	 * {@link ConfigChannelId#SESSION_ENERGY_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSessionEnergyLimit() {
		return this.getSessionEnergyLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ConfigChannelId#SESSION_ENERGY_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSessionEnergyLimit(Integer value) {
		this.getSessionEnergyLimitChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ConfigChannelId#SESSION_ENERGY_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSessionEnergyLimit(int value) {
		this.getSessionEnergyLimitChannel().setNextValue(value);
	}

	/**
	 * Sets the energy limit for the current or next session in [Wh]. See
	 * {@link ChannelId#SESSION_ENERGY_LIMIT}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSessionEnergyLimit(Integer value) throws OpenemsNamedException {
		this.getSessionEnergyLimitChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#WARN_CONFIG_ENERGY_LIMIT}.
	 *
	 * @return the StateChannel
	 */
	public default StateChannel getWarnConfigEnergyLimitChannel() {
		return this.channel(ChannelId.WARN_CONFIG_ENERGY_LIMIT);
	}

	/**
	 * Gets the warn config energy limit status. See
	 * {@link ChannelId#WARN_CONFIG_ENERGY_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getWarnConfigEnergyLimit() {
		return this.getWarnConfigEnergyLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#WARN_CONFIG_ENERGY_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWarnConfigEnergyLimit(boolean value) {
		this.getWarnConfigEnergyLimitChannel().setNextValue(value);
	}
}