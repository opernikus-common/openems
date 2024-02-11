package io.openems.edge.io.kmtronic.temperature.monitor.sensor;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface KmtronicTempSensor extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SENSOR_ID(Doc.of(OpenemsType.STRING)), //
		SENSOR_NAME(Doc.of(OpenemsType.STRING));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public default StringReadChannel getSensorIdChannel() {
		return this.channel(ChannelId.SENSOR_ID);
	}

	public default Value<String> getSensorId() {
		return this.getSensorIdChannel().value();
	}

	/**
	 * Set the sensor id by index.
	 * 
	 * @param id the id of the sensor.
	 */
	public default void _setSensorId(String id) {
		this.getSensorIdChannel().setNextValue(id);
	}

	public default StringReadChannel getSensorNameChannel() {
		return this.channel(ChannelId.SENSOR_NAME);
	}

	public default Value<String> getSensorName() {
		return this.getSensorNameChannel().value();
	}

	/**
	 * Set the sensorname .
	 * 
	 * @param name the name of the sensor.
	 */
	public default void _setSensorName(String name) {
		this.getSensorNameChannel().setNextValue(name);
	}

}
