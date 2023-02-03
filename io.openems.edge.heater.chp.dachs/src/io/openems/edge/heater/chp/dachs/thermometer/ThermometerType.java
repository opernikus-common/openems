package io.openems.edge.heater.chp.dachs.thermometer;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.chp.dachs.DachsGlt;

public enum ThermometerType {
	FLOW_TEMPERATURE(Heater.ChannelId.FLOW_TEMPERATURE), //
	RETURN_TEMPERATURE(Heater.ChannelId.RETURN_TEMPERATURE), //
	HOT_WATER(DachsGlt.ChannelId.HOT_WATER_TEMPERATURE), //
	OUTDOOR(DachsGlt.ChannelId.OUTDOOR_TEMPERATURE), //
	SENSOR_1(DachsGlt.ChannelId.TEMPERATURE_SENSOR_1), //
	SENSOR_2(DachsGlt.ChannelId.TEMPERATURE_SENSOR_2) //
	;

	ThermometerType(ChannelId copyChannel) {
		this.copyChannel = copyChannel;
	}

	public ChannelId copyChannel;

}
