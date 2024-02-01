package io.openems.edge.evcs.compleo;

import io.openems.edge.evcs.api.Evcs;

public class CalculateEnergySession {

	private final Evcs parent;

	private boolean isConnectedOld = false;
	private Long energyAtStartOfSession;

	public CalculateEnergySession(Evcs parent) {
		this.parent = parent;
	}

	/**
	 * Updates the {@link Evcs.ChannelId#ENERGY_SESSION} from the
	 * {@link Evcs.ChannelId#ACTIVE_CONSUMPTION_ENERGY}.
	 * 
	 * @param isConnected true, if a car is connected
	 */
	public void update(boolean isConnected) {
		if (isConnected) {
			if (!this.isConnectedOld) {
				this.energyAtStartOfSession = this.parent.getActiveConsumptionEnergy().orElse(0L);
			}
			this.parent._setEnergySession(
					(int) (this.parent.getActiveConsumptionEnergy().orElse(0L) - this.energyAtStartOfSession));
		}
		this.isConnectedOld = isConnected;
	}

}
