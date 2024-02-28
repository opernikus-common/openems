package io.openems.edge.evcs.cluster.chargemanagement.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.evcs.api.Priority;
import io.openems.edge.evcs.cluster.chargemanagement.ClusterEvcs;
import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmt;
import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmtImpl;
import io.openems.edge.evcs.cluster.chargemanagement.State;
import io.openems.edge.evcs.cluster.chargemanagement.statemachine.Context;

public class TestUtils {

	public static AbstractComponentTest.TestCase setChargeConfigFullLoad() {
		return new AbstractComponentTest.TestCase().input(Consts.EVCS_ONE_CHARGE_POWER_LIMIT, Integer.MAX_VALUE)
				.input(Consts.EVCS_TWO_CHARGE_POWER_LIMIT, Integer.MAX_VALUE)
				.input(Consts.EVCS_THREE_CHARGE_POWER_LIMIT, Integer.MAX_VALUE)
				.input(Consts.EVCS_FOUR_CHARGE_POWER_LIMIT, Integer.MAX_VALUE);
	}

	/**
	 * Asserts that the default configuration is correctly shown on the channel
	 * side.
	 *
	 * @param test the test which state should be checked
	 */
	public static void assertDefaultConfigSetValid(ChargeManagementClusterTestComponent test) {
		assertEquals(sutChannelInt(test, EvcsClusterChargeMgmt.ChannelId.NUMBER_OF_EVCS), 4);
		assertEquals(sutChannelInt(test, EvcsClusterChargeMgmt.ChannelId.NUMBER_OF_EVCS_PRIO), 0);
	}

	/**
	 * Asserts that all output channels are appropriate when in state red.
	 *
	 * @param test the test which state should be checked
	 */
	public static void assertStateRedSafe(ChargeManagementClusterTestComponent test) {
		assertEquals(sutClusterState(test), State.RED);

		assertEquals(sutChannelInt(test, EvcsClusterChargeMgmt.ChannelId.EVCS_POWER_LIMIT), 0);
		assertEquals(sutChannelInt(test, EvcsClusterChargeMgmt.ChannelId.EVCS_POWER_LIMIT_PRIO), 0);
		assertEquals(sutChannelInt(test, EvcsClusterChargeMgmt.ChannelId.NUMBER_OF_CHARGING_EVCS), 0);
		assertEquals(sutChannelInt(test, EvcsClusterChargeMgmt.ChannelId.NUMBER_OF_CHARGING_EVCS_PRIO), 0);

	}

	/**
	 * Gets the SUT (system-under-test), the evcs fair share cluster component.
	 *
	 * @param test the test which state should be checked
	 * @return the evcsCluster object itself
	 */
	public static EvcsClusterChargeMgmtImpl sut(ChargeManagementClusterTestComponent test) {
		return (EvcsClusterChargeMgmtImpl) test.getSut();
	}

	/**
	 * Gets the evcs fair share cluster state.
	 *
	 * @param test the test which state should be checked
	 * @return the evcsCluster cluster state
	 */
	public static State sutClusterState(ChargeManagementClusterTestComponent test) {
		return sut(test).getClusterState().asEnum();
	}

	/**
	 * Gets the given state channels info level for the evcs fair share cluster.
	 *
	 * @param test      the test which state should be checked
	 * @param channelId the channel id to use
	 * @return the evcsCluster error state level
	 */
	public static Boolean sutChannelBool(ChargeManagementClusterTestComponent test, ChannelId channelId) {
		return (Boolean) sutChannel(test, channelId).get();
	}

	/**
	 * Gets the given channel value for the evcs fair share cluster.
	 *
	 * @param test      the test which state should be checked
	 * @param channelId the channel id to use
	 * @return the value the channel value
	 */
	public static Value<?> sutChannel(ChargeManagementClusterTestComponent test, ChannelId channelId) {
		return sut(test).channel(channelId).value();
	}

	/**
	 * Gets the given channel value for the evcs fair share cluster as an integer.
	 *
	 * @param test      the test which state should be checked
	 * @param channelId the channel id to use
	 * @return the value the channel value as integer
	 */
	public static int sutChannelInt(ChargeManagementClusterTestComponent test, ChannelId channelId) {
		return (int) sutChannel(test, channelId).get();
	}

	/**
	 * Easy function to check the cluster state.
	 *
	 * @param test  the test which state should be checked
	 * @param state the reference state
	 */
	public static void assertEqualClusterState(ChargeManagementClusterTestComponent test, State state) {
		assertEquals(sut(test).getClusterState().get(), state);
	}

