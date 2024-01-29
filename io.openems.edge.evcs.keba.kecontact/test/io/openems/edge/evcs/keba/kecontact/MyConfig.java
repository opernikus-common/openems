package io.openems.edge.evcs.keba.kecontact;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evcs.api.Priority;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private int minHwCurrent;
		private Priority priority;
		private String ip;
		private boolean debugMode;

		private boolean dipSwitchInfo;
		private boolean useDisplay;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMinHwCurrent(int minHwCurrent) {
			this.minHwCurrent = minHwCurrent;
			return this;
		}

		public Builder setPriority(Priority priority) {
			this.priority = priority;
			return this;
		}

		public Builder setIp(String ip) {
			this.ip = ip;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder setDipSwitchInfo(boolean dipSwitchInfo) {
			this.dipSwitchInfo = dipSwitchInfo;
			return this;
		}

		public Builder setUseDisplay(boolean useDisplay) {
			this.useDisplay = useDisplay;
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
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public boolean dipSwitchInfo() {
		return this.builder.dipSwitchInfo;
	}

	@Override
	public Priority priority() {
		return this.builder.priority;
	}

	@Override
	public String ip() {
		return this.builder.ip;
	}

	@Override
	public int minHwCurrent() {
		return this.builder.minHwCurrent;
	}

	@Override
	public boolean useDisplay() {
		return this.builder.useDisplay;
	}
}