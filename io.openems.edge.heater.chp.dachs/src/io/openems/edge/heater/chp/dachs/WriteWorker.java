package io.openems.edge.heater.chp.dachs;

import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.heater.api.OperationModeRequest;

public class WriteWorker extends AbstractCycleWorker {

	private final DachsGltImpl parent;
	private final DachsDevice dachs;

	public WriteWorker(DachsGltImpl parent, DachsDevice dachs) {
		this.parent = parent;
		this.dachs = dachs;
	}

	@Override
	protected void forever() throws Throwable {

		this.parent._setOperationModeRequest(this.parent.getOperationModeRequest());

		if (this.dachs.available()) {
			//
			
			/*
			 * This is the on-off switch. There are some things to watch out for: - This is
			 * not a hard command, especially the ’off’ command. The Dachs has a list of
			 * reasons to be running (see ’Dachs-Lauf-Anforderungen’), the ’external
			 * requirement’ (this on/off switch) being one of many. If any one of those
			 * reasons is true, it is running. Only if all of them are false, it will shut
			 * down. Bottom line, deactivateDachs() only does something if nothing else
			 * tells the Dachs to run. And activateDachs() might be ignored because of a
			 * limitation. - Timing: need to send ’on’ command at least every 10 minutes for
			 * the Dachs to keep running. ’interval’ is capped at 9 minutes, so this should
			 * be taken care of. - Also: You cannot switch a CHP on/off as you want. Number
			 * of starts should be minimized, and because of that there is a limit/hour on
			 * how often you can start. If the limit is reached, the chp won't start.
			 * Currently the code does not enforce any restrictions to not hit that limit!
			 */

			if (this.parent.getOperationModeRequestChannel().getNextWriteValue().orElse(OperationModeRequest.OFF).equals(OperationModeRequest.ON)) {
				this.dachs.activateDachs();
				return;
			}

			this.dachs.deactivateDachs();
		}
	}

}
