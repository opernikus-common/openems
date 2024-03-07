package io.openems.edge.evcs.cluster.chargemanagement.statemachine;

import static org.junit.Assert.assertEquals;

import java.util.Dictionary;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.osgi.service.event.Event;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmtImpl;
import io.openems.edge.evcs.cluster.chargemanagement.config.ConfigDefault;
import io.openems.edge.evcs.cluster.chargemanagement.config.MyConfig;
import io.openems.edge.evcs.cluster.chargemanagement.helper.ChargeManagementClusterTestComponent;
import io.openems.edge.evcs.cluster.chargemanagement.helper.TestUtils;
import io.openems.edge.evcs.test.DummyManagedEvcs;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RoundRobinBaseTest {

	private static final int MINPOWER = Evcs.DEFAULT_MINIMUM_HARDWARE_POWER;
	private static final int ___NO___ = 0;
	// private DummyComponentManager cpm;
	private MyConfig config;
	private RoundRobin roundRobin;

	/*
	 * checks if
	 *
	 * <ul> <<br/>
	 *
	 * <li>max charge sessions is applied correctly
	 *
	 * <li>priorisation and chargeRequests are applied correctly
	 *
	 * <li>the unlimited flag is handled approprietaly
	 *
	 * </ul>
	 */
	//@Test
	protected void roundRobin01MaxChargeSessionsTest() throws Exception {
		var test = this.baseTestController();
		// TestCase tc1 = new TestCase();

		// init
		var c = ((EvcsClusterChargeMgmtImpl) test.getSut()).getContext();
		c.getParent().logInfo("roundRobinMaxChargeSessionsTest --------------------");
		TestUtils.prepareTestCondition(c, //
				true, false, //
				false, false, //
				true, true, //
				false, true //
		);
		this.applyCycle(test, c);

		this.roundRobin = new RoundRobin(c);
		this.setMaxChargeSessions(test, 1);
		this.roundRobinUpdate(test, c);
		assertEquals(this.roundRobin.isUnlimited(), false);

		// check two cycles, expect same result
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, ___NO___ }, { 2, MINPOWER }, { 3, ___NO___ } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, ___NO___ }, { 2, MINPOWER }, { 3, ___NO___ } });

		// increase charge sessions
		this.setMaxChargeSessions(test, 2);

		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, MINPOWER }, { 1, ___NO___ }, { 2, MINPOWER }, { 3, ___NO___ } });

		assertEquals(this.roundRobin.isUnlimited(), false);

		// increase charge sessions, but only two could charge
		this.setMaxChargeSessions(test, 3);
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, MINPOWER }, { 1, ___NO___ }, { 2, MINPOWER }, { 3, ___NO___ } });

		// check limit state
		assertEquals(this.roundRobin.isUnlimited(), false);
		this.roundRobinNextRound(test, c);
		this.roundRobinUpdate(test, c);
		assertEquals(this.roundRobin.isUnlimited(), true);
	}

	/*
	 * check if
	 *
	 * -prio charge requests are handled appropriately -normal charge requests are
	 * handled when there are no prio charge requests
	 */
	// @Test
	protected void roundRobin20PrioChargeSessionRotateTest() throws Exception {
		var test = this.baseTestController();
		// TestCase tc1 = new TestCase();

		// init
		var c = ((EvcsClusterChargeMgmtImpl) test.getSut()).getContext();
		c.getParent().logInfo("roundRobinPrioChargeSessionRotateTest --------------------");
		TestUtils.prepareTestCondition(c, //
				true, false, //
				false, false, //
				true, true, //
				false, true //
		);
		this.applyCycle(test, c);

		this.roundRobin = new RoundRobin(c);
		this.setMaxChargeSessions(test, 1);
		this.roundRobinUpdate(test, c);
		assertEquals(this.roundRobin.isUnlimited(), false);

		// check two cycles, expect same result
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, ___NO___ }, { 2, MINPOWER }, { 3, ___NO___ } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, ___NO___ }, { 2, MINPOWER }, { 3, ___NO___ } });

		// finish session on evcs3, allow session evcs4
		var evcss = c.getCluster().getAllEvcss();
		TestUtils.setEnergyLimitReached(c, evcss.get(2), true);
		TestUtils.setEnergyLimitReached(c, evcss.get(3), false);
		this.applyCycle(test, c);
		this.roundRobinUpdate(test, c);
		this.assertChargePower(c,
				new Integer[][] { { 0, ___NO___ }, { 1, ___NO___ }, { 2, ___NO___ }, { 3, ___NO___ } });

		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, ___NO___ }, { 2, ___NO___ }, { 3, MINPOWER } });
		assertEquals(this.roundRobin.isUnlimited(), false);

		// finish session on evcs4
		TestUtils.setEnergyLimitReached(c, evcss.get(3), true);
		this.applyCycle(test, c);
		this.roundRobinUpdate(test, c);
		this.assertChargePower(c,
				new Integer[][] { { 0, ___NO___ }, { 1, ___NO___ }, { 2, ___NO___ }, { 3, ___NO___ } });

		// next cycle, evcs1 should charge
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, MINPOWER }, { 1, ___NO___ }, { 2, ___NO___ }, { 3, ___NO___ } });
		assertEquals(this.roundRobin.isUnlimited(), false);

	}

	/*
	 * Check if 2 out of 4 are rotated as expected. All 4 are normal (unprioritized)
	 * charges.
	 */
	// @Test
	protected void roundRobin30ChargeSessionRotate2of4Test() throws Exception {
		var test = this.baseTestController();
		// TestCase tc1 = new TestCase();

		/*
		 * init:
		 *
		 * 4 charge points want to charge
		 *
		 * 2 are allowed to charge
		 */
		var c = ((EvcsClusterChargeMgmtImpl) test.getSut()).getContext();
		c.getParent().logInfo("roundRobinChargeSessionRotate2of4Test --------------------");
		TestUtils.prepareTestCondition(c, //
				true, false, //
				true, false, //
				true, false, //
				true, false //
		);
		this.applyCycle(test, c);

		this.roundRobin = new RoundRobin(c);
		this.setMaxChargeSessions(test, 2);
		this.roundRobinUpdate(test, c);
		assertEquals(this.roundRobin.isUnlimited(), false);

		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, MINPOWER }, { 1, MINPOWER }, { 2, ___NO___ }, { 3, ___NO___ } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, ___NO___ }, { 2, MINPOWER }, { 3, MINPOWER } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, MINPOWER }, { 1, MINPOWER }, { 2, ___NO___ }, { 3, ___NO___ } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, ___NO___ }, { 2, MINPOWER }, { 3, MINPOWER } });

		assertEquals(this.roundRobin.isUnlimited(), false);

	}

	/*
	 * Check if 1 out of 4 are rotated as expected. All 4 are normal (unprioritized)
	 * charges.
	 */
	// @Test
	protected void roundRobin40ChargeSessionRotate1of4Test() throws Exception {
		var test = this.baseTestController();
		// TestCase tc1 = new TestCase();

		/*
		 * init:
		 *
		 * 4 charge points want to charge
		 *
		 * 2 are allowed to charge
		 */
		var c = ((EvcsClusterChargeMgmtImpl) test.getSut()).getContext();
		c.getParent().logInfo("roundRobinChargeSessionRotate1of4Test --------------------");
		TestUtils.prepareTestCondition(c, //
				true, false, //
				true, false, //
				true, false, //
				true, false //
		);
		this.applyCycle(test, c);

		this.roundRobin = new RoundRobin(c);
		this.setMaxChargeSessions(test, 1);
		this.roundRobinUpdate(test, c);
		assertEquals(this.roundRobin.isUnlimited(), false);

		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, MINPOWER }, { 1, ___NO___ }, { 2, ___NO___ }, { 3, ___NO___ } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, MINPOWER }, { 2, ___NO___ }, { 3, ___NO___ } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, ___NO___ }, { 2, MINPOWER }, { 3, ___NO___ } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, ___NO___ }, { 2, ___NO___ }, { 3, MINPOWER } });

		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, MINPOWER }, { 1, ___NO___ }, { 2, ___NO___ }, { 3, ___NO___ } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, MINPOWER }, { 2, ___NO___ }, { 3, ___NO___ } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, ___NO___ }, { 2, MINPOWER }, { 3, ___NO___ } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, ___NO___ }, { 2, ___NO___ }, { 3, MINPOWER } });

		assertEquals(this.roundRobin.isUnlimited(), false);
	}

	/*
	 * Check if 2 out of 4 are rotated as expected. All 4 are normal (unprioritized)
	 * charges.
	 */
	// @Test
	protected void roundRobin50ChargeSessionRotate3of4Test() throws Exception {
		var test = this.baseTestController();
		/*
		 * init:
		 *
		 * 4 charge points want to charge
		 *
		 * 2 are allowed to charge
		 */
		var c = ((EvcsClusterChargeMgmtImpl) test.getSut()).getContext();
		c.getParent().logInfo("roundRobinChargeSessionRotate3of4Test --------------------");
		TestUtils.prepareTestCondition(c, //
				true, false, //
				true, false, //
				true, false, //
				true, false //
		);
		this.applyCycle(test, c);

		this.roundRobin = new RoundRobin(c);
		this.setMaxChargeSessions(test, 3);
		this.roundRobinUpdate(test, c);
		assertEquals(this.roundRobin.isUnlimited(), false);

		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, MINPOWER }, { 1, MINPOWER }, { 2, MINPOWER }, { 3, ___NO___ } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, MINPOWER }, { 1, MINPOWER }, { 2, ___NO___ }, { 3, MINPOWER } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, MINPOWER }, { 1, ___NO___ }, { 2, MINPOWER }, { 3, MINPOWER } });
		this.applyCycleCheck(test, c,
				new Integer[][] { { 0, ___NO___ }, { 1, MINPOWER }, { 2, MINPOWER }, { 3, MINPOWER } });

		assertEquals(this.roundRobin.isUnlimited(), false);

	}

	private void applyCycleCheck(ChargeManagementClusterTestComponent test, Context c, Integer[][] expectedValues)
			throws Exception {
		// apply new cycle
		this.roundRobinNextRound(test, c);
		this.roundRobinUpdate(test, c);
		this.assertChargePower(c, expectedValues);

	}

	private void setMaxChargeSessions(ChargeManagementClusterTestComponent test, int number) {
		this.roundRobin.setMaxAllowedChargeSessions(number);
		for (Channel<?> channel : test.getSut().channels()) {
			channel.nextProcessImage();
		}
	}

	private void roundRobinNextRound(ChargeManagementClusterTestComponent test, Context c) throws Exception {
		c.getParent().logInfo("Next round");
		this.roundRobin.nextRound();
		this.applyCycle(test, c);
		this.assertAllChargePower(c, 0);
	}

	private void roundRobinUpdate(ChargeManagementClusterTestComponent test, Context c) throws Exception {
		c.getParent().logInfo("Update");
		this.roundRobin.update();
		this.applyCycle(test, c);
	}

	private void assertAllChargePower(Context c, int expectedPower) {
		c.getParent().logInfo(this.roundRobin.toString());
		c.getCluster().getAllEvcss().stream().forEach(evcs -> {
			assertEquals(evcs.getChargePower(), Integer.valueOf(expectedPower));
		});
	}

	private void assertChargePower(Context c, Integer[][] values) {
		c.getParent().logInfo(this.roundRobin.toString());
		var evcss = c.getCluster().getAllEvcss();
		for (Integer[] value : values) {
			var evcs = evcss.get(value[0]);
			var power = value[1];
			assertEquals(evcs.getChargePower(), power);
		}
	}

	private void applyCycle(ChargeManagementClusterTestComponent test, Context c) throws Exception {
		for (Channel<?> channel : test.getSut().channels()) {
			channel.nextProcessImage();
		}
		c.getCluster().getAllEvcss().stream().forEach(clusterEvcs -> {
			var e = (DummyManagedEvcs) clusterEvcs.getEvcs();
			e.handleEvent(new Event(EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, (Dictionary<String, ?>) null));
			e.writeHandlerRun();
		});
	}

	private ChargeManagementClusterTestComponent baseTestController() throws Exception {
		this.config = ConfigDefault.createDefaultConfig().build();
		var test = new ChargeManagementClusterTestComponent(new EvcsClusterChargeMgmtImpl());
		// this.cpm =
		ConfigDefault.setBaseReferences(test);
		test.activate(this.config); //
		return test;
	}
}
