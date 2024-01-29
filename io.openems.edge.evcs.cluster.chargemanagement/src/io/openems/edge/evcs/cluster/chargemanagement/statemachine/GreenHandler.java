package io.openems.edge.evcs.cluster.chargemanagement.statemachine;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmt;
import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmtImpl;
import io.openems.edge.evcs.cluster.chargemanagement.State;
import io.openems.edge.evcs.cluster.chargemanagement.utils.EvcsTools;

public class GreenHandler extends BaseHandler {

	private boolean resetLimitsOnEntry;
	protected int stepInterval;
	protected Instant lastLimitChangeTime = Instant.MIN;
	private Integer resPowerMaxSoftBorder = null;
	private Integer resPowerMinSoftBorder = null;

	public GreenHandler() {
		super();
	}

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.stepInterval = context.getConfig().stepInterval() - 1;
		this.resetLimitsOnEntry = true;
		this.lastLimitChangeTime = Instant.now();
		context.getImbalanceHoldTimer().reset();
		context.getLimitsExceededTimer().reset();
		context.getSoftBorderResetTimer().reset();
		context.getCluster().limitPowerAll(context.getCluster().getEvcsMinPowerLimit());
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var cluster = context.getParent();

		if (cluster.hasFaults()) {
			return State.RED;
		}
		if (!context.getParent().getAllowCharging().orElse(false)) {
			return State.RED;
		}
		if (!context.getCableConstraints().safeOperationMode()) {
			return State.RED;
		}
		if (context.getCableConstraints().isUnbalanced()) {
			if (this.hasDurationPassed(context.getImbalanceHoldTimer())) {
				// give green handler some time to fix unbalance, before going to yellow
				return State.YELLOW;
			}
		} else {
			context.getImbalanceHoldTimer().reset();
		}

		if (context.getCableConstraints().isAboveTargetLimit()) {
			if (this.hasDurationPassed(context.getLimitsExceededTimer())) {
				// unable to handle aboveTargetLimit-situation within time. Switch state.
				return State.YELLOW;
			}
		} else {
			context.getLimitsExceededTimer().reset();
		}

		// TODO wenn alle charge evcss bereits auf minPower sind und residualPower == 0
		// ist, sofort in gelb umschalten

