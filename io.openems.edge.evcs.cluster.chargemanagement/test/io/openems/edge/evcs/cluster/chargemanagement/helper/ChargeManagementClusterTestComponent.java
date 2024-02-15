package io.openems.edge.evcs.cluster.chargemanagement.helper;

import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.evcs.test.DummyManagedEvcs;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ChargeManagementClusterTestComponent
		extends AbstractComponentTest<ChargeManagementClusterTestComponent, OpenemsComponent> {
	public ChargeManagementClusterTestComponent(OpenemsComponent controller, OpenemsComponent... components)
			throws OpenemsException {
		super(controller);
	}

	@Override
	protected ChargeManagementClusterTestComponent self() {
		return this;
	}

	/**
	 * Runs the controller under test conditions for the given number of seconds.
	 * 
	 * @param seconds the number of seconds to run the controller
	 * @param tc      the testcase to run
	 * @param cpm     reference to the dummy component manager
	 * @throws Exception on any error.
	 */
	public void runSeconds(int seconds, TestCase tc, DummyComponentManager cpm) throws Exception {
		for (int i = 0; i < seconds; i++) {
			this.next(tc//
					.timeleap((TimeLeapClock) cpm.getClock(), 1, ChronoUnit.SECONDS));
			// Execute Run of the EVCS
			for (int n = 0; n < Consts.evcsIds.length; n++) {
				try {
					DummyManagedEvcs evcs = cpm.getComponent(Consts.evcsIds[n]);
					evcs.writeHandlerRun();
				} catch (Exception e) {
					continue;
				}
			}

			DummyElectricityMeter meter = cpm.getComponent(Consts.meterId);
			if (meter instanceof DummyClusterChargemanagementMeter me) {
				me.run(cpm);
			}
		}
	}

	/**
	 * Runs the controller under test conditions for the given number of seconds.
	 * 
	 * <p>
	 * Beginning each second it will apply the checks.
	 * 
	 * @param seconds the number of seconds to run the controller
	 * @param tc      the testcase to run
	 * @param cpm     reference to the dummy component manager
	 * @param test    the running test
	 * @param checks  the checks to apply
	 * @throws Exception on any error.
	 */
	public void runSeconds(int seconds, TestCase tc, DummyComponentManager cpm,
			ChargeManagementClusterTestComponent test, Consumer<ChargeManagementClusterTestComponent> checks)
			throws Exception {
		for (var cnt = 0; cnt < seconds; cnt++) {
			this.runSeconds(1, tc, cpm);
			checks.accept(test);
		}
	}

}
