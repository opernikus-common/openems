package io.openems.edge.evcharger.managed.base;

import org.osgi.service.metatype.annotations.AttributeDefinition;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evcharger.api.data.OperationMode;
import io.openems.edge.evcharger.api.data.PhaseRotation;
import io.openems.edge.evcharger.api.data.Priority;
import io.openems.edge.evcharger.managed.base.Config;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String evChargerId;
		private String evChargerTarget;
		private int activePowerPerPhase;
		private int energySessionLimit;
		private OperationMode operationMode;
		private Priority priority;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEvChargerId(String evChargerId) {
			this.evChargerId = evChargerId;
			return this;
		}

		public Builder setEvChargerTarget(String evChargerTarget) {
			this.evChargerTarget = evChargerTarget;
			return this;
		}

		public Builder setActivePowerPerPhase(int activePowerPerPhase) {
			this.activePowerPerPhase = activePowerPerPhase;
			return this;
		}

		public Builder setOperationMode(OperationMode operationMode) {
			this.operationMode = operationMode;
			return this;
		}

		public Builder setPriority(Priority priority) {
			this.priority = priority;
			return this;
		}

		public Builder setEnergySessionLimit(int energySessionLimit) {
			this.energySessionLimit = energySessionLimit;
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
	public String evcharger_id() {
		return this.builder.evChargerId;
	}

	@Override
	public OperationMode operationMode() {
		return this.builder.operationMode;
	}

	@Override
	public Priority priority() {
		return this.builder.priority;
	}

	@Override
	public int energySessionLimit() {
		return this.builder.energySessionLimit;
	}

	@Override
	public String evcharger_target() {
		return this.builder.evChargerTarget;
	}

}