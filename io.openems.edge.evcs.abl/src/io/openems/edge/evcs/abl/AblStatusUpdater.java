package io.openems.edge.evcs.abl;

import io.openems.edge.evcs.api.Status;

public class AblStatusUpdater {

	private final EvcsAblImpl parent;
	private long lastEnergy;

	protected AblStatusUpdater(EvcsAblImpl parent) {
		this.parent = parent;
	}

	protected void update() {
		this.setPhaseCount();
		this.setStatus();
		this.setEnergySession();
	}

	private void setEnergySession() {
		if (this.lastEnergy == 0 && this.parent.getStatus().getValue() == 3) {
			this.lastEnergy = this.parent.getActiveConsumptionEnergy().orElse(0L);
		}
		this.parent._setEnergySession((int) (this.parent.getActiveConsumptionEnergy().orElse(0L) - this.lastEnergy));
		if (this.parent.getStatus().getValue() != 3) {
			this.lastEnergy = 0;
		}
	}

	private void setStatus() {
		switch (this.parent.getChargePointState()) {
		case (160), (161) -> this.parent._setStatus(Status.NOT_READY_FOR_CHARGING);
		case (162), (178) -> this.parent._setStatus(Status.READY_FOR_CHARGING);
		case (194) -> this.parent._setStatus(Status.CHARGING);
		case (176) -> this.parent._setStatus(Status.CHARGING_REJECTED);
		case (179) -> this.parent._setStatus(Status.CHARGING_FINISHED);
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
		if (this.parent.getCurrentL1().orElse(0) >= 500) {
			phases += 1;
		}
		if (this.parent.getCurrentL2().orElse(0) >= 500) {
			phases += 1;
		}
		if (this.parent.getCurrentL3().orElse(0) >= 500) {
			phases += 1;
		}
		this.parent._setPhases(phases);
	}
}
