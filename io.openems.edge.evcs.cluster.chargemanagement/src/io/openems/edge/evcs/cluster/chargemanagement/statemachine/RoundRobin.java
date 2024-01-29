package io.openems.edge.evcs.cluster.chargemanagement.statemachine;

import java.util.PriorityQueue;

import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.cluster.chargemanagement.ClusterEvcs;
import io.openems.edge.evcs.cluster.chargemanagement.utils.EvcsTools;
import io.openems.edge.evcs.cluster.chargemanagement.utils.RoundRobinComparator;

public class RoundRobin {

	private Context context;
	private PriorityQueue<RoundRobinEvcs> queue;
	private boolean unlimited;
	private RoundRobinComparator rrComparator;

	public RoundRobin(Context context, int cntChargingEvcs) {
		this.context = context;
		this.unlimited = false;
		this.rrComparator = new RoundRobinComparator(this.context.getCluster());
		this.queue = new PriorityQueue<RoundRobinEvcs>(this.rrComparator);
		this.setMaxAllowedChargeSessions(0);
		this.syncQueue();

		this.queue.stream().forEach(rre -> {
			if (EvcsTools.equalsStatus(rre.evcs(), Status.CHARGING)) {
				rre.setLocked(false);
			}
		});
	}

	public RoundRobin(Context context) {
		this(context, 0);
	}

	private boolean makeEffectivelyFinal(boolean var) {
		return var;
	}

	/**
	 * Update charge point behavior.
	 * 
	 * <p/>
	 * a) cyclically update all power limits on all EVCSs
	 * 
	 * <p/>
	 * b) switch off chargepoints to stop and start a timer.
	 * 
	 * <p/>
	 * c) switch on chargepoints to start when timer hits.
	 */
	protected void update() {
		if (this.context.getConfig().verboseDebug()) {
			this.context.getParent().logInfo(this.toString());
		}
		var cluster = this.context.getCluster();

		boolean step1 = this.queue.stream().anyMatch(rre -> rre.isLockRequestedTrue());
		boolean step2 = this.queue.stream().anyMatch(rre -> rre.isLockRequestedFalse());
		if (step1) {
			step2 = false;
			this.context.getRoundRobinSwitchTimer().reset();
		}
		if (step2) {
			if (!this.context.getRoundRobinSwitchTimer().check()) {
				step2 = false;
			}
		}
		var step2Final = this.makeEffectivelyFinal(step2);

		this.queue.forEach(rre -> {

			if (step1) {
				if (rre.isLockRequestedTrue()) {
					rre.setLocked(true);
					rre.setLockRequested(null);
				}
			}
			if (step2Final) {
				if (rre.isLockRequestedFalse()) {
					rre.setLocked(false);
					rre.setLockRequested(null);
				}
			}
			var power = 0;
			if (rre.isUnlocked()) {
				power = cluster.getEvcsMinPowerLimit();
				rre.updateUsedPhases();
			}
			cluster.limitPowerSingleEvcs(rre.evcs(), power, true);
		});
	}

	private void initNextRound() {
		var rrActivity = this.context.getParent().getRoundRobinActivity().orElse(false);
		this.context.getParent()._setRoundRobinActivity(!rrActivity);
		this.context.getRoundRobinTimer().reset();
		this.context.getLimitsExceededCheckTimer().reset();
		this.syncQueue();
		this.disableAll();
		this.resortQueue();
	}

	/**
	 * Apply a new round robin cycle. Stop existing charge processes. Start new
	 * charge processes.
	 */
	protected void nextRound() {
		final var cntPrev = this.countUnlocked();
		this.initNextRound();

		// enable a different set of chargepoints (and implicitly resort again)
		var q = new PriorityQueue<RoundRobinEvcs>(this.rrComparator);
		var chargeCnt = 0;
		while (!this.queue.isEmpty()) {
			var rrEvcs = this.queue.poll();
			var chargeReq = this.chargeRequest(rrEvcs);
			if (!chargeReq) {
				rrEvcs.resetUnlockCount();
			}
			// unlock first evcss in sorted queue
			if (chargeCnt < this.getMaxAllowedChargeSessions() && this.fitsBalance(rrEvcs)) {
				if (chargeReq) {
					chargeCnt++;
				}
				rrEvcs.setLockRequested(false);
			}
			q.offer(rrEvcs);
		}
		// replace queue
		this.queue = q;
		var cnt = this.countUnlockRequested();
		this.unlimited = (cnt == cntPrev && cnt >= this.context.getCluster().getAllEvcss().size());
	}

	/**
	 * Checks if all charge points are allowed to charge and run with MinPower.
	 * 
	 * @return true if all charge points work "normal"
	 */
	protected boolean isUnlimited() {
		return this.unlimited;
	}

	protected void setMaxAllowedChargeSessions(int maxAllowedChargeSessions) {
		this.context.getParent()._setRoundRobinAllowedChargeSessions(maxAllowedChargeSessions);
	}

