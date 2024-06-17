package io.openems.edge.consolinno.meter.mbus;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfigWaterMeter extends AbstractComponentConfig implements ConfigWaterMeter {

	protected static class Builder {
		private String id;
		private String mbusId;
		private int primaryAddress;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMbusId(String mbusId) {
			this.mbusId = mbusId;
			return this;
		}

		public Builder setPrimaryAddress(int primaryAddress) {
			this.primaryAddress = primaryAddress;
			return this;
		}

		public MyConfigWaterMeter build() {
			return new MyConfigWaterMeter(this);
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

	private MyConfigWaterMeter(Builder builder) {
		super(ConfigGasMeter.class, builder.id);
		this.builder = builder;
	}

	@Override
	public int primaryAddress() {
		return this.builder.primaryAddress;
	}

	@Override
	public int pollingIntervalSeconds() {
		return 0;
	}

	@Override
	public WaterMeterModelMbus model() {
		return WaterMeterModelMbus.ITRON_BM_M;
	}

	@Override
	public String wmbus_Id() {
		return this.builder.mbusId;
	}

	@Override
	public String Mbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.wmbus_Id());
	}
}
