package io.openems.edge.evcs.cluster.chargemanagement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.cluster.chargemanagement.utils.EvcsTools;

/**
 * Cluster holding all connected charge points.
 * 
 * <p>
 * Also responsible for Channels
 * <ul>
 * <li>NumberOfEvcs
 * <li>NumberOfEvcsPrio
 * <li>NumberOfChargingEvcs
 * <li>NumberOfChargingEvcsPrio
 * <li>EvcsPowerLimit
 * <li>EvcsPowerLimitPrio
 * <li>
 * </ul>
 */
public class Cluster {

	private final EvcsClusterChargeMgmtImpl parent;
	private final List<ClusterEvcs> clusterEvcss = new ArrayList<>();

	public Cluster(EvcsClusterChargeMgmtImpl parent) {
		this.parent = parent;
	}

	/**
	 * Clean up the cluster evcss.
	 */
	public void clean() {
		this.clusterEvcss.stream().forEach(clusterEvcs -> {
			clusterEvcs.setIsClustered(false);
		});
		this.clusterEvcss.clear();
		this.parent._setNumberOfEvcs(this.clusterEvcss.size());
	}

	/**
	 * Add a managed evcs to the cluster.
	 * 
	 * @param managedEvcs the managed evcs to add.
	 */
	public void add(ManagedEvcs managedEvcs) {
		ClusterEvcs clusterEvcs = new ClusterEvcs(this.parent.getDiagnostics(), managedEvcs);
		clusterEvcs.setIsClustered(true);
		this.clusterEvcss.add(clusterEvcs);
	}

	/**
	 * Remove a managed evcs from the cluster.
	 * 
	 * @param managedEvcs the managed evcs to add.
	 */
	public void remove(ManagedEvcs managedEvcs) {
		Iterator<ClusterEvcs> it = this.clusterEvcss.iterator();
		while (it.hasNext()) {
			ClusterEvcs ce = it.next();
			if (ce.has(managedEvcs)) {
				ce.setIsClustered(false);
				it.remove();
				break;
			}
		}
	}

	/**
	 * Gets the list of all Evcss.
	 * 
	 * @return the list
	 */
	public List<ClusterEvcs> getAllEvcss() {
		return this.clusterEvcss;
	}

	/**
	 * Gets the list of Evcss which are connected to a car.
	 * 
	 * @return the list
	 */
	public List<ClusterEvcs> connectedEvcss() {
		return this.clusterEvcss.stream().filter(e -> //
		e.getStatus().equals(Status.STARTING) //
				|| e.getStatus().equals(Status.READY_FOR_CHARGING) //
				|| e.getStatus().equals(Status.CHARGING_REJECTED) //
				|| e.getStatus().equals(Status.CHARGING) //
		).toList();
	}

	/**
	 * Gets the list of Evcss which are ready for charging or already charging.
	 * 
	 * @return the list
	 */
	public List<ClusterEvcs> wannaChargeEvcss() {
		return this.clusterEvcss.stream().filter(e -> //
		e.getStatus().equals(Status.READY_FOR_CHARGING) //
				|| e.getStatus().equals(Status.CHARGING_REJECTED) //
				|| e.getStatus().equals(Status.CHARGING) //
		).toList();
	}

	/**
	 * Gets the list of prio Evcss.
	 *
	 * @return the list
	 */
	public List<ClusterEvcs> prioEvcss() {
		return this.clusterEvcss.stream().filter(evcs -> evcs.isPrioritized()).toList();
	}

	/**
	 * Gets the list of unprio Evcss.
	 *
	 * @return the list
	 */
	public List<ClusterEvcs> normalEvcss() {
		return this.clusterEvcss.stream().filter(evcs -> !evcs.isPrioritized()).toList();
	}

	/**
	 * Max Power Limit of the highest charge point.
	 * 
	 * @return the max power limit in W.
	 */
	public int getEvcsMaxPowerLimit() {
		return EvcsTools.getEvcsMaxPowerLimit(this.clusterEvcss);
	}

	/**
	 * Min Power Limit of the lowest charge point.
	 * 
	 * @return the min power limit in W.
	 */
	public int getEvcsMinPowerLimit() {
		return EvcsTools.getEvcsMinPowerLimit(this.wannaChargeEvcss());
	}

	/**
	 * The min power to drive all plugged charge points with min power over all
	 * charge points.
	 * 
	 * @return sum in W.
	 */
	public int getClusterMinimumPower() {
		// int power = 0;
		return this.connectedEvcss().stream().mapToInt(ClusterEvcs::minPower).sum();
		// for (var e : this.connectedEvcss()) {
		// power += e.minPower();
		// }
		// return power;
	}

