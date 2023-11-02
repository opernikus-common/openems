package io.openems.edge.io.revpi.bsp.digitalio;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private ExpansionModule revpiType;
		private String[] in;
		private String[] out;
		private boolean updateOutputFromHardware;
		private boolean simulationMode;
		private String simulationDataIn;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setRevpiType(ExpansionModule type) {
			this.revpiType = type;
			return this;
		}

		public Builder setIn(String[] in) {
			this.in = in;
			return this;
		}

		public Builder setOut(String[] out) {
			this.out = out;
			return this;
		}

		public Builder setUpdateOutputFromHardware(boolean updateOutputFromHardware) {
			this.updateOutputFromHardware = updateOutputFromHardware;
			return this;
		}

		public Builder setSimulationMode(boolean simulationMode) {
			this.simulationMode = simulationMode;
			return this;
		}

		public Builder setSimulationDataIn(String simulationDataIn) {
			this.simulationDataIn = simulationDataIn;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
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

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public ExpansionModule revpiType() {
		return this.builder.revpiType;
	}

	@Override
	public String[] in() {
		return this.builder.in;
	}

	@Override
	public String[] out() {
		return this.builder.out;
	}

	@Override
	public boolean updateOutputFromHardware() {
		return this.builder.updateOutputFromHardware;
	}

	@Override
	public boolean simulationMode() {
		return this.builder.simulationMode;
	}

	@Override
	public String simulationDataIn() {
		return this.builder.simulationDataIn;
	}
}