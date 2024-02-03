package io.openems.edge.evcs.cluster.chargemanagement;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.evcs.cluster.chargemanagement.config.ConfigDefault;
import io.openems.edge.evcs.cluster.chargemanagement.config.MyConfig;
import io.openems.edge.evcs.cluster.chargemanagement.helper.Consts;
import io.openems.edge.evcs.cluster.chargemanagement.helper.FairShareClusterTestComponent;
import io.openems.edge.evcs.cluster.chargemanagement.helper.TestUtils;

public class GreenTest {

	private DummyComponentManager cpm;
	private MyConfig config;

	private FairShareClusterTestComponent baseTestController() throws Exception {
		this.config = ConfigDefault.createDefaultConfig().build();
		var test = new FairShareClusterTestComponent(new EvcsClusterChargeMgmtImpl());
		this.cpm = ConfigDefault.setBaseReferences(test);
		test.activate(this.config); //
		return test;
	}

	// @Test
	protected void testGreenState() throws Exception {
		// var test =
		this.baseTestController();
		// TestCase tc1 = TestUtils.setChargeConfigFullLoad();
		// test.runSeconds(1, tc1, this.cpm);
		// TestUtils.assertEqualClusterState(test, State.RED);
		// test.runSeconds(this.config.redHoldTime() + 1, tc1, this.cpm);
		// TestUtils.assertEqualClusterState(test, State.YELLOW);
		// test.runSeconds(60, tc1, this.cpm);
		// TestUtils.assertEqualClusterState(test, State.GREEN);
	}

	// @Test
	protected void testSafetyPower() throws Exception {
		var test = this.baseTestController();
		TestCase tc1 = TestUtils.setChargeConfigFullLoad();
		test.runSeconds(1, tc1, this.cpm);
		TestUtils.assertEqualClusterState(test, State.RED);
		test.runSeconds(this.config.redHoldTime() + 1, tc1, this.cpm);
		TestUtils.assertEqualClusterState(test, State.YELLOW);
		test.runSeconds(60, tc1, this.cpm);
		TestUtils.assertEqualClusterState(test, State.GREEN);
		TestUtils.checkMeterPowerLessOrEquals(test, Consts.safetyLimit);
		TestUtils.checkMeterPowerGreaterOrEquals(test, -1);
	}

}