	/**
	 * The max power to drive all plugged charge points with min power over all
	 * charge points.
	 * 
	 * @return sum in W.
	 */
	public int getClusterMaximumPower() {
		return Math.min(this.connectedEvcss().stream().mapToInt(ClusterEvcs::maxPower).sum(),
				Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
	}

	/**
	 * limits distribution power for all charging stations.
	 *
	 * @param powerToDistribute the overall power that can be distributed amongst
	 *                          unprioritized charging stations
	 * @param setWithFilter     if the ramp filter shall be applied
	 * @throws OpenemsNamedException on write error
	 */
	public void limitPower(int powerToDistribute, boolean setWithFilter) throws OpenemsNamedException {
		for (ClusterEvcs evcs : this.clusterEvcss) {
			if (!evcs.isPrioritized()) {
				evcs.setChargePowerLimit(powerToDistribute, setWithFilter);
			}
		}
		this.parent._setEvcsPowerLimit(powerToDistribute);
	}

	/**
	 * limits distribution power for all prioritized charging stations.
	 *
	 * @param reqChargePower the overall power that can be distributed amongst
	 *                       prioritized charging stations
	 * @param setWithFilter  if the ramp filter shall be applied
	 * @throws OpenemsNamedException on write error
	 */
	public void limitPowerPrio(int reqChargePower, boolean setWithFilter) throws OpenemsNamedException {
		for (ClusterEvcs evcs : this.clusterEvcss) {
			if (evcs.isPrioritized()) {
				this.limitPowerSingleEvcs(evcs, reqChargePower, setWithFilter);
			}
		}
		this.parent._setEvcsPowerLimitPrio(reqChargePower);
	}

	/**
	 * limits distribution power for all charging stations.
	 *
	 * @param powerlimit the power that can be distributed amongst charging stations
	 * @throws OpenemsNamedException on write error
	 */
	public void limitPowerAll(int powerlimit) throws OpenemsNamedException {
		this.limitPower(powerlimit, true);
		this.limitPowerPrio(powerlimit, true);
	}

	/**
	 * limits charge power for a single charging stations.
	 *
	 * @param evcs           the evcs to control
	 * @param reqChargePower the overall power that can be distributed amongst
	 *                       prioritized charging stations
	 * @param setWithFilter  if the ramp filter shall be applied
	 * @throws OpenemsNamedException on write error
	 */
	public void limitPowerSingleEvcs(ClusterEvcs evcs, int reqChargePower, boolean setWithFilter) {
		evcs.setChargePowerLimit(reqChargePower, setWithFilter);
	}

	/**
	 * turns of all charging stations.
	 *
	 * @throws OpenemsNamedException on write error
	 */
	public void turnOffAllChargingEvcs() {
		this.connectedEvcss().forEach(evcs -> {
			evcs.setChargePowerLimit(0, false);
		});
	}

	/**
	 * Used to cyclically update channel values the cluster is responsible for.
	 */
	public void updateChannelValues() {
		this.updateEvcsCount();
		this.updateNumberOfChargingEvcs();
		this.parent.updateMinMaxChannels(this.getClusterMaximumPower());
	}

	private void updateEvcsCount() {
		this.parent._setNumberOfEvcsPrio(EvcsTools.countEvcs(this.getAllEvcss(), true));
		this.parent._setNumberOfEvcs(EvcsTools.countEvcs(this.getAllEvcss(), false));
	}

	private void updateNumberOfChargingEvcs() {
		int prioCharge = 0;
		int unprioCharge = 0;
		for (ClusterEvcs evcs : this.clusterEvcss) {
			if (EvcsTools.equalsStatus(evcs, Status.CHARGING)) {
				if (evcs.isPrioritized()) {
					prioCharge++;
				} else {
					unprioCharge++;
				}
			}
		}
		this.parent._setNumberOfChargingEvcs(unprioCharge);
		this.parent._setNumberOfChargingEvcsPrio(prioCharge);
	}

	/**
	 * Counts the number of all charging evcs (prio and unprio).
	 *
	 * @return the number of charging evcs.
	 */
	public int countAllChargingReadyEvcs() {
		int count = 0;
		for (ClusterEvcs evcs : this.clusterEvcss) {
			if (EvcsTools.equalsStatus(evcs, Status.CHARGING)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Asks if there are charging prioritized evcss.
	 *
	 * @return true, if there are charging prioritized evcss
	 */
	public boolean hasChargingPrioEvcs() {
		return EvcsTools.hasChargingEvcs(
				this.clusterEvcss.stream().filter(x -> x.isPrioritized()).collect(Collectors.toList()));
	}

	/**
	 * Asks if there are charging evcss.
	 *
	 * @return true, if there are charging evcss
	 */
	public boolean hasChargingEvcs() {
		return EvcsTools.hasChargingEvcs(this.clusterEvcss);
	}

	/**
	 * Reinitializes the limit for prioritized evcss.
	 *
	 * @throws OpenemsNamedException on write error
	 */
	public void reinitPrioLimits() throws OpenemsNamedException {
		var currentChargePowerMax = EvcsTools.getEvcsChargePowerMaxPrio(this.clusterEvcss);
		var prioPowerLimit = this.parent.getEvcsPowerLimitPrio().orElse(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);

		if (prioPowerLimit > currentChargePowerMax) {
			this.limitPowerPrio(currentChargePowerMax, false);
		}
	}

	/**
	 * Reinitializes the limit for unprioritized evcss.
	 *
	 * @throws OpenemsNamedException on write error
	 */
	public void reinitLimits() throws OpenemsNamedException {
		var currentChargePowerMax = EvcsTools.getEvcsChargePowerMax(this.clusterEvcss);
		var unprioPowerLimit = this.parent.getEvcsPowerLimit().orElse(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);

		if (unprioPowerLimit > currentChargePowerMax) {
			this.limitPower(currentChargePowerMax, false);
		}
	}

	/**
	 * Debug Information.
	 * 
	 * @return debug info.
	 */
	@Override
	public String toString() {
		return "( cnt " + EvcsTools.countEvcs(this.getAllEvcss(), false) + ", prio cnt " //
				+ EvcsTools.countEvcs(this.getAllEvcss(), true) + " EVCSs)";
	}

}
