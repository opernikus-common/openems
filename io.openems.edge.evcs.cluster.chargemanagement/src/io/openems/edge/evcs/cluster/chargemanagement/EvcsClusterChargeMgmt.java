package io.openems.edge.evcs.cluster.chargemanagement;

import java.util.Optional;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.evcs.cluster.chargemanagement.PhaseImbalance;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.MetaEvcs;

public interface EvcsClusterChargeMgmt extends OpenemsComponent, MetaEvcs, Evcs {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// State channels

		INFO_PHASE_IMBALANCE(Doc.of(Level.INFO) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Reduced performance due to phase imbalance.")), //
		WARN_NO_PHASE_IMBALANCE(Doc.of(Level.WARNING) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Missing (or duplicated) phase imbalance information.")), //
		EVCS_NO_MAX_POWER_WARN(Doc.of(Level.WARNING) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("At least one EVCS does not provide a MAX power.")), //
		EVCS_NO_MIN_POWER_WARN(Doc.of(Level.WARNING) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("At least one EVCS does not provide a MIN power.")), //
		SET_CHARGE_POWER_LIMIT_WARN(Doc.of(Level.WARNING) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("At least one EVCS is unable to accept SetChargePower limits.")), //
		CLUSTER_STATE(Doc.of(State.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Switches off evcsClusterChargeManagement when false (leads to state RED).
		 */
		SAFE_OPERATION_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Minimum free available power over all limiters.
		 *
		 * <p>
		 * When positive, there is freely available power for chargepoints. When
		 * negative the target power limit is exceeded.
		 *
		 */
		MIN_FREE_AVAILABLE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Minimum free available current on L1 over all limiters. The free available
		 * current between current situation and targetPower (in A).
		 *
		 * <p>
		 * positive values indicates a safe distance to the fuseLimit. Negative values
		 * indicate that we are above the target power (available per phase) and it may
		 * be close to the fuse limit.
		 */
		MIN_FREE_AVAILABLE_CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Minimum free current on L2 over all limiters. The free available current
		 * between current situation and targetPower (in A).
		 *
		 * <p>
		 * positive values indicates a safe distance to the fuseLimit. Negative values
		 * indicate that we are above the target power (available per phase) and it may
		 * be close to the fuse limit.
		 */
		MIN_FREE_AVAILABLE_CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Minimum free current on L3 over all limiters. The free available current
		 * between current situation and targetPower (in A).
		 *
		 * <p>
		 * positive values indicates a safe distance to the fuseLimit. Negative values
		 * indicate that we are above the target power (available per phase) and it may
		 * be close to the fuse limit.
		 */
		MIN_FREE_AVAILABLE_CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),
		/**
		 * Each cluster can be supplied by multiple supply cable segments. This
		 * indicates the max transport capacity of the weakest segment (in W).
		 */
		TRANSPORT_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * The id of the limiter currently steering the cluster.
		 */
		RESPONSIBLE_LIMITER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * The phase imbalance on the responsible limiter.
		 */
		PHASE_IMBALANCE(Doc.of(PhaseImbalance.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * The phase imbalance current on the responsible limiter.
		 */
		PHASE_IMBALANCE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //

		// ****************************************************************
		// other channels

		/**
		 * Number of currently available EVCS with Priority.LOW or
		 * Priority.EXCESS_POWER.
		 */
		NUMBER_OF_EVCS(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Number of currently available EVCS with Priority.HIGH.
		 */
		NUMBER_OF_EVCS_PRIO(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Number of prioritized evcs.")), //
		/**
		 * Number of currently charging EVCS with Priority.LOW or Priority.EXCESS_POWER.
		 */
		NUMBER_OF_CHARGING_EVCS(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * Number of currently charging EVCS with Priority.HIGH.
		 */
		NUMBER_OF_CHARGING_EVCS_PRIO(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH)), //
		EVCS_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		EVCS_POWER_LIMIT_PRIO(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		ROUND_ROBIN_ALLOWED_CHARGE_SESSIONS(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)), //
		ROUND_ROBIN_ACTIVITY(Doc.of(OpenemsType.BOOLEAN)//
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * control switch to allow or deny charging.
		 *
		 * <ul>
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		SET_ALLOW_CHARGING(new BooleanDoc().unit(Unit.ON_OFF) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
		// .onChannelSetNextWriteMirrorToDebugChannel(DEBUG_ALLOW_CHARGING) //
		), //

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
	 * Gets the Channel for {@link ChannelId#INFO_PHASE_IMBALANCE}.
	 *
	 * @return the StateChannel
	 */
	public default StateChannel getInfoPhaseImbalanceChannel() {
		return this.channel(ChannelId.INFO_PHASE_IMBALANCE);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#INFO_PHASE_IMBALANCE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setInfoPhaseImbalance(boolean value) {
		this.getInfoPhaseImbalanceChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel {@link ChannelId#ALLOW_CHARGING}.
	 *
	 * @return the channel
	 */
	public default BooleanWriteChannel getAllowChargingChannel() {
		return this.<BooleanWriteChannel>channel(ChannelId.SET_ALLOW_CHARGING);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#SET_ALLOW_CHARGING}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Boolean> getAllowCharging() {
		return this.getAllowChargingChannel().value();
	}

	/**
	 * Gets the write value for the channel {@link ChannelId#SET_ALLOW_CHARGING} and resets it.
	 *
	 * @return the write value
	 */
	public default Optional<Boolean> getAllowChargingValueAndReset() {
		return this.getAllowChargingChannel().getNextWriteValueAndReset();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#ALLOW_CHARGING}.
	 *
	 * @param value the next value
	 */
	public default void _setAllowCharging(Boolean value) {
		this.getAllowChargingChannel().setNextValue(value);
	}

	public default void setAllowCharging(Boolean value) throws OpenemsNamedException {
		this.getAllowChargingChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the channel for {@link ChannelId#CLUSTER_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<State> getClusterStateChannel() {
		return this.channel(ChannelId.CLUSTER_STATE);
	}

	/**
	 * Gets the cluster state. See {@link ChannelId#CLUSTER_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<State> getClusterState() {
		return this.getClusterStateChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#CLUSTER_STATE}.
	 *
	 * @param state the next state
	 */
	public default void _setClusterState(State state) {
		this.getClusterStateChannel().setNextValue(state);
	}

	/**
	 * Gets the Channel {@link ChannelId#NUMBER_OF_EVCS}.
	 *
	 * @return the channel
	 */
	public default Channel<Integer> getNumberOfEvcsChannel() {
		return this.channel(ChannelId.NUMBER_OF_EVCS);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#NUMBER_OF_EVCS}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getNumberOfEvcs() {
		return this.getNumberOfEvcsChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#NUMBER_OF_EVCS}.
	 *
	 * @param numberEvcs the number of evcss
	 */
	public default void _setNumberOfEvcs(Integer numberEvcs) {
		this.getNumberOfEvcsChannel().setNextValue(numberEvcs);
	}

	/**
	 * Gets the Channel {@link ChannelId#NUMBER_OF_EVCS_PRIO}.
	 *
	 * @return the channel
	 */
	public default Channel<Integer> getNumberOfEvcsPrioChannel() {
		return this.channel(ChannelId.NUMBER_OF_EVCS_PRIO);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#NUMBER_OF_EVCS_PRIO}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getNumberOfEvcsPrio() {
		return this.getNumberOfEvcsPrioChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#NUMBER_OF_EVCS_PRIO}.
	 *
	 * @param numberEvcsPrio the number of prioritized evcss
	 */
	public default void _setNumberOfEvcsPrio(Integer numberEvcsPrio) {
		this.getNumberOfEvcsPrioChannel().setNextValue(numberEvcsPrio);
	}

	/**
	 * Gets the Channel {@link ChannelId#NUMBER_OF_CHARGING_EVCS}.
	 *
	 * @return the channel
	 */
	public default Channel<Integer> getNumberOfChargingEvcsChannel() {
		return this.channel(ChannelId.NUMBER_OF_CHARGING_EVCS);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#NUMBER_OF_CHARGING_EVCS}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getNumberOfChargingEvcs() {
		return this.getNumberOfChargingEvcsChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#NUMBER_OF_CHARGING_EVCS}.
	 *
	 * @param value the number of charging evcs
	 */
	public default void _setNumberOfChargingEvcs(int value) {
		this.getNumberOfChargingEvcsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel {@link ChannelId#NUMBER_OF_CHARGING_EVCS_PRIO}.
	 *
	 * @return the channel
	 */
	public default Channel<Integer> getNumberOfChargingEvcsPrioChannel() {
		return this.channel(ChannelId.NUMBER_OF_CHARGING_EVCS_PRIO);
	}

	/**
	 * Gets the value for the channel
	 * {@link ChannelId#NUMBER_OF_CHARGING_EVCS_PRIO}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getNumberOfChargingEvcsPrio() {
		return this.getNumberOfChargingEvcsPrioChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#NUMBER_OF_CHARGING_EVCS_PRIO}.
	 *
	 * @param value the number of charging evcs
	 */
	public default void _setNumberOfChargingEvcsPrio(int value) {
		this.getNumberOfChargingEvcsPrioChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel {@link ChannelId#ROUND_ROBIN_ALLOWED_CHARGE_SESSIONS}.
	 *
	 * @return the channel
	 */
	public default Channel<Integer> getRoundRobinAllowedChargeSessionsChannel() {
		return this.channel(ChannelId.ROUND_ROBIN_ALLOWED_CHARGE_SESSIONS);
	}

	/**
	 * Gets the value for the channel
	 * {@link ChannelId#ROUND_ROBIN_ALLOWED_CHARGE_SESSIONS}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getRoundRobinAllowedChargeSessions() {
		return this.getRoundRobinAllowedChargeSessionsChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#ROUND_ROBIN_ALLOWED_CHARGE_SESSIONS}.
	 *
	 * @param value the number of charging evcs
	 */
	public default void _setRoundRobinAllowedChargeSessions(int value) {
		this.getRoundRobinAllowedChargeSessionsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel {@link ChannelId#ROUND_ROBIN_ACTIVITY}.
	 *
	 * @return the channel
	 */
	public default Channel<Boolean> getRoundRobinActivityChannel() {
		return this.channel(ChannelId.ROUND_ROBIN_ACTIVITY);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#ROUND_ROBIN_ACTIVITY}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Boolean> getRoundRobinActivity() {
		return this.getRoundRobinActivityChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#ROUND_ROBIN_ACTIVITY}.
	 *
	 * @param value round robin activity indicator
	 */
	public default void _setRoundRobinActivity(Boolean value) {
		this.getRoundRobinActivityChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel {@link ChannelId#EVCS_POWER_LIMIT_PRIO}.
	 *
	 * @return the channel
	 */
	public default Channel<Integer> getEvcsPowerLimitPrioChannel() {
		return this.channel(ChannelId.EVCS_POWER_LIMIT_PRIO);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#EVCS_POWER_LIMIT_PRIO}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getEvcsPowerLimitPrio() {
		return this.getEvcsPowerLimitPrioChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#EVCS_POWER_LIMIT_PRIO}.
	 *
	 * @param powerLimit the power limit for prioritized evcss
	 */
	public default void _setEvcsPowerLimitPrio(Integer powerLimit) {
		this.getEvcsPowerLimitPrioChannel().setNextValue(powerLimit);
	}

	/**
	 * Gets the Channel {@link ChannelId#EVCS_POWER_LIMIT}.
	 *
	 * @return the channel
	 */
	public default Channel<Integer> getEvcsPowerLimitChannel() {
		return this.channel(ChannelId.EVCS_POWER_LIMIT);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#EVCS_POWER_LIMIT}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getEvcsPowerLimit() {
		return this.getEvcsPowerLimitChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#EVCS_POWER_LIMIT}.
	 *
	 * @param powerLimit the power limit for unprioritized evcss
	 */
	public default void _setEvcsPowerLimit(Integer powerLimit) {
		this.getEvcsPowerLimitChannel().setNextValue(powerLimit);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EVCS_NO_MAX_POWER_WARN}.
	 *
	 * @return the StateChannel
	 */
	public default StateChannel getEvcsNoMaxPowerWarnChannel() {
		return this.channel(ChannelId.EVCS_NO_MAX_POWER_WARN);
	}

	/**
	 * Gets the EVCS no max power channel warning state of the EVCS charging
	 * station. See {@link ChannelId#EVCS_NO_MAX_POWER_WARN}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getEvcsNoMaxPowerWarn() {
		return this.getEvcsNoMaxPowerWarnChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EVCS_NO_MAX_POWER_WARN} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEvcsNoMaxPowerWarn(boolean value) {
		this.getEvcsNoMaxPowerWarnChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EVCS_NO_MIN_POWER_WARN}.
	 *
	 * @return the StateChannel
	 */
	public default StateChannel getEvcsNoMinPowerWarnChannel() {
		return this.channel(ChannelId.EVCS_NO_MIN_POWER_WARN);
	}

	/**
	 * Gets the EVCS no max power channel warning state of the EVCS charging
	 * station. See {@link ChannelId#EVCS_NO_MIN_POWER_WARN}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getEvcsNoMinPowerWarn() {
		return this.getEvcsNoMinPowerWarnChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#EVCS_NO_MIN_POWER_WARN} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEvcsNoMinPowerWarn(boolean value) {
		this.getEvcsNoMinPowerWarnChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CHARGE_POWER_LIMIT_WARN}.
	 *
	 * @return the StateChannel
	 */
	public default StateChannel getSetChargePowerLimitWarnChannel() {
		return this.channel(ChannelId.SET_CHARGE_POWER_LIMIT_WARN);
	}

	/**
	 * Gets the EVCS no max power channel warning state of the EVCS charging
	 * station. See {@link ChannelId#SET_CHARGE_POWER_LIMIT_WARN}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getSetChargePowerLimitWarn() {
		return this.getSetChargePowerLimitWarnChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SET_CHARGE_POWER_LIMIT_WARN} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSetChargePowerLimitWarn(boolean value) {
		this.getSetChargePowerLimitWarnChannel().setNextValue(value);
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
	public default Value<PhaseImbalance> getPhaseImbalance() {
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
	 * Gets the Channel {@link ChannelId#MIN_FREE_AVAILABLE_POWER}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getMinFreeAvailablePowerChannel() {
		return this.channel(ChannelId.MIN_FREE_AVAILABLE_POWER);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#MIN_FREE_AVAILABLE_POWER}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getMinFreePower() {
		return this.getMinFreeAvailablePowerChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#MIN_FREE_AVAILABLE_POWER}.
	 *
	 * @param power the minimum free power in W of all limiters.
	 */
	public default void _setMinFreeAvailablePower(Integer power) {
		this.getMinFreeAvailablePowerChannel().setNextValue(power);
	}

	/**
	 * Gets the Channel {@link ChannelId#MIN_FREE_AVAILABLE_CURRENT_L1}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getMinFreeAvailableCurrentL1Channel() {
		return this.channel(ChannelId.MIN_FREE_AVAILABLE_CURRENT_L1);
	}

	/**
	 * Gets the value for the channel
	 * {@link ChannelId#MIN_FREE_AVAILABLE_CURRENT_L1}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getMinFreeAvailableCurrentL1() {
		return this.getMinFreeAvailableCurrentL1Channel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#MIN_FREE_AVAILABLE_CURRENT_L1}.
	 *
	 * @param minFreeCurrent the minimum free power in W of all limiters.
	 */
	public default void _setMinFreeAvailableCurrentL1(Integer minFreeCurrent) {
		this.getMinFreeAvailableCurrentL1Channel().setNextValue(minFreeCurrent);
	}

	/**
	 * Gets the Channel {@link ChannelId#MIN_FREE_AVAILABLE_CURRENT_L2}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getMinFreeAvailableCurrentL2Channel() {
		return this.channel(ChannelId.MIN_FREE_AVAILABLE_CURRENT_L2);
	}

	/**
	 * Gets the value for the channel
	 * {@link ChannelId#MIN_FREE_AVAILABLE_CURRENT_L2}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getMinFreeAvailableCurrentL2() {
		return this.getMinFreeAvailableCurrentL2Channel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#MIN_FREE_AVAILABLE_CURRENT_L2}.
	 *
	 * @param minFreeCurrent the minimum free power in W of all limiters.
	 */
	public default void _setMinFreeAvailableCurrentL2(Integer minFreeCurrent) {
		this.getMinFreeAvailableCurrentL2Channel().setNextValue(minFreeCurrent);
	}

	/**
	 * Gets the Channel {@link ChannelId#MIN_FREE_AVAILABLE_CURRENT_L3}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getMinFreeAvailableCurrentL3Channel() {
		return this.channel(ChannelId.MIN_FREE_AVAILABLE_CURRENT_L3);
	}

	/**
	 * Gets the value for the channel
	 * {@link ChannelId#MIN_FREE_AVAILABLE_CURRENT_L3}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getMinFreeAvailableCurrentL3() {
		return this.getMinFreeAvailableCurrentL3Channel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#MIN_FREE_AVAILABLE_CURRENT_L3}.
	 *
	 * @param minFreeCurrent the minimum free power in W of all limiters.
	 */
	public default void _setMinFreeAvailableCurrentL3(Integer minFreeCurrent) {
		this.getMinFreeAvailableCurrentL3Channel().setNextValue(minFreeCurrent);
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
	 * @param transportCapacity the residual power in W
	 */
	public default void _setTransportCapacity(Integer transportCapacity) {
		this.getTransportCapacityChannel().setNextValue(transportCapacity);
	}

	/**
	 * Gets the Channel {@link ChannelId#RESPONSIBLE_LIMITER}.
	 *
	 * @return the channel
	 */
	public default IntegerReadChannel getResponsibleLimiterChannel() {
		return this.channel(ChannelId.RESPONSIBLE_LIMITER);
	}

	/**
	 * Gets the value for the channel {@link ChannelId#RESPONSIBLE_LIMITER}.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getResponsibleLimiter() {
		return this.getResponsibleLimiterChannel().value();
	}

	/**
	 * Internal method to set the next value on Channel
	 * {@link ChannelId#RESPONSIBLE_LIMITER}.
	 *
	 * @param limiterId the id (as a number) of the responsible limiter.
	 */
	public default void _setResponsibleLimiter(Integer limiterId) {
		this.getResponsibleLimiterChannel().setNextValue(limiterId);
	}

	/**
	 * Checks if component is activated or deactivated.
	 *
	 * @return true if component is activated.
	 */
	public boolean isActivated();

}
