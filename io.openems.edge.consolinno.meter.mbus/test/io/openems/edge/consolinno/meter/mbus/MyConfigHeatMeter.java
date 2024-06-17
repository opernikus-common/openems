package io.openems.edge.consolinno.meter.mbus;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfigHeatMeter extends AbstractComponentConfig implements ConfigHeatMeter {

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

		public MyConfigHeatMeter build() {
			return new MyConfigHeatMeter(this);
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

	private MyConfigHeatMeter(Builder builder) {
		super(ConfigHeatMeter.class, builder.id);
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
	public HeatMeterModel model() {
		return HeatMeterModel.ITRON_CF_51;
	}

	@Override
	public String Mbus_Id() {
		return this.builder.mbusId;
	}

	@Override
	public String Mbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.Mbus_Id());
	}
}
