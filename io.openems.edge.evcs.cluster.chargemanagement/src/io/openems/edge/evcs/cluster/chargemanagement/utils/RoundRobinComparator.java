package io.openems.edge.evcs.cluster.chargemanagement.utils;

import java.util.Comparator;

import io.openems.edge.evcs.cluster.chargemanagement.Cluster;
import io.openems.edge.evcs.cluster.chargemanagement.statemachine.RoundRobinEvcs;

/**
 * RoundRobinComparator. Added evcs will be sorted into the list with the
 * following sequence.
 *
 * <p>
 * 1. HighPrio, wants to charge
 *
 * <p>
 * 2. Normal Prio, wants to charge
 *
 * <p>
 * 3. High Prio, no charge
 *
 * <p>
 * 4. Normal prio, dont want to charge
 */
public class RoundRobinComparator implements Comparator<RoundRobinEvcs> {

	private final Cluster cluster;

	public RoundRobinComparator(Cluster cluster) {
		this.cluster = cluster;
	}

	private boolean chargeRequest(RoundRobinEvcs rrEvcs) {
		return this.cluster.connectedEvcss().contains(rrEvcs.evcs());
	}

	@Override
	public int compare(RoundRobinEvcs o1, RoundRobinEvcs o2) {
		// priority for cars to be charged

		// high prio
		var p1 = o1.evcs().isPrioritized();
		var p2 = o2.evcs().isPrioritized();
		var retCode = 0;
		if (p1 && !p2) {
			retCode += 0x10;
		}
		if (!p1 && p2) {
			retCode -= 0x10;
		}
		// charger requested
		var cr1 = RoundRobinComparator.this.chargeRequest(o1);
		var cr2 = RoundRobinComparator.this.chargeRequest(o2);
		if (cr1 && !cr2) {
			retCode += 0x08;
		}
		if (!cr1 && cr2) {
			retCode -= 0x08;
		}
		// // locked
		// var ena1 = o1.isLocked();
		// var ena2 = o2.isLocked();
		// if (!ena1 && ena2) {
		// retCode += 0x04;
		// }
		// if (ena1 && !ena2) {
		// retCode -= 0x04;
		// }
		var uc1 = o1.getUnlockCount();
		var uc2 = o2.getUnlockCount();
		if (uc1 < uc2) {
			retCode += 0x02;
		} else if (uc1 > uc2) {
			retCode -= 0x02;
		}
		var val = o1.evcs().id().compareTo(o2.evcs().id());
		if (val > 0) {
			retCode += 0x01;
		} else if (val < 0) {
			retCode -= 0x01;
		}
		return -1 * retCode;
	}
}