		this.adoptLimits(context);
		return State.GREEN;
	}

	private void adoptLimits(Context context) throws OpenemsNamedException {

		if ((!context.getCableConstraints().isUnbalanced())
				&& context.getCableConstraints().getMinFreeAvailablePower() > 0) {

			// increase charge power

			if (!this.resetLimitsOnEntry) {
				this.resetLimitsOnEntry = true;
				this.setResPowerMaxSoftLimit(context, context.getCableConstraints().getMinFreeAvailablePower());
			}
			if (this.fitsWithinSoftBorder(context)) {
				return;
			}
			if (context.getConfig().verboseDebug()) {
				context.getParent()
						.logInfo(" GREEN ---                                 Increase "
								+ context.getCableConstraints().getMinFreeAvailablePower() + " cluster "
								+ context.getParent().getChargePower());
			}
			if (this.incrPrioLimits(context.getParent())) {
				return;
			}
			this.incrLimits(context.getParent());

		} else {

			// decrease charge power

			if (context.getConfig().verboseDebug()) {

				context.getParent()
						.logInfo(" GREEN ---                                 Decrease "
								+ context.getCableConstraints().getMinFreeAvailablePower() + " cluster "
								+ context.getParent().getChargePower());
			}
			// only very few power available (close to yellow) or unbalanced
			if (this.resetLimitsOnEntry) {
				this.resetLimitsOnEntry = false;
				// context.getCluster().reinitPrioLimits();
				// context.getCluster().reinitLimits();
				this.setResPowerMinLimit(context.getCableConstraints().getMinFreeAvailablePower());
				return;
			}
			if (this.decrLimits(context.getParent())) {
				return;
			}
			this.decrPrioLimits(context.getParent());
		}
	}

	private void setResPowerMinLimit(int curResidualPower) {
		// called when having residual power minimum
		if (this.resPowerMaxSoftBorder == null) {
			this.resPowerMinSoftBorder = curResidualPower;
		}
	}

	private void setResPowerMaxSoftLimit(Context context, int curResidualPower) {
		// called when having residual power maximum

		if (this.resPowerMinSoftBorder != null && this.resPowerMaxSoftBorder == null) {

			var offset = 0;
			if (this.resPowerMinSoftBorder < 0) {
				// readjust higher/lower soft border, because residual power should be > 0
				offset = -this.resPowerMinSoftBorder;
				this.resPowerMinSoftBorder = 0;
			}
			this.resPowerMaxSoftBorder = curResidualPower + offset;
			context.getParent().logInfo("-----------  Updated Soft Limits " + this.resPowerMinSoftBorder + " - "
					+ this.resPowerMaxSoftBorder);
			context.getSoftBorderResetTimer().reset();
		}
	}

	private boolean fitsWithinSoftBorder(Context context) {
		var residualPower = context.getCableConstraints().getMinFreeAvailablePower();
		if (this.resPowerMinSoftBorder == null || this.resPowerMaxSoftBorder == null) {
			return false;
		}
		if (this.hasDurationPassed(context.getSoftBorderResetTimer())) {
			this.resPowerMinSoftBorder = null;
			this.resPowerMaxSoftBorder = null;
			return false;
		}
		if (residualPower >= this.resPowerMinSoftBorder.intValue()
				&& residualPower <= this.resPowerMaxSoftBorder.intValue()) {
			// context.getParent().logInfo(" GREEN --- fitsSoftBorder " +
			// context.getCableConstraints().getResidualPower()
			// + " cluster " + context.getParent().getChargePower());
			return true;
		}
		return false;
	}

	private boolean hasDurationPassed(boolean decreasing) {
		var si = this.stepInterval;
		if (decreasing) {
			si = Math.max(0, (int) (si / 2));
		}
		if (Duration.between(this.lastLimitChangeTime, Instant.now()).getSeconds() > si) {
			this.lastLimitChangeTime = Instant.now();
			return true;
		}
		return false;
	}

	/**
	 * Decreases the limits of unprioritized Evcss.
	 *
	 * @param parent the {@link EvcsClusterChargeMgmt}
	 * @return true if still needs to decrease
	 * @throws OpenemsNamedException on write error
	 */
	private boolean decrLimits(EvcsClusterChargeMgmtImpl parent) throws OpenemsNamedException {
		if (EvcsTools.hasPowerLimitReachedMinPower(parent)) {
			var minEvcsLimit = parent.getContext().getCluster().getEvcsMinPowerLimit();
			parent.getContext().getCluster().limitPower(minEvcsLimit, true);
			return false;
		}
		if (parent.getContext().getCluster().hasChargingEvcs() == false) {
			EvcsTools.decreaseDistributedPower(parent);
			return true;
		}
		if (this.hasDurationPassed(true)) {
			EvcsTools.decreaseDistributedPower(parent);
		}
		return true;
	}

	/**
	 * Decreases the limits of prioritized Evcss.
	 * 
	 * @param parent the {@link EvcsClusterChargeMgmt}
	 * @return true if still needs to decrease
	 * @throws OpenemsNamedException on write error
	 */
	private boolean decrPrioLimits(EvcsClusterChargeMgmtImpl parent) throws OpenemsNamedException {
		if (EvcsTools.hasPowerLimitPrioReachedMinPower(parent)) {
			var minEvcsLimit = parent.getContext().getCluster().getEvcsMinPowerLimit();
			parent.getContext().getCluster().limitPowerPrio(minEvcsLimit, true);
			return false;
		}
		if (parent.getContext().getCluster().hasChargingPrioEvcs() == false) {
			var minEvcsLimit = parent.getContext().getCluster().getEvcsMinPowerLimit();
			parent.getContext().getCluster().limitPowerPrio(minEvcsLimit, false);
			return false;
		}
		if (this.hasDurationPassed(true)) {
			EvcsTools.decreaseDistributedPowerPrio(parent);
		}
		return true;
	}

	/**
	 * Increases the limits of unprioritized Evcss.
	 * 
	 * @param parent the {@link EvcsClusterChargeMgmt}
	 * @return true if still needs to increase
	 * @throws OpenemsNamedException on write error
	 */
	private boolean incrLimits(EvcsClusterChargeMgmtImpl parent) throws OpenemsNamedException {
		if (EvcsTools.hasPowerLimitReachedMaxPower(parent)) {
			var maxEvcsLimit = parent.getContext().getCluster().getEvcsMaxPowerLimit();
			parent.getContext().getCluster().limitPower(maxEvcsLimit, true);
			return false;
		}
		if (parent.getContext().getCluster().hasChargingEvcs() == false) {
			var maxEvcsLimit = parent.getContext().getCluster().getEvcsMaxPowerLimit();
			parent.getContext().getCluster().limitPower(maxEvcsLimit, true);
			return false;
		}
		if (this.hasDurationPassed(false)) {
			EvcsTools.increaseDistributedPower(parent);
		}
		return true;
	}

	/**
	 * Increases the limits of prioritized Evcss.
	 * 
	 * @param parent the {@link EvcsClusterChargeMgmt}
	 * @return true if still needs to increase
	 * @throws OpenemsNamedException on write error
	 */
	private boolean incrPrioLimits(EvcsClusterChargeMgmtImpl parent) throws OpenemsNamedException {
		if (EvcsTools.hasPowerLimitPrioReachedMaxPower(parent)) {
			var maxEvcsLimit = parent.getContext().getCluster().getEvcsMaxPowerLimit();
			parent.getContext().getCluster().limitPowerPrio(maxEvcsLimit, true);
			return false;
		}
		if (parent.getContext().getCluster().hasChargingPrioEvcs() == false) {
			var maxEvcsLimit = parent.getContext().getCluster().getEvcsMaxPowerLimit();
			parent.getContext().getCluster().limitPowerPrio(maxEvcsLimit, true);
			return true;
		}
		if (this.hasDurationPassed(false)) {
			EvcsTools.increaseDistributedPowerPrio(parent);
		}
		return true;
	}

}
