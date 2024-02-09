package io.openems.edge.evcs.cluster.chargemanagement.helper;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.evcs.cluster.chargemanagement.EvcsClusterLimiter;

public class DummyEvcsClusterLimiter extends AbstractOpenemsComponent implements EvcsClusterLimiter {

	public DummyEvcsClusterLimiter(String id, int fuseLimit, int fuseSafetyOffset, int targetPower) {
		super(OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				EvcsClusterLimiter.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	@Override
	public void run() throws OpenemsNamedException {
		// nothing to do here
	}

	@Override
	public Integer getLimiterId() {
		return 1;
	}

	@Override
	public boolean isPhaseImbalanceLimiter() {
		return false;
	}

}
