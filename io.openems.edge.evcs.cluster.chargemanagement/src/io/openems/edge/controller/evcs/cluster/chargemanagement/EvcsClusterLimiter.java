package io.openems.edge.controller.evcs.cluster.chargemanagement;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface EvcsClusterLimiter extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// State channels

		CONFIG_WARN(Doc.of(Level.WARNING) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("TargetPower too close to fuseLimit.")), //
		METER_ERROR(Doc.of(Level.FAULT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Meter does not provide valid phase currents.")), //

		FREE_AVAILABLE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * The max transport capacity of the cable segment monitored by this limiter.
		 */
		TRANSPORT_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Set to false to switch evcsClusterChargeManagement to off.
		 */
		SAFE_OPERATION_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.HIGH)), //
		PHASE_IMBALANCE(Doc.of(PhaseImbalance.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //
		PHASE_IMBALANCE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //

		METER_CURRENT_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		METER_CURRENT_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		METER_CURRENT_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Free available current on L1. The free available current between current
		 * situation and targetPower (in A).
		 */
		FREE_AVAILABLE_CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Free available current on L2. The free available current between current
		 * situation and targetPower (in A).
		 */
		FREE_AVAILABLE_CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Free available current on L3. The free available current between current
		 * situation and targetPower (in A).
		 */
		FREE_AVAILABLE_CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),
		PHASE_LIMIT_EXCEEDED_L1(Doc.of(Level.INFO) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Cluster shutdown due to phase 1 above fuse safety limit.")), //
		PHASE_LIMIT_EXCEEDED_L2(Doc.of(Level.INFO) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Cluster shutdown due to phase 2 above fuse safety limit.")), //
		PHASE_LIMIT_EXCEEDED_L3(Doc.of(Level.INFO) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Cluster shutdown due to phase 3 above fuse safety limit.")), //
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
	 * Gets the Channel for {@link ChannelId#CONFIG_WARN}.
	 *
	 * @return the StateChannel
	 */
	public default StateChannel getConfigWarningChannel() {
		return this.channel(ChannelId.CONFIG_WARN);
	}

	/**
	 * Gets the configuration status. See {@link ChannelId#CONFIG_WARN}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getConfigWarning() {
		return this.getConfigWarningChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CONFIG_WARN}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConfigWarning(boolean value) {
		this.getConfigWarningChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#METER_ERROR}.
	 *
	 * @return the StateChannel
	 */
	public default StateChannel getMeterErrorChannel() {
		return this.channel(ChannelId.METER_ERROR);
	}

	/**
	 * Gets the Status of the EVCS charging station. See
	 * {@link ChannelId#METER_ERROR}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMeterError() {
		return this.getMeterErrorChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#METER_ERROR}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMeterError(boolean value) {
		this.getMeterErrorChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel {@link ChannelId#PHASE_IMBALANCE_CURRENT}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getPhaseImbalanceCurrentChannel() {
		return this.channel(ChannelId.PHASE_IMBALANCE_CURRENT);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#PHASE_IMBALANCE_CURRENT}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getPhaseImbalanceCurrent() {
		return this.getPhaseImbalanceCurrentChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#PHASE_IMBALANCE_CURRENT}.
	 *
	 * @param phaseImbalanceCurrent the maximum phase imbalance (in mA)
	 */
	public default void _setPhaseImbalanceCurrent(Integer phaseImbalanceCurrent) {
		this.getPhaseImbalanceCurrentChannel().setNextValue(phaseImbalanceCurrent);
	}

	/**
	 * Gets the Channel {@link ChannelId#PHASE_IMBALANCE}.
	 *
	 * @return the channel
	 */
	public default EnumReadChannel getPhaseImbalanceChannel() {
		return this.<EnumReadChannel>channel(ChannelId.PHASE_IMBALANCE);
	}

	/**
	 * Gets the value for {@link ChannelId#PHASE_IMBALANCE}.
	 *
	 * @return the channel {@link Value}
	 */
	public default PhaseImbalance getPhaseImbalance() {
		return this.getPhaseImbalanceChannel().value().asEnum();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#PHASE_IMBALANCE}.
	 *
	 * @param value the next value
	 */
	public default void _setPhaseImbalance(PhaseImbalance value) {
		this.getPhaseImbalanceChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel {@link ChannelId#SAFE_OPERATION_MODE}.
	 *
	 * @return the channel
	 */
	public default BooleanReadChannel getSafeOperationModeChannel() {
		return this.channel(ChannelId.SAFE_OPERATION_MODE);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#SAFE_OPERATION_MODE}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Boolean> getSafeOperationMode() {
		return this.getSafeOperationModeChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#SAFE_OPERATION_MODE}.
	 *
	 * @param setSwitchOn switch on/off charge park (true|false)
	 */
	public default void _setSafeOperationMode(Boolean setSwitchOn) {
		this.getSafeOperationModeChannel().setNextValue(setSwitchOn);
	}

	/**
	 * Gets the Channel {@link ChannelId#FREE_AVAILABLE_POWER}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getFreeAvailablePowerChannel() {
		return this.channel(ChannelId.FREE_AVAILABLE_POWER);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#FREE_AVAILABLE_POWER}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getFreeAvailablePower() {
		return this.getFreeAvailablePowerChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#FREE_AVAILABLE_POWER}.
	 *
	 * @param power the residual power in W
	 */
	public default void _setFreeAvailablePower(Integer power) {
		this.getFreeAvailablePowerChannel().setNextValue(power);
	}

	/**
	 * Gets the Channel {@link ChannelId#TRANSPORT_CAPACITY}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getTransportCapacityChannel() {
		return this.channel(ChannelId.TRANSPORT_CAPACITY);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#TRANSPORT_CAPACITY}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getTransportCapacity() {
		return this.getTransportCapacityChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#TRANSPORT_CAPACITY}.
	 *
	 * @param transportCapacity the transport capacity in W
	 */
	public default void _setTransportCapacity(Integer transportCapacity) {
		this.getTransportCapacityChannel().setNextValue(transportCapacity);
	}

	/**
	 * Gets the Channel {@link ChannelId#METER_CURRENT_L1}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getMeterCurrentL1Channel() {
		return this.channel(ChannelId.METER_CURRENT_L1);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#METER_CURRENT_L1}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getMeterCurrentL1() {
		return this.getMeterCurrentL1Channel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#METER_CURRENT_L1}.
	 *
	 * @param current the current in mA.
	 */
	public default void _setMeterCurrentL1(Integer current) {
		this.getMeterCurrentL1Channel().setNextValue(current);
	}

	/**
	 * Gets the Channel {@link ChannelId#METER_CURRENT_L2}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getMeterCurrentL2Channel() {
		return this.channel(ChannelId.METER_CURRENT_L2);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#METER_CURRENT_L2}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getMeterCurrentL2() {
		return this.getMeterCurrentL2Channel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#METER_CURRENT_L2}.
	 *
	 * @param current the current in mA.
	 */
	public default void _setMeterCurrentL2(Integer current) {
		this.getMeterCurrentL2Channel().setNextValue(current);
	}

	/**
	 * Gets the Channel {@link ChannelId#METER_CURRENT_L3}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getMeterCurrentL3Channel() {
		return this.channel(ChannelId.METER_CURRENT_L3);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#METER_CURRENT_L3}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getMeterCurrentL3() {
		return this.getMeterCurrentL3Channel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#METER_CURRENT_L3}.
	 *
	 * @param current the current in mA.
	 */
	public default void _setMeterCurrentL3(Integer current) {
		this.getMeterCurrentL3Channel().setNextValue(current);
	}

	/**
	 * Gets the Channel {@link ChannelId#FREE_AVAILABLE_CURRENT_L1}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getFreeAvailableCurrentL1Channel() {
		return this.channel(ChannelId.FREE_AVAILABLE_CURRENT_L1);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#FREE_AVAILABLE_CURRENT_L1}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getFreeAvailableCurrentL1() {
		return this.getFreeAvailableCurrentL1Channel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#FREE_AVAILABLE_CURRENT_L1}.
	 *
	 * @param minFreeCurrent the minimum free power in W of all limiters.
	 */
	public default void _setFreeAvailableCurrentL1(Integer minFreeCurrent) {
		this.getFreeAvailableCurrentL1Channel().setNextValue(minFreeCurrent);
	}

	/**
	 * Gets the Channel {@link ChannelId#FREE_AVAILABLE_CURRENT_L2}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getFreeAvailableCurrentL2Channel() {
		return this.channel(ChannelId.FREE_AVAILABLE_CURRENT_L2);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#FREE_AVAILABLE_CURRENT_L2}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getFreeAvailableCurrentL2() {
		return this.getFreeAvailableCurrentL2Channel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#FREE_AVAILABLE_CURRENT_L2}.
	 *
	 * @param minFreeCurrent the minimum free power in W of all limiters.
	 */
	public default void _setFreeAvailableCurrentL2(Integer minFreeCurrent) {
		this.getFreeAvailableCurrentL2Channel().setNextValue(minFreeCurrent);
	}

	/**
	 * Gets the Channel {@link ChannelId#FREE_AVAILABLE_CURRENT_L3}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getFreeAvailableCurrentL3Channel() {
		return this.channel(ChannelId.FREE_AVAILABLE_CURRENT_L3);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#FREE_AVAILABLE_CURRENT_L3}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getFreeAvailableCurrentL3() {
		return this.getFreeAvailableCurrentL3Channel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#FREE_AVAILABLE_CURRENT_L3}.
	 *
	 * @param minFreeCurrent the minimum free power in W of all limiters.
	 */
	public default void _setFreeAvailableCurrentL3(Integer minFreeCurrent) {
		this.getFreeAvailableCurrentL3Channel().setNextValue(minFreeCurrent);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#PHASE_LIMIT_EXCEEDED_L1}.
	 *
	 * @return the StateChannel
	 */
	public default StateChannel getPhaseLimitExceededL1Channel() {
		return this.channel(ChannelId.PHASE_LIMIT_EXCEEDED_L1);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PHASE_LIMIT_EXCEEDED_L1} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPhaseLimitExceededL1(Boolean value) {
		this.getPhaseLimitExceededL1Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_LIMIT_EXCEEDED_L2}.
	 *
	 * @return the StateChannel
	 */
	public default StateChannel getPhaseLimitExceededL2Channel() {
		return this.channel(ChannelId.PHASE_LIMIT_EXCEEDED_L2);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PHASE_LIMIT_EXCEEDED_L2} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPhaseLimitExceededL2(Boolean value) {
		this.getPhaseLimitExceededL2Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PHASE_LIMIT_EXCEEDED_L3}.
	 *
	 * @return the StateChannel
	 */
	public default StateChannel getPhaseLimitExceededL3Channel() {
		return this.channel(ChannelId.PHASE_LIMIT_EXCEEDED_L3);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#PHASE_LIMIT_EXCEEDED_L3} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPhaseLimitExceededL3(Boolean value) {
		this.getPhaseLimitExceededL3Channel().setNextValue(value);
	}

	/**
	 * Asks whether phase imbalance is considered for this limiter.
	 *
	 * @return true, if the limiter limits the phase imbalance
	 */
	public boolean isPhaseImbalanceLimiter();

	/**
	 * Gives back the Limiter ID.
	 *
	 * @return id of the current limiter.
	 */
	public Integer getLimiterId();
}
