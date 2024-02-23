package io.openems.edge.evcs.cluster.chargemanagement.statemachine;

import java.time.Duration;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.common.timer.Timer;
import io.openems.edge.common.timer.TimerManager;
import io.openems.edge.evcs.cluster.chargemanagement.Cluster;
import io.openems.edge.evcs.cluster.chargemanagement.Config;
import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmtImpl;
import io.openems.edge.evcs.cluster.chargemanagement.SupplyCableConstraints;

public class Context extends AbstractContext<EvcsClusterChargeMgmtImpl> {

	private Cluster cluster;
	private Config config;
	private SupplyCableConstraints cableConstraints;
	private Timer redHoldTimer;
	private Timer imbalanceHoldTimer;
	private Timer roundRobinWaitTimer;
	/**
	 * time in core cycles during starting a new round robin cycle. timer starts at
	 * end of last cycle. timer stops at begin of new cycle. this timer gives charge
	 * points time to stop charging before other chargepoints start charging
	 */
	private Timer roundRobinSwitchTimer;
	private Timer limitsExceededTimer;
	/**
	 * Check if limits are exceeded and act. Should probably hold 1/3 or 1/4 of time
	 * of limitsExceededTimer.
	 */
	private Timer limitsExceededCheckTimer;
	/** Timer used by TargetPowerZone to reinit cyclically. */
	private Timer targetPowerZoneTimer;
	private TimerManager tm;

	public Context(EvcsClusterChargeMgmtImpl parent, Cluster cluster, SupplyCableConstraints cableConstraints) {
		super(parent);
		this.cluster = cluster;
		this.cableConstraints = cableConstraints;
	}

	public void setTimerManager(TimerManager timerManager) {
		this.tm = timerManager;
	}

	/**
	 * Updates the configuration.
	 * 
	 * @param config the config.
	 */
	public void updateConfig(Config config) {
		this.config = config;
		this.redHoldTimer = this.tm.getTimerByTime(this.config.redHoldTime());
		this.imbalanceHoldTimer = this.tm.getTimerByTime(this.config.imbalanceHoldTime());
		this.roundRobinWaitTimer = this.tm.getTimerByTime(this.config.roundRobinTime());
		this.limitsExceededTimer = this.tm.getTimerByTime(this.config.limitsExceededTime());

		// hardcoded timings
		this.limitsExceededCheckTimer = this.tm.getTimerByTime(this.config.limitsExceededTime() / 3);
		this.targetPowerZoneTimer = this.tm.getTimerByTime((int) Duration.ofMinutes(1).getSeconds());
		this.roundRobinSwitchTimer = this.tm.getTimerByCoreCycles(4);
	}

	/**
	 * Gets the power step from the configured current step.
	 *
	 * @return the power step
	 */
	public int powerStep() {
		return this.config.currentStep() * 230 * 3; // 230V, 3 phases
	}

	public Cluster getCluster() {
		return this.cluster;
	}

	public Config getConfig() {
		return this.config;
	}

	public SupplyCableConstraints getCableConstraints() {
		return this.cableConstraints;
	}

	public Timer getRedHoldTimer() {
		return this.redHoldTimer;
	}

	public Timer getImbalanceHoldTimer() {
		return this.imbalanceHoldTimer;
	}

	public Timer getRoundRobinTimer() {
		return this.roundRobinWaitTimer;
	}

	public Timer getRoundRobinSwitchTimer() {
		return this.roundRobinSwitchTimer;
	}

	public Timer getLimitsExceededTimer() {
		return this.limitsExceededTimer;
	}

	public Timer getLimitsExceededCheckTimer() {
		return this.limitsExceededCheckTimer;
	}

	public Timer getTargetPowerZoneTimer() {
		return this.targetPowerZoneTimer;
	}

	/**
	 * Returns the number of chargesessions we can add (on positive value) or the
	 * number of chargesessions we need to remove (on negative value).
	 * 
	 * @return the number of chargesessions to add/remove.
	 */
	public int getNumberOfSimultaneousChargeSessionsToAddRemove() {
		return this.cableConstraints.getMinFreeAvailablePower() / this.getCluster().getEvcsMinPowerLimit();
	}

}
