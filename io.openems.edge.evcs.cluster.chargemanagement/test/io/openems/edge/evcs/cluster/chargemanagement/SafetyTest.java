package io.openems.edge.evcs.cluster.chargemanagement;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.evcs.cluster.chargemanagement.config.ConfigDefault;
import io.openems.edge.evcs.cluster.chargemanagement.config.MyConfig;
import io.openems.edge.evcs.cluster.chargemanagement.helper.ChargeManagementClusterTestComponent;
import io.openems.edge.evcs.cluster.chargemanagement.helper.TestUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SafetyTest {

	private DummyComponentManager cpm;
	private MyConfig config;

	private ChargeManagementClusterTestComponent baseTestController() throws Exception {
		this.config = ConfigDefault.createDefaultConfig().build();
		var test = new ChargeManagementClusterTestComponent(new EvcsClusterChargeMgmtImpl());
		this.cpm = ConfigDefault.setBaseReferences(test);
		test.activate(this.config); //
		return test;
	}

	private ChargeManagementClusterTestComponent invalidChargeTestController() throws Exception {
		this.config = ConfigDefault.createForbidConfig().build();
		var test = new ChargeManagementClusterTestComponent(new EvcsClusterChargeMgmtImpl());
		this.cpm = ConfigDefault.setBaseReferences(test);
		test.activate(this.config); //
		return test;
	}

	/*
	 * private FairShareClusterTestComponent meterBrokenTestController() throws
	 * Exception { this.config = ConfigBrokenMeter.createDefaultConfig().build();
	 * var test = new FairShareClusterTestComponent(new
	 * EvcsClusterChargeMgmtImpl()); this.cpm =
	 * ConfigBrokenMeter.setBaseReferences(test); test.activate(this.config); //
	 * return test; }
	 * 
	 * @Test public void test01MeterBroken() throws Exception { var test =
	 * this.meterBrokenTestController(); TestCase tc1 = new TestCase();
	 * 
	 * // check that it continuously stays in red for longer than redHoldTime
	 * test.runSeconds(this.config.redHoldTime() + 5, tc1, this.cpm, test, c -> {
	 * TestUtils.assertDefaultConfigSetValid(test);
	 * TestUtils.assertStateRedSafe(test); });
	 * 
	 * // finally check other conditions assertEquals(TestUtils.sutChannelInt(test,
	 * Evcs.ChannelId.CHARGE_POWER), 0); assertEquals(TestUtils.sutChannelBool(test,
	 * EvcsClusterChargeMgmt.ChannelId.METER_ERROR), true); }
	 */

	// TODO add test02NoMeter() - configuration without a meter

	// TODO add test03AllowChannel = false - starten in gruen sollte automatisch in
	// rot wechseln

	// @Test
	protected void test10RedHoldTime() throws Exception {
		var test = this.baseTestController();
		TestCase tc1 = TestUtils.setChargeConfigFullLoad();
		test.runSeconds(1, tc1, this.cpm);
		TestUtils.assertEqualClusterState(test, State.RED);
		test.runSeconds(1, tc1, this.cpm);
		TestUtils.assertEqualClusterState(test, State.RED);
		TestUtils.checkClusterPower(test, 0);
		test.runSeconds(this.config.redHoldTime() - 4, tc1, this.cpm);
		TestUtils.assertEqualClusterState(test, State.RED);
		TestUtils.checkClusterPower(test, 0);
		test.runSeconds(4, tc1, this.cpm);
		TestUtils.assertEqualClusterState(test, State.YELLOW);
	}

	// @Test
	protected void test11ForbidChargingConfig() throws Exception {
		var test = this.invalidChargeTestController();
		TestCase tc1 = TestUtils.setChargeConfigFullLoad();
		test.runSeconds(1, tc1, this.cpm);
		TestUtils.assertEqualClusterState(test, State.RED);
		test.runSeconds(this.config.redHoldTime() - 4, tc1, this.cpm);
		TestUtils.assertEqualClusterState(test, State.RED);
		TestUtils.checkClusterPower(test, 0);
		test.runSeconds(5, tc1, this.cpm);
		TestUtils.assertEqualClusterState(test, State.RED);
	}

}
