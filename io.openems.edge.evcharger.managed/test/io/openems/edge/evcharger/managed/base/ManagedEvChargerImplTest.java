package io.openems.edge.evcharger.managed.base;

import org.junit.Test;

import io.openems.common.function.ThrowingRunnable;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evcharger.api.data.OperationMode;
import io.openems.edge.evcharger.api.data.Priority;
import io.openems.edge.evcharger.api.test.DummyManagedEvCharger;

public class ManagedEvChargerImplTest {

	private static final String CTRL_ID = "evChargerMgt0";
	private static final String EVCS_ID = "evCharger0";
	private static final String EVCS_TARGET = "(&(enabled=true)(!(service.pid=evChargerMgt0))(|(id=evCharger0)))";
	private static final OperationMode OP_MODE = OperationMode.Automatic;

	@Test
	public void test() throws Exception {
		// var compMan = new DummyComponentManager(new
		// TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC));

		var evCharger = new DummyManagedEvCharger(EVCS_ID);
		var tc = new TestCase();
		tc.onBeforeProcessImage(new ThrowingRunnable<Exception>() {
			@Override
			public void run() throws Exception {
				evCharger.handleEvent(null);
			}

		});

		new ComponentTest(new ManagedEvChargerImpl()) // ^
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evCharger", evCharger) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEvChargerId(EVCS_ID) //
						.setOperationMode(OP_MODE) //
						.setActivePowerPerPhase(0) //
						.setPriority(Priority.HIGH) //
						.setEnergySessionLimit(0) //
						.setEvChargerTarget(EVCS_TARGET) //
						.build()) //
				.next(tc) //
				.next(new TestCase());
		;
	}
}