	/**
	 * Checks if the ChargePower Channel of the Cluster is a specific Value.
	 *
	 * @param test  Controller Test
	 * @param power Expected Value
	 */
	public static void checkClusterPower(ChargeManagementClusterTestComponent test, int power) {
		assertEquals(getClusterPowerFromSut(test.getSut()), power);
	}

	/**
	 * Easy function to check the cluster state.
	 *
	 * @param sut the sut
	 * @return the power channel value as an integer
	 */
	public static int getClusterPowerFromSut(OpenemsComponent sut) {
		return (int) sut.channel(Consts.power.getChannelId()).value().get();
	}

	/**
	 * Checks if the ChargePower Channel of the Cluster is a specific Value.
	 *
	 * @param test  Controller Test
	 * @param power Expected Value
	 */
	public static void checkMeterPowerLessOrEquals(ChargeManagementClusterTestComponent test, int power) {
		assertTrue(getMeterPowerFromSut(test.getSut()) <= power);
	}

	/**
	 * Checks if the ChargePower Channel of the Cluster is a specific Value.
	 *
	 * @param test  Controller Test
	 * @param power Expected Value
	 */
	public static void checkMeterPowerEquals(ChargeManagementClusterTestComponent test, int power) {
		assertTrue(getMeterPowerFromSut(test.getSut()) == power);
	}

	/**
	 * Checks if the ChargePower Channel of the Cluster is a specific Value.
	 *
	 * @param test  Controller Test
	 * @param power Expected Value
	 */
	public static void checkMeterPowerGreaterOrEquals(ChargeManagementClusterTestComponent test, int power) {
		assertTrue(getMeterPowerFromSut(test.getSut()) >= power);
	}

	/**
	 * Gets the meter power channel value from the SUT.
	 *
	 * @param sut the sut
	 * @return the meter power value
	 */
	public static int getMeterPowerFromSut(OpenemsComponent sut) {
		return (int) sut.channel(Consts.power.getChannelId()).value().get();
	}

	/**
	 * Prepare the Test.
	 *
	 * @param c            the context
	 * @param oneEnabled   boolean value for evcs one.
	 * @param onePrio      boolean value for evcs one.
	 * @param twoEnabled   boolean value for evcs two.
	 * @param twoPrio      boolean value for evcs two.
	 * @param threeEnabled boolean value for evcs three.
	 * @param threePrio    boolean value for evcs three.
	 * @param fourEnabled  boolean value for evcs four.
	 * @param fourPrio     boolean value for evcs four.
	 * @throws Exception in case of any error.
	 */
	public static void prepareTestCondition(Context c, //
			boolean oneEnabled, boolean onePrio, //
			boolean twoEnabled, boolean twoPrio, //
			boolean threeEnabled, boolean threePrio, //
			boolean fourEnabled, boolean fourPrio) throws Exception {
		var evcss = c.getCluster().getAllEvcss();

		if (oneEnabled) {
			setEnergyLimitReached(c, evcss.get(0), false);
		} else {
			// this disables ChargeReq on evcs0
			setEnergyLimitReached(c, evcss.get(0), true);
		}
		if (twoEnabled) {
			setEnergyLimitReached(c, evcss.get(1), false);
		} else {
			// this disables ChargeReq on evcs1
			setEnergyLimitReached(c, evcss.get(1), true);
		}
		if (threeEnabled) {
			setEnergyLimitReached(c, evcss.get(2), false);
		} else {
			// this disables ChargeReq on evcs2
			setEnergyLimitReached(c, evcss.get(2), true);
		}
		if (fourEnabled) {
			setEnergyLimitReached(c, evcss.get(3), false);
		} else {
			// this disables ChargeReq on evcs3
			setEnergyLimitReached(c, evcss.get(3), true);
		}
		if (onePrio) {
			evcss.get(0).setPriority(Priority.HIGH);
		} else {
			evcss.get(0).setPriority(Priority.LOW);
		}
		if (twoPrio) {
			evcss.get(1).setPriority(Priority.HIGH);
		} else {
			evcss.get(1).setPriority(Priority.LOW);
		}
		if (threePrio) {
			evcss.get(2).setPriority(Priority.HIGH);
		} else {
			evcss.get(2).setPriority(Priority.LOW);
		}
		if (fourPrio) {
			evcss.get(3).setPriority(Priority.HIGH);
		} else {
			evcss.get(3).setPriority(Priority.LOW);
		}
	}

	public static void setEnergyLimitReached(Context c, ClusterEvcs evcs, boolean energyLimitReached) throws Exception {
		if (energyLimitReached) {
			evcs.setEnergyLimit(10000);
		} else {
			evcs.setEnergyLimit(0);
		}
		evcs._setEnergySession(10000);
	}
}
