package io.openems.edge.evcs.cluster.chargemanagement.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Priority;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.cluster.chargemanagement.ClusterEvcs;
import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmt;
import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmtImpl;

public class EvcsTools {

	private static final int DECREASE_POWER_STEP_OFFSET = 2;

	/**
	 * Asks if the given Evcs is prioritized.
	 *
	 * @param evcs the given Evcs
	 * @return true, if the given Evcs is prioritized
	 */
	public static boolean isPrioritized(ManagedEvcs evcs) {
		var v = evcs.channel(ManagedEvcs.ChannelId.PRIORITY).value();
		if (!v.isDefined()) {
			return false;
		}
		return ((Priority) v.asEnum()).isAtLeast(Priority.HIGH);
	}

	/**
	 * Calculates the number of prio evcss.
	 * 
	 * @param evcss       list of evcss
	 * @param prioritized true for prioritized evcss, false for unprioritized evcss.
	 * @return the number of prio evcss
	 */
	public static int countEvcs(List<ClusterEvcs> evcss, final boolean prioritized) {
		var count = 0;
		for (ClusterEvcs clusterEvcs : evcss) {
			if (clusterEvcs.isPrioritized() == prioritized) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Gets the maximum power limit for the list of {@link ManagedEvcs}s.
	 * 
	 * @param clusterEvcss the clusterEvcss
	 * @return the maximum power limit
	 */
	public static int getEvcsMaxPowerLimit(List<ClusterEvcs> clusterEvcss) {
		var maxEvcs = clusterEvcss.stream().max(Comparator.comparing(ClusterEvcs::maxPower)).orElse(null);
		if (maxEvcs != null) {
			return maxEvcs.maxPower();
		}
		return Evcs.DEFAULT_MAXIMUM_HARDWARE_POWER;
	}

	/**
	 * Gets the minimum power limit for the list of {@link ManagedEvcs}s.
	 * 
	 * @param clusterEvcss the clusterEvcss
	 * @return the minimum power limit
	 */
	public static int getEvcsMinPowerLimit(List<ClusterEvcs> clusterEvcss) {
		var minEvcs = clusterEvcss.stream().max(Comparator.comparing(ClusterEvcs::minPower)).orElse(null);
		if (minEvcs != null) {
			return minEvcs.minPower();
		}
		return Evcs.DEFAULT_MINIMUM_HARDWARE_POWER;
	}

	/**
	 * Asks if at least one of the given Evcss is charging.
	 * 
	 * @param clusteredEvcss the list of Evcss
	 * @return true, if at least one of them is charging
	 */
	public static boolean hasChargingEvcs(List<ClusterEvcs> clusteredEvcss) {
		if (clusteredEvcss == null) {
			return false;
		}
		for (ClusterEvcs evcs : clusteredEvcss) {
			if (equalsStatus(evcs, Status.CHARGING)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Asks if a given Evcs has a given status.
	 * 
	 * @param evcs   the {@link Evcs}
	 * @param status the {@link Status}
	 * @return true, if the evcs has the status
	 */
	public static boolean equalsStatus(ClusterEvcs evcs, Status status) {
		if (evcs.getStatus().getValue() == status.getValue()) {
			return true;
		}
		return false;
	}

	/**
	 * Decreases the distribution power for the prioritized Evcss.
	 * 
	 * <p>
	 * Note that we decrease one powerstep faster than increasing (and configured).
	 * 
	 * @param parent the {@link EvcsClusterChargeMgmt}
	 * @throws OpenemsNamedException on write error
	 */
	public static void decreaseDistributedPowerPrio(EvcsClusterChargeMgmtImpl parent) throws OpenemsNamedException {
		if (hasPowerLimitPrioReachedMinPower(parent)) {
			return;
		}
		var prioLimit = parent.getEvcsPowerLimitPrio().orElse(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
		if ((prioLimit - parent.getContext().powerStep()
				- DECREASE_POWER_STEP_OFFSET) < Evcs.DEFAULT_MINIMUM_HARDWARE_POWER) {
			return;
		}
		parent.getContext().getCluster()
				.limitPowerPrio(prioLimit - parent.getContext().powerStep() - DECREASE_POWER_STEP_OFFSET, true);
	}

	/**
	 * Increases the distribution power for the prioritized Evcss.
	 * 
	 * @param parent the {@link EvcsClusterChargeMgmt}
	 * @throws OpenemsNamedException on write error
	 */
	public static void increaseDistributedPowerPrio(EvcsClusterChargeMgmtImpl parent) throws OpenemsNamedException {
		if (hasPowerLimitPrioReachedMaxPower(parent)) {
			return;
		}
		var prioLimit = parent.getEvcsPowerLimitPrio().orElse(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
		parent.getContext().getCluster().limitPowerPrio(prioLimit + parent.getContext().powerStep(), true);
	}

	/**
	 * Decreases the distribution power for the unprioritized Evcss.
	 * 
	 * <p>
	 * Note that we decrease one powerstep faster than increasing (and configured).
	 * 
	 * @param parent the {@link EvcsClusterChargeMgmt}
	 * @throws OpenemsNamedException on write error
	 */
	public static void decreaseDistributedPower(EvcsClusterChargeMgmtImpl parent) throws OpenemsNamedException {
		if (hasPowerLimitReachedMinPower(parent)) {
			return;
		}
		var unprioLimit = parent.getEvcsPowerLimit().orElse(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
		if ((unprioLimit - parent.getContext().powerStep()
				- DECREASE_POWER_STEP_OFFSET) < Evcs.DEFAULT_MINIMUM_HARDWARE_POWER) {
			return;
		}
		parent.getContext().getCluster()
				.limitPower(unprioLimit - parent.getContext().powerStep() - DECREASE_POWER_STEP_OFFSET, true);
	}

	/**
	 * Increases the distribution power for the unprioritized Evcss.
	 * 
	 * @param parent the {@link EvcsClusterChargeMgmt}
	 * @throws OpenemsNamedException on write error
	 */
	public static void increaseDistributedPower(EvcsClusterChargeMgmtImpl parent) throws OpenemsNamedException {
		if (hasPowerLimitReachedMaxPower(parent)) {
			return;
		}
		var unprioLimit = parent.getEvcsPowerLimit().orElse(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
		parent.getContext().getCluster().limitPower(unprioLimit + parent.getContext().powerStep(), true);
	}

	/**
	 * Asks if the unprioritized limit has reached the maximum power.
	 * 
	 * @param parent the {@link EvcsClusterChargeMgmt}
	 * @return true, if the unprioritized limit has reached the maximum power
	 */
	public static boolean hasPowerLimitReachedMaxPower(EvcsClusterChargeMgmtImpl parent) {
		var unprioLimit = parent.getEvcsPowerLimit().orElse(0);
		var nextUnprioLimit = parent.getEvcsPowerLimitChannel().getNextValue().orElse(0);
		var maxEvcsLimit = parent.getContext().getCluster().getEvcsMaxPowerLimit();
		if (unprioLimit >= maxEvcsLimit || nextUnprioLimit >= maxEvcsLimit) {
			return true;
		}
		return false;
	}

	/**
	 * Check if unprioritized limit and next limit has reached minimum power.
	 * 
	 * @param parent the {@link EvcsClusterChargeMgmt}
	 * @return true, if the unprioritized limit has reached the minimum power
	 */
	public static boolean hasPowerLimitReachedMinPower(EvcsClusterChargeMgmtImpl parent) {
		var unprioLimit = parent.getEvcsPowerLimit().orElse(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
		var nextUnprioLimit = parent.getEvcsPowerLimitChannel().getNextValue()
				.orElse(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
		var minEvcsLimit = parent.getContext().getCluster().getEvcsMinPowerLimit();
		if (unprioLimit <= minEvcsLimit || nextUnprioLimit <= minEvcsLimit) {
			return true;
		}
		return false;
	}

	/**
	 * Asks if the prioritized limit has reached the maximum power.
	 * 
	 * @param parent the {@link EvcsClusterChargeMgmt}
	 * @return true, if the prioritized limit has reached the maximum power
	 */
	public static boolean hasPowerLimitPrioReachedMaxPower(EvcsClusterChargeMgmtImpl parent) {
		var prioLimit = parent.getEvcsPowerLimitPrio().orElse(0);
		var nextPrioLimit = parent.getEvcsPowerLimitPrioChannel().getNextValue().orElse(0);
		var maxEvcsLimit = parent.getContext().getCluster().getEvcsMaxPowerLimit();
		if (prioLimit >= maxEvcsLimit || nextPrioLimit >= maxEvcsLimit) {
			return true;
		}
		return false;
	}

	/**
	 * Asks if the prioritized limit has reached the minimum power.
	 * 
	 * @param parent the {@link EvcsClusterChargeMgmt}
	 * @return true, if the unprioritized limit has reached the minimum power
	 */
	public static boolean hasPowerLimitPrioReachedMinPower(EvcsClusterChargeMgmtImpl parent) {
		var prioLimit = parent.getEvcsPowerLimitPrio().orElse(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
		var nextPrioLimit = parent.getEvcsPowerLimitPrioChannel().getNextValue()
				.orElse(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
		var minEvcsLimit = parent.getContext().getCluster().getEvcsMinPowerLimit();
		if (prioLimit <= minEvcsLimit || nextPrioLimit <= minEvcsLimit) {
			return true;
		}
		return false;
	}

	/**
	 * Gets the maximum charge power for the prioritized Evcss.
	 * 
	 * @param clusteredEvcss the list of all Evcss
	 * @return the maximum charge power for the prioritized Evcss
	 */
	public static int getEvcsChargePowerMaxPrio(List<ClusterEvcs> clusteredEvcss) {
		return getChargeMaxPower(true, clusteredEvcss);
	}

	/**
	 * Gets the maximum charge power for the unprioritized Evcss.
	 * 
	 * @param clusteredEvcss the list of all Evcss
	 * @return the maximum charge power for the unprioritized Evcss
	 */
	public static int getEvcsChargePowerMax(List<ClusterEvcs> clusteredEvcss) {
		return getChargeMaxPower(false, clusteredEvcss);
	}

	/**
	 * Gets the maximum charge power for the whole cluster or the prioritized Evcss.
	 * 
	 * @param prioritized    whether the return value is for the whole cluster or
	 *                       the prioritized Evcss
	 * @param clusteredEvcss the list of {@link ManagedEvcs}s
	 * @return The maximum power for the given list
	 */
	private static int getChargeMaxPower(boolean prioritized, List<ClusterEvcs> clusteredEvcss) {
		var chargeMaxPower = getEvcsMinPowerLimit(clusteredEvcss);
		for (ClusterEvcs clusteredEvcs : clusteredEvcss) {
			if (clusteredEvcs.isPrioritized() == prioritized) {
				if (equalsStatus(clusteredEvcs, Status.CHARGING)) {
					int chargePower = clusteredEvcs.getChargePower();
					if (chargePower > chargeMaxPower) {
						chargeMaxPower = chargePower;
					}
				}
			}
		}
		return chargeMaxPower;
	}

	/**
	 * Checks if needle is part of any string within haystack.
	 * 
	 * @param hayStack the list of strings to search in.
	 * @param needle   the part of a string we need to find.
	 * @return true if found
	 */
	public static boolean contains(String[] hayStack, String needle) {
		return Arrays.stream(hayStack).anyMatch(x -> {
			if (x.indexOf(needle) >= 0) {
				return true;
			}
			return false;
		});
	}

}
