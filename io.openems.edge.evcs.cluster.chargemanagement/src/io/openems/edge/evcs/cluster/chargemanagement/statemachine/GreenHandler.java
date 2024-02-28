package io.openems.edge.evcs.cluster.chargemanagement.statemachine;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmt;
import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmtImpl;
import io.openems.edge.evcs.cluster.chargemanagement.State;
import io.openems.edge.evcs.cluster.chargemanagement.utils.EvcsTools;
import io.openems.edge.evcs.cluster.chargemanagement.utils.TargetPowerZone;

public class GreenHandler extends BaseHandler {

	private boolean resetLimitsOnEntry;
	private Context context;
	protected int stepInterval;
	protected Instant lastLimitChangeTime = Instant.MIN;
	private TargetPowerZone targetPowerZone;
	private BiConsumer<Value<Integer>, Value<Integer>> numberChangeCallback;

	public GreenHandler() {
		super();
	}

	@Override
	protected void onEntry(Context context) throws OpenemsNamedException {
		this.context = context;
		this.stepInterval = context.getConfig().stepInterval() - 1;
		this.resetLimitsOnEntry = true;
		this.lastLimitChangeTime = Instant.now();
		this.context.getImbalanceHoldTimer().reset();
		this.context.getLimitsExceededTimer().reset();
		this.context.getCluster().limitPowerAll(context.getCluster().getEvcsMinPowerLimit());
		this.reinitTargetPowerZone();
	}

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		this.context = context;
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
		// check phase imbalance
		if (context.getCableConstraints().isUnbalanced()) {
			if (context.getImbalanceHoldTimer().checkAndReset()) {
				return State.YELLOW;
			}
		} else {
			context.getImbalanceHoldTimer().reset();
		}

		// check target limits
		if (context.getCableConstraints().exceedsTargetLimit()) {
			// unable to handle aboveTargetLimit-situation within time. Switch state.
			if (context.getLimitsExceededTimer().checkAndReset()) {
				return State.YELLOW;
			}
		} else {
			context.getLimitsExceededTimer().reset();
		}

		// TODO switch to yellow immediately, when all charging evcss are at minPower
		// and MIN_FREE_AVAILABLE_POWER < 0

		this.adoptLimits();
		return State.GREEN;
	}

	private void reinitTargetPowerZone() {
		this.context.getTargetPowerZoneTimer().reset();
		this.targetPowerZone = new TargetPowerZone();

		if (this.numberChangeCallback == null) {
			this.numberChangeCallback = (t, u) -> {
				GreenHandler.this.targetPowerZone.disableTemporarily(GreenHandler.this.context);
			};
		}
		this.context.getParent().getNumberOfChargingEvcsChannel().removeOnChangeCallback(this.numberChangeCallback);
		this.context.getParent().getNumberOfChargingEvcsChannel().onChange(this.numberChangeCallback);
		this.context.getParent().getNumberOfChargingEvcsPrioChannel().removeOnChangeCallback(this.numberChangeCallback);
		this.context.getParent().getNumberOfChargingEvcsPrioChannel().onChange(this.numberChangeCallback);
	}

	private void adoptLimits() throws OpenemsNamedException {

		if ((!this.context.getCableConstraints().isUnbalanced())
				&& this.context.getCableConstraints().getMinFreeAvailablePower() > 0) {
			this.increaseChargePower();

		} else {
			this.decreaseChargePower();
		}
	}

	private void increaseChargePower() throws OpenemsNamedException {
		if (!this.resetLimitsOnEntry) {
			this.resetLimitsOnEntry = true;
			this.targetPowerZone.setHigherBorder(this.context,
					this.context.getCableConstraints().getMinFreeAvailablePower());
		}
		// do not increase if within target power zone
		if (this.targetPowerZone.fitsTargetPowerZone(this.context)) {
			this.holdLimit();
			return;
		}
		this.logInfo(this.context,
				" GREEN ---                                 Increase "
						+ this.context.getCableConstraints().getMinFreeAvailablePower() + " cluster "
						+ this.context.getParent().getChargePower());
		if (this.incrPrioLimits(this.context.getParent())) {
			return;
		}
		this.incrLimits(this.context.getParent());
	}

	private void decreaseChargePower() throws OpenemsNamedException {
		this.logInfo(this.context,
				" GREEN ---                                 Decrease "
						+ this.context.getCableConstraints().getMinFreeAvailablePower() + " cluster "
						+ this.context.getParent().getChargePower());
		// only very few power available (close to yellow) or unbalanced
		if (this.resetLimitsOnEntry) {
			this.resetLimitsOnEntry = false;
			// context.getCluster().reinitPrioLimits();
			// context.getCluster().reinitLimits();
			this.targetPowerZone.setLowerBorder(this.context.getCableConstraints().getMinFreeAvailablePower());
			return;
		}
		if (this.decrLimits(this.context.getParent())) {
			return;
		}
		this.decrPrioLimits(this.context.getParent());

	}

	private boolean hasStepIntervalTimePassed(boolean decreasing) {
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
		if (this.hasStepIntervalTimePassed(true)) {
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
		if (this.hasStepIntervalTimePassed(true)) {
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

		/*
		 * TODO 10 active chargingpoints a 3 phases => one increase step of 1A means
		 * 10*3*230 ~ 6kW power change. -> space for improvement. Do not increase 1A for
		 * all chargepoints. Maybe increase only 5 chargepoints and in the next step
		 * increase another 5 chargepoints. Same for incrPrioLimits. Maybe same for
		 * decrease
		 */

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
		if (this.hasStepIntervalTimePassed(false)) {
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
		if (this.hasStepIntervalTimePassed(false)) {
			EvcsTools.increaseDistributedPowerPrio(parent);
		}
		return true;
	}

	private void holdLimit() throws OpenemsNamedException {
		EvcsTools.holdDistributedPowerAll(this.context.getParent());
	}

}
