package io.openems.edge.controller.evcharger.fixactivepower;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evcharger.api.data.OperationMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String genManagedEvChargerId;
		private String genManagedEvChargerTarget;
		private int activePowerPerPhase;
		private int numberOfPhases;
		private int energySessionLimit;
		private OperationMode operationMode;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setGenManagedEvChargerId(String genManagedEvChargerId) {
			this.genManagedEvChargerId = genManagedEvChargerId;
			return this;
		}

		public Builder setGenManagedEvChargerTarget(String genManagedEvChargerTarget) {
			this.genManagedEvChargerTarget = genManagedEvChargerTarget;
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

		public Builder setNumberOfPhases(int numberOfPhases) {
			this.numberOfPhases = numberOfPhases;
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
	public String managedEvCharger_id() {
		return this.builder.genManagedEvChargerId;
	}

	@Override
	public OperationMode operationMode() {
		return this.builder.operationMode;
	}

	@Override
	public int activePowerPerPhase() {
		return this.builder.activePowerPerPhase;
	}

	@Override
	public int maxUsablePhases() {
		return this.builder.numberOfPhases;
	}

	@Override
	public int energySessionLimit() {
		return this.builder.energySessionLimit;
	}

	@Override
	public String managedEvCharger_target() {
		return this.builder.genManagedEvChargerTarget;
	}
}