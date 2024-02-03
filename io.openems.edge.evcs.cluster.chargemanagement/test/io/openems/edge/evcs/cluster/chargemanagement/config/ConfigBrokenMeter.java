package io.openems.edge.evcs.cluster.chargemanagement.config;

import java.time.Instant;
import java.time.ZoneOffset;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.filter.DisabledRampFilter;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.core.timer.DummyTimerManager;
import io.openems.edge.evcs.cluster.chargemanagement.helper.Consts;
import io.openems.edge.evcs.cluster.chargemanagement.helper.DummyEvcsClusterLimiter;
import io.openems.edge.evcs.cluster.chargemanagement.helper.FairShareClusterTestComponent;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evcs.test.DummyManagedEvcs;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ConfigBrokenMeter {

	private static final DummyEvcsPower EVCS_POWER = new DummyEvcsPower(new DisabledRampFilter());

	/**
	 * Creates a default cluster fairshare configuration.
	 * 
	 * @return the configuration.
	 */
	public static MyConfig.Builder createDefaultConfig() {
		// so war es .setTargetPower(10000)//
		// .setSafetyLimit(50)//
		// .setFuseLimit(70)//

		return MyConfig.create().setId(Consts.id)//
				.setEnabled(true)//
				.setAllowCharging(true)//
				.setAlias("default config")//
				.setCurrentStep(1)//
				.setStepInterval(1)//
				.setRedHoldTime(10)//
				.setRoundRobinTime(900)//
				.setResidualExcessHoldTime(30) //
				.setAllowPrioritization(true) //
				.setEvcsTarget("(&(enabled=true)(!(service.pid=test))(|(id=evcs1)(id=evcs2)(id=evcs3)(id=evcs4)))")//
				.setEvcsClusterLimiterTarget("(enabled=true)") //
				.setEvcsClusterLimiterIds(new String[] { //
						Consts.evcsClusterLimiterOne //
				}) //
				.setEvcsIds(new String[] { //
						Consts.evcsOne, //
						Consts.evcsTwo, //
						Consts.evcsThree, //
						Consts.evcsFour });
	}

	/**
	 * Creates an invalid cluster fairshare configuration.
	 * 
	 * @return the configuration.
	 */
	public static MyConfig.Builder createForbidConfig() {
		// .setTargetPower(10000)//
		// .setSafetyLimit(20)//
		// .setFuseLimit(70)//

		return MyConfig.create().setId(Consts.id)//
				.setEnabled(true)//
				.setAllowCharging(false)//
				.setAlias("invalid")//
				.setCurrentStep(1)//
				.setStepInterval(1)//
				.setRedHoldTime(10)//
				.setRoundRobinTime(900)//
				.setResidualExcessHoldTime(30) //
				.setAllowPrioritization(true) //
				.setEvcsTarget("(&(enabled=true)(!(service.pid=test))(|(id=evcs1)(id=evcs2)(id=evcs3)(id=evcs4)))")//
				.setEvcsClusterLimiterTarget("(enabled=true)") //
				.setEvcsClusterLimiterIds(new String[] { //
						Consts.evcsClusterLimiterOne //
				}) //
				.setEvcsIds(new String[] { //
						Consts.evcsOne, //
						Consts.evcsTwo, //
						Consts.evcsThree, //
						Consts.evcsFour });
	}

	/**
	 * Sets the test base references.
	 * 
	 * @param test the test
	 * @return the component manager.
	 */
	public static DummyComponentManager setBaseReferences(FairShareClusterTestComponent test) throws Exception {
		DummyComponentManager cpm = new DummyComponentManager(
				new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC));
		var cm = new DummyConfigurationAdmin();
		test.addReference("power", new DummyEvcsPower());
		test.addReference("timerManager", new DummyTimerManager(cpm));
		test.addReference("configAdmin", cm);
		test.addReference("componentManager", cpm);
		test.addReference("meter", new DummyElectricityMeter(Consts.meterId));
		test.addReference("addEvcs", new DummyManagedEvcs(Consts.evcsOne, EVCS_POWER, true));
		test.addReference("addEvcs", new DummyManagedEvcs(Consts.evcsTwo, EVCS_POWER, true));
		test.addReference("addEvcs", new DummyManagedEvcs(Consts.evcsThree, EVCS_POWER, true));
		test.addReference("addEvcs", new DummyManagedEvcs(Consts.evcsFour, EVCS_POWER, true));
		test.addReference("addEvcsClusterLimiter", new DummyEvcsClusterLimiter(Consts.evcsClusterLimiterOne,
				Consts.FUSE_LIMIT, Consts.FUSE_SAFETY_OFFSET, Consts.TARGET_POWER));
		return cpm;
	}
}
