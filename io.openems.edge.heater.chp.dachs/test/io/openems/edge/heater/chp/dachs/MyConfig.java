package io.openems.edge.heater.chp.dachs;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String url;
		private String username;
		private String password;
		private int interval;
		private boolean readOnly;
		private boolean verbose;
		

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setUrl(String url) {
			this.url = url;
			return this;
		}
		
		public Builder setUsername(String username) {
			this.username = username;
			return this;
		}
		
		public Builder setPassword(String password) {
			this.password = password;
			return this;
		}

		public Builder setInterval(int interval) {
			this.interval = interval;
			return this;
		}

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public Builder setVerbose(boolean verbose) {
			this.verbose = verbose;
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
	public String url() {
		return this.builder.url;
	}

	@Override
	public String username() {
		return this.builder.username;
	}

	@Override
	public String password() {
		return this.builder.password;
	}

	@Override
	public int interval() {
		return this.builder.interval;
	}

	@Override
	public boolean readOnly() {
		return this.builder.readOnly;
	}

	@Override
	public boolean verbose() {
		return this.builder.verbose;
	}

}