	protected int getMaxAllowedChargeSessions() {
		// at least 1 charge point should run in round robin
		var channel = this.context.getParent().getRoundRobinAllowedChargeSessionsChannel();
		return channel.getNextValue().orElse(1);
	}

	/**
	 * Adopt max allowed charge sessions.
	 * 
	 * @param decrementOnly true if only decrement should be applied.
	 * @return true, if max allowed charge sessions is 0, false else.
	 */
	protected boolean adoptMaxAllowedChargeSessions(boolean decrementOnly) {
		var chargeSessionDiff = this.context.getNumberOfSimultaneousChargeSessionsToAddRemove();
		var cur = this.getMaxAllowedChargeSessions();
		if (chargeSessionDiff == 0) {
			if (this.context.getCableConstraints().isAboveTargetLimit()) {
				if (cur > 1) {
					this.setMaxAllowedChargeSessions(cur - 1);
					this.nextRound();
				}
				if (cur == 0) {
					return true;
				}
			}

		} else if (chargeSessionDiff < 0) {
			if (cur > -chargeSessionDiff) {
				this.setMaxAllowedChargeSessions(cur - (-chargeSessionDiff));
				this.nextRound();
			} else {
				this.setMaxAllowedChargeSessions(0);
				return true;
			}

		} else {
			if (!decrementOnly) {
				if (!this.context.getCableConstraints().isUnbalanced()) {
					this.setMaxAllowedChargeSessions(cur + chargeSessionDiff);
					this.nextRound();
				}
			}
		}
		return false;
	}

	private void resortQueue() {
		if (this.context.getConfig().verboseDebug()) {
			this.context.getParent().logInfo("Resort");
		}
		var q = new PriorityQueue<RoundRobinEvcs>(this.rrComparator);
		while (!this.queue.isEmpty()) {
			var rrEvcs = this.queue.poll();
			if (this.context.getConfig().verboseDebug()) {
				this.context.getParent().logInfo(rrEvcs.toString());
			}
			q.offer(rrEvcs);
		}
		this.queue = q;
	}

	private void disableAll() {
		this.queue.forEach(rrEvcs -> {
			rrEvcs.setLockRequested(true);
		});
		// this.update();
	}

	/**
	 * Initially creates the queue based on priority. Subequent calls will update
	 * the queue by removing elements or adding new ones at the end of the queue.
	 */
	private void syncQueue() {
		var cluster = this.context.getCluster();
		for (ClusterEvcs e : cluster.prioEvcss()) {
			this.queueAdd(e);
		}
		for (ClusterEvcs e : cluster.normalEvcss()) {
			this.queueAdd(e);
		}
	}

	private boolean queueContains(ClusterEvcs e) {
		return this.queue.stream().anyMatch(rre -> rre.evcs().equals(e));
	}

	private void queueAdd(ClusterEvcs e) {
		if (!this.queueContains(e)) {
			this.queue.offer(new RoundRobinEvcs(e));
		}
	}

	private boolean chargeRequest(RoundRobinEvcs rrEvcs) {
		return this.context.getCluster().connectedEvcss().contains(rrEvcs.evcs());
	}

	private int countUnlocked() {
		return (int) this.queue.stream().filter(rre -> !rre.isLocked()).count();
	}

	private int countUnlockRequested() {
		return (int) this.queue.stream().filter(rre -> rre.isLockRequestedFalse()).count();
	}

	/**
	 * A brief summary of this component.
	 * 
	 * @return String the summary as a string
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer("\n[roundRobinList [" + this.getMaxAllowedChargeSessions() + " maxSessions"
				+ (this.isUnlimited() ? ", unlimited" : "") + "]");

		this.queue.stream().forEach(rre -> {
			buf.append("\n                 [" + rre.evcs().id() //
					+ "  " + (rre.evcs().isPrioritized() ? "(prio)  " : "        ") //
					+ "  " + (!rre.isLocked() ? "(Unlocked) " : "           ") //
					+ "  " + (this.chargeRequest(rre) ? "(ChargeReq) " : "            ") //
					+ "  " + rre.evcs().getChargePower() //
					+ ", " + rre.evcs().getSetChargePowerLimitChannel().getNextWriteValue() //
					+ ",  " + rre.evcs().getStatus() //
					+ "]");
		});
		return buf.toString() + "]";
	}

	/**
	 * Checks the behaviour of this EVCS on imbalance state.
	 * 
	 * @param rrEvcs the evcs to check
	 * @return true if we have no phase unbalance or if this rrEvcs has no or a good
	 *         impact on phase imbalance, false else
	 */
	private boolean fitsBalance(RoundRobinEvcs rrEvcs) {
		if (!this.context.getCableConstraints().isUnbalanced()) {
			return true;
		}
		var phaseImbalance = this.context.getCableConstraints().getPhaseImbalance();
		if (rrEvcs.mayFixPhaseImbalance(phaseImbalance)) {
			return true;
		}
		return false;
	}

}
