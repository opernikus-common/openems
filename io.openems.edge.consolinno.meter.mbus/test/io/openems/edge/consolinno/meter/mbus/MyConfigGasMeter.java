package io.openems.edge.consolinno.meter.mbus;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfigGasMeter extends AbstractComponentConfig implements ConfigGasMeter {

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

		public MyConfigGasMeter build() {
			return new MyConfigGasMeter(this);
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

	private MyConfigGasMeter(Builder builder) {
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
	public GasMeterModel model() {
		return GasMeterModel.ITRON_CYBLE;
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
