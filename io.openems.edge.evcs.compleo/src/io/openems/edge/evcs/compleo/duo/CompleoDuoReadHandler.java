package io.openems.edge.evcs.compleo.duo;

import io.openems.edge.evcs.api.Status;

public class CompleoDuoReadHandler {

	private final EvcsCompleoDuoImpl parent;

	protected CompleoDuoReadHandler(EvcsCompleoDuoImpl parent) {
		this.parent = parent;
	}

	protected void run() {
		this.setPhaseCount();
		this.setStatus();
	}

	private void setStatus() {
		//The Compleo DUO does not change the state if there is a car connected or not. To ensure functionality,
		//state 1 (0b00000001) state will be READY_FOR_CHARGING.
		switch (this.parent.getChargePointState().orElse(-1)) {
			case (1) -> this.parent._setStatus(Status.READY_FOR_CHARGING);
			case (2),(4) -> this.parent._setStatus(Status.CHARGING);
			case (16),(32) ->
					this.parent._setStatus(Status.UNDEFINED);
			default -> this.parent._setStatus(Status.ERROR);
		}
	}

	/**
	 * Writes the Amount of Phases in the Phase channel.
	 */
	private void setPhaseCount() {
		int phases = 0;
		/*
		 * The EVCS will pull power from the grid for its own consumption and report
		 * that on one of the phases. This value is different from EVCS to EVCS but can
		 * be high. Because of this, this will only register a Phase starting with 1A
		 * because then we definitively know that this load is caused by a car.
		 */
		if (this.parent.getCurrentL1().orElse(0) >= 1000) {
			phases += 1;
		}
		if (this.parent.getCurrentL2().orElse(0) >= 1000) {
			phases += 1;
		}
		if (this.parent.getCurrentL3().orElse(0) >= 1000) {
			phases += 1;
		}
		this.parent._setPhases(phases);
	}
}
