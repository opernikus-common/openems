package io.openems.edge.simulator.evcharger;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcharger.api.EvCharger;
import io.openems.edge.evcharger.api.ManageableEvCharger;
import io.openems.edge.meter.api.ElectricityMeter;

public interface SimulatorEvCharger extends ManageableEvCharger, EvCharger, ElectricityMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

}
