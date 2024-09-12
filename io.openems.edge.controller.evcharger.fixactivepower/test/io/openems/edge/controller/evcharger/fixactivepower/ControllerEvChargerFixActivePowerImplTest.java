package io.openems.edge.controller.evcharger.fixactivepower;

import org.junit.Test;

import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evcharger.api.data.OperationMode;
import io.openems.edge.evcharger.managed.base.test.DummyManagedEvCharger;

public class ControllerEvChargerFixActivePowerImplTest {

	private static final String CTRL_ID = "ctrlEvChargerFix0";
	private static final String MANAGED_EVCS_ID = "charger0";
	private static final String MANAGED_EVCS_TARGET = "(&(enabled=true)(!(service.pid=ctrlEvChargerFix0))(|(id=charger0)))";
	private static final OperationMode OP_MODE = OperationMode.Automatic;

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEvChargerFixActivePowerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("evCharger", new DummyManagedEvCharger(MANAGED_EVCS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setGenManagedEvChargerId(MANAGED_EVCS_ID) //
						.setOperationMode(OP_MODE) //
						.setActivePowerPerPhase(0) //
						.setNumberOfPhases(3) //
						.setEnergySessionLimit(0) //
						.setGenManagedEvChargerTarget(MANAGED_EVCS_TARGET) //
						.build()); //
		;
	}
}
