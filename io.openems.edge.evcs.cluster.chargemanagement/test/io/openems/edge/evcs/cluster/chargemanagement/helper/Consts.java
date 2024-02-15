package io.openems.edge.evcs.cluster.chargemanagement.helper;

import io.openems.common.types.ChannelAddress;

public interface Consts {

	int safetyLimit = 25;
	int target = 10000;

	String clusterid = "dummycluster0";
	String meterId = "dummyMeter0";
	String evcsClusterLimiterOne = "evcsClusterLimiter0";
	String limiterId = "(&(enabled=true)(!(service.pid=dummylimiter0))(|(id=" + evcsClusterLimiterOne + ")))";
	String evcsOne = "evcs1";
	String evcsTwo = "evcs2";
	String evcsThree = "evcs3";
	String evcsFour = "evcs4";

	int FUSE_LIMIT = 70;
	int FUSE_SAFETY_OFFSET = 20;
	int TARGET_POWER = 10000;

	String[] evcsIds = new String[] { evcsOne, evcsTwo, evcsThree, evcsFour };
	ChannelAddress EVCS_ONE_CHARGE_POWER_LIMIT = new ChannelAddress(evcsOne, "SetChargePowerLimit");
	ChannelAddress EVCS_TWO_CHARGE_POWER_LIMIT = new ChannelAddress(evcsTwo, "SetChargePowerLimit");
	ChannelAddress EVCS_THREE_CHARGE_POWER_LIMIT = new ChannelAddress(evcsThree, "SetChargePowerLimit");
	ChannelAddress EVCS_FOUR_CHARGE_POWER_LIMIT = new ChannelAddress(evcsFour, "SetChargePowerLimit");
	ChannelAddress status = new ChannelAddress(clusterid, "ClusterState");

	ChannelAddress power = new ChannelAddress(clusterid, "ChargePower");

}
