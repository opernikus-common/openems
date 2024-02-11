package io.openems.edge.simulator.evcs;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Priority;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private int maxHwPower;
		private int minHwPower;
		private Priority priority;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMaxHwPower(int maxHwPower) {
			this.maxHwPower = maxHwPower;
			return this;
		}

		public Builder setMinHwPower(int minHwPower) {
			this.minHwPower = minHwPower;
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
	public int maxHwPower() {
		return this.builder.maxHwPower;
	}

	@Override
	public int minHwPower() {
		return this.builder.minHwPower;
	}

	@Override
	public PhaseRotation phaseRotation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String datasource_id() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Priority priority() {
		return this.builder.priority;
	}

}