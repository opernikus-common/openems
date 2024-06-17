package io.openems.edge.consolinno.meter.mbus.wmbus;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfigWaterMeterWMbus extends AbstractComponentConfig implements ConfigWaterMeterWirelessMbus {

	protected static class Builder {
		private String id;
		private String wmbusId;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setWmbusId(String wmbusId) {
			this.wmbusId = wmbusId;
			return this;
		}

		public MyConfigWaterMeterWMbus build() {
			return new MyConfigWaterMeterWMbus(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfigWaterMeterWMbus(Builder builder) {
		super(ConfigWaterMeterWirelessMbus.class, builder.id);
		this.builder = builder;
	}

	@Override
	public WaterMeterModelWirelessMbus model() {
		return WaterMeterModelWirelessMbus.ENGELMANN_WATERSTAR_M;
	}

	@Override
	public String key() {
		return "";
	}

	@Override
	public String Wmbus_Id() {
		return this.builder.wmbusId;
	}

	@Override
	public String radioAddress() {
		return "12345678";
	}

	@Override
	public String Wmbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.Wmbus_Id());
	}
}