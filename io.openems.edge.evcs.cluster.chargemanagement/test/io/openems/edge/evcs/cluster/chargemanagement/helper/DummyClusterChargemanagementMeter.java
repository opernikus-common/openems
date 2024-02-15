package io.openems.edge.evcs.cluster.chargemanagement.helper;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.evcs.test.DummyManagedEvcs;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class DummyClusterChargemanagementMeter extends DummyElectricityMeter {

	private int otherLoad = 0;

	public DummyClusterChargemanagementMeter(String id) {
		super(id);
	}

	public DummyClusterChargemanagementMeter(String id, int otherLoad) {
		super(id);
		this.otherLoad = otherLoad;
	}

	void run(DummyComponentManager cpm) {
		int powerSum = this.otherLoad;
		for (int n = 0; n < Consts.evcsIds.length; n++) {
			try {
				DummyManagedEvcs evcs = cpm.getComponent(Consts.evcsIds[n]);
				powerSum += evcs.getChargePower().orElse(0);
			} catch (Exception e) {
				continue;
			}
			this._setActivePower(powerSum);
			this._setActivePowerL1(powerSum / 3);
			this._setActivePowerL2(powerSum / 3);
			this._setActivePowerL3(powerSum / 3);
			this._setCurrent(powerSum / 230);
			this._setCurrentL1(powerSum / 230 / 3);
			this._setCurrentL2(powerSum / 230 / 3);
			this._setCurrentL3(powerSum / 230 / 3);

		}
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
	}

}
