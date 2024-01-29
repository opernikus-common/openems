package io.openems.edge.evcs.cluster.chargemanagement;

import static io.openems.edge.evcs.api.Evcs.DEFAULT_MINIMUM_HARDWARE_CURRENT;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Priority;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.cluster.chargemanagement.utils.Diagnostics;
import io.openems.edge.evcs.cluster.chargemanagement.utils.EvcsTools;

/**
 * This is a wrapper for EVCSs which should be clusterable by opernikus cluster
 * controller.
 * 
 * <p>
 * channels Accessed:
 * 
 * <p>
 * ManagedEvcs.ChannelId.PRIORITY, ManagedEvcs.ChannelId.IS_CLUSTERED,
 * ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT,
 * ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT_WITH_FILTER
 *
 * <p>
 * Evcs.ChannelId.MAXIMUM_POWER, Evcs.ChannelId.MINIMUM_POWER,
 * Evcs.ChannelId.STATUS, Evcs.ChannelId.CHARGE_POWER,
 * Evcs.ChannelId.CURRENT_L1, Evcs.ChannelId.CURRENT_L2,
 * Evcs.ChannelId.CURRENT_L3, Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY
 * 
 */
public class ClusterEvcs {

	private Diagnostics diagnostics;
	private ManagedEvcs evcs;

	public ClusterEvcs(Diagnostics diagnostics, ManagedEvcs evcs) {
		super();
		this.evcs = evcs;
		this.diagnostics = diagnostics;
	}

	public void setIsClustered(boolean isClustered) {
		this.evcs._setIsClustered(isClustered);
	}

	public Status getStatus() {
		return this.evcs.getStatus();
	}

	/**
	 * Checks if this ClusterEvcs Container holds the given evcs.
	 * 
	 * @param evcs the evcs to check.
	 * @return (this.evcs == evcs)
	 */
	public boolean has(ManagedEvcs evcs) {
		return (this.evcs == evcs);
	}

	public boolean isPrioritized() {
		return EvcsTools.isPrioritized(this.evcs);
	}

	/**
	 * Get the max power.
	 * 
	 * @return the max power.
	 */
	public Integer maxPower() {
		// TODO wir muessen hier gelegentlich aufraeumen
		// var val = this.evcs.getMaximumPower();
		var val = this.evcs.getMaximumHardwarePower();
		this.diagnostics.raiseMaxWarning(this.evcs.id(), !val.isDefined());
		if (val.isDefined()) {
			return val.get();
		}
		return Evcs.DEFAULT_MINIMUM_HARDWARE_POWER;
	}

	/**
	 * Get the min power.
	 * 
	 * @return the min power.
	 */
	public Integer minPower() {
		// TODO wir muessen hier gelegentlich aufraeumen
		// var val = this.evcs.getMinimumPower();
		var val = this.evcs.getMinimumHardwarePower();
		this.diagnostics.raiseMinWarning(this.evcs.id(), !val.isDefined());
		if (val.isDefined()) {
			return val.get();
		}
		return Evcs.DEFAULT_MINIMUM_HARDWARE_POWER;
	}

	public Integer getChargePower() {
		return this.evcs.getChargePower().orElse(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
	}

	public Integer getCurrentL1() {
		return this.evcs.getCurrentL1().orElse(DEFAULT_MINIMUM_HARDWARE_CURRENT);
	}

	public Integer getCurrentL2() {
		return this.evcs.getCurrentL2().orElse(DEFAULT_MINIMUM_HARDWARE_CURRENT);
	}

	public Integer getCurrentL3() {
		return this.evcs.getCurrentL3().orElse(DEFAULT_MINIMUM_HARDWARE_CURRENT);
	}

	/**
	 * Return the id of the evcs.
	 * 
	 * @return the id
	 */
	public String id() {
		return this.evcs.id();
	}

	public void setChargePowerLimit(int powerToDistribute, boolean setWithFilter) {
		try {
			if (setWithFilter) {
				this.evcs.setChargePowerLimitWithFilter(powerToDistribute);
			} else {
				this.evcs.setChargePowerLimit(powerToDistribute);
			}
			this.diagnostics.raiseSetChargePowerLimit(this.evcs.id(), false);

		} catch (Exception e) {
			this.diagnostics.raiseSetChargePowerLimit(this.evcs.id(), true);
		}
	}

	public IntegerWriteChannel getSetChargePowerLimitChannel() {
		return this.evcs.getSetChargePowerLimitChannel();
	}

	public Channel<Integer> getChargePowerChannel() {
		return this.evcs.getChargePowerChannel();
	}

	public Channel<Integer> getCurrentL1Channel() {
		return this.evcs.getCurrentL1Channel();
	}

	public Channel<Integer> getCurrentL2Channel() {
		return this.evcs.getCurrentL2Channel();
	}

	public Channel<Integer> getCurrentL3Channel() {
		return this.evcs.getCurrentL3Channel();
	}

	public Channel<Long> getActiveConsumptionEnergyChannel() {
		return this.evcs.getActiveConsumptionEnergyChannel();
	}

	public void setPriority(Priority prio) {
		this.evcs._setPriority(prio);
	}

	/**
	 * Method mainly for junit tests.
	 * 
	 * @param energyLimit to set
	 */
	public void setEnergyLimit(Integer energyLimit) throws OpenemsNamedException {
		this.evcs.setEnergyLimit(energyLimit);
	}

	/**
	 * Method mainly for junit tests.
	 * 
	 * @param energySession to set
	 */
	public void _setEnergySession(Integer energySession) {
		this.evcs._setEnergySession(energySession);
	}

	// for junit tests
	public ManagedEvcs getEvcs() {
		return this.evcs;
	}

}
