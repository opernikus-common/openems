package io.openems.edge.evcs.cluster.chargemanagement.config;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evcs.cluster.chargemanagement.Config;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {

		private int roundRobinTime;
		private String evcsTarget;
		private int redHoldTime;
		private int imbalanceHoldTime;
		private int stepInterval;
		private int currentStep;
		private String[] evcsIds;
		private boolean allowCharging;
		private String id;
		private String alias;
		private boolean enabled = true;
		private boolean allowPrioritization = true;
		private String[] evcsClusterLimiterIds;
		private int residualExcessHoldTime;
		private String evcsClusterLimiterTarget;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder setEvcsTarget(String evcsTarget) {
			this.evcsTarget = evcsTarget;
			return this;
		}

		public Builder setRedHoldTime(int redHoldTime) {
			this.redHoldTime = redHoldTime;
			return this;
		}

		public Builder setStepInterval(int stepInterval) {
			this.stepInterval = stepInterval;
			return this;
		}

		public Builder setCurrentStep(int currentStep) {
			this.currentStep = currentStep;
			return this;
		}

		public Builder setEvcsIds(String[] evcsIds) {
			this.evcsIds = evcsIds;
			return this;
		}

		public Builder setEvcsClusterLimiterIds(String[] clusterLimiterIds) {
			this.evcsClusterLimiterIds = clusterLimiterIds;
			return this;
		}

		public Builder setAllowCharging(boolean allowCharging) {
			this.allowCharging = allowCharging;
			return this;
		}

		public Builder setAllowPrioritization(boolean allowPrio) {
			this.allowPrioritization = allowPrio;
			return this;
		}

		public Builder setAlias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder setRoundRobinTime(int roundRobinTime) {
			this.roundRobinTime = roundRobinTime;
			return this;
		}

		public Builder setUnbalancedHoldTime(int unbalancedHoldTime) {
			this.imbalanceHoldTime = unbalancedHoldTime;
			return this;
		}

		public Builder setResidualExcessHoldTime(int residualExcessHoldTime) {
			this.residualExcessHoldTime = residualExcessHoldTime;
			return this;
		}

		public Builder setEvcsClusterLimiterTarget(String target) {
			this.evcsClusterLimiterTarget = target;
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
	public boolean enabled() {
		return this.builder.enabled;
	}

	@Override
	public boolean allowCharging() {
		return this.builder.allowCharging;
	}

	@Override
	public String[] evcs_ids() {
		return this.builder.evcsIds;
	}

	@Override
	public int imbalanceHoldTime() {
		return this.builder.imbalanceHoldTime;
	}

	@Override
	public int currentStep() {
		return this.builder.currentStep;
	}

	@Override
	public int stepInterval() {
		return this.builder.stepInterval;
	}

	@Override
	public int redHoldTime() {
		return this.builder.redHoldTime;
	}

	@Override
	public String Evcs_target() {
		return this.builder.evcsTarget;
	}

	@Override
	public boolean allowPrioritization() {
		return this.builder.allowPrioritization;
	}

	@Override
	public String[] evcsClusterLimiter_ids() {
		return this.builder.evcsClusterLimiterIds;
	}

	@Override
	public int limitsExceededTime() {
		return this.builder.residualExcessHoldTime;
	}

	@Override
	public String EvcsClusterLimiter_target() {
		return this.builder.evcsClusterLimiterTarget;
	}

	@Override
	public int roundRobinTime() {
		return this.builder.roundRobinTime;
	}

	@Override
	public boolean verboseDebug() {
		return false;
	}
}
