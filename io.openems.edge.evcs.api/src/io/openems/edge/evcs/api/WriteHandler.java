package io.openems.edge.evcs.api;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Handles writes. Called in every cycle
 */
public class WriteHandler implements Runnable {

	private final Logger log = LoggerFactory.getLogger(WriteHandler.class);

	private final ManagedEvcs parent;
	
	private boolean applyChargePowerPerPhase = false;

	// Default power buffer for indicating a power increase or decrease in watt
	private static final int DEFAULT_INCREASE_BUFFER = 50;

	public WriteHandler(ManagedEvcs parent) {
		this.parent = parent;
	}

	/**
	 * Sends commands to the charging station depending on which profiles it
	 * implements.
	 * 
	 * <p>
	 * It is not sending a command, if the communication failed or the write channel
	 * is not set.
	 */
	@Override
	public void run() {
		if (this.parent.getChargingstationCommunicationFailed().orElse(true)) {
			this.setChargeState();
			return;
		}

		this.setEnergyLimit();
		this.setPower();
		this.setChargeState();
		this.setDisplay();
	}

	private int lastTarget = Integer.MIN_VALUE;
	private LocalDateTime nextPowerWrite = LocalDateTime.MIN;

	/**
	 * Sets the current or power from SET_CHARGE_POWER channel.
	 * 
	 * <p>
	 * Depending on the charging type it will send different commands with different
	 * units. Invalid values are discarded. If the energy limit is reached it will
	 * send zero.
	 */
	private void setPower() {
		int energyLimit = this.parent.getSetEnergyLimitChannel().getNextValue().orElse(0);

		// Check energy limit
		if (energyLimit > 0 && this.parent.getEnergySession().orElse(0) >= energyLimit) {

			try {
				this.parent.setDisplayText(energyLimit + " Wh erreicht");
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}

			// Attention: Even if the state is set in here, the read state of an EVCS could
			// be CHARGING and could overrides this state
			this.logDebug("Maximum energy limit [" + energyLimit + "] reached");
			this.parent._setStatus(Status.ENERGY_LIMIT_REACHED);

			// Apply Charge Power
			if (this.lastTarget != 0 || this.parent.getChargePower().orElse(0) != 0) {
				this.parent.getChargeStateHandler().applyNewChargeState(ChargeState.DECREASING);
				this.applyChargePower(0);
			}
		} else {

			Optional<Integer> valueOpt = this.parent.getSetChargePowerLimitChannel().getNextWriteValueAndReset();

			if (valueOpt.isPresent()) {
				int power = valueOpt.get();

				int minPower;
				int maxPower;

				if (this.applyChargePowerPerPhase) {
					// Minimum and maximum hardware power
					maxPower = this.parent.getFixedMaximumHardwarePower()
							.orElse(this.parent.getConfiguredMaximumHardwarePower());
					minPower = this.parent.getFixedMinimumHardwarePower()
							.orElse(this.parent.getConfiguredMinimumHardwarePower());
				} else {
					// Minimum and maximum hardware power
					maxPower = this.parent.getMaximumHardwarePower()
							.orElse(this.parent.getConfiguredMaximumHardwarePower());
					minPower = this.parent.getMinimumHardwarePower()
							.orElse(this.parent.getConfiguredMinimumHardwarePower());
				}
				// Adjust the power to the minimum and maximum power
				var target = Math.min(maxPower, power);
				target = target < minPower ? 0 : target;

				/*
				 * Try to apply new charge state.
				 */
				boolean newStateAccepted = true;
				if (target > this.lastTarget + DEFAULT_INCREASE_BUFFER) {
					newStateAccepted = this.parent.getChargeStateHandler().applyNewChargeState(ChargeState.INCREASING);

				} else if (target < this.lastTarget - DEFAULT_INCREASE_BUFFER) {
					newStateAccepted = this.parent.getChargeStateHandler().applyNewChargeState(ChargeState.DECREASING);
				}
				if (this.parent.ignoreChargeState()) {
					newStateAccepted = true;
				}

				target = newStateAccepted ? target : this.lastTarget;

				/*
				 * Only if the target has changed or a time has passed.
				 */
				if (this.lastTarget != target || this.nextPowerWrite.isBefore(LocalDateTime.now())) {
					this.applyChargePower(target);
				}
			}
		}
	}

	/**
	 * Apply the given charge power.
	 * 
	 * <p>
	 * Call the applyChargePowerLimit or pauseChargeProcess method for each EVCS
	 * implementation.
	 * 
	 * @param power Power that should be applied
	 */
	private void applyChargePower(int power) {
		try {

			boolean sent = false;
			if (power <= 0) {
				sent = this.parent.pauseChargeProcess();
			} else {
				sent = this.parent.applyChargePowerLimit(power);
			}

			if (sent) {
				this.logDebug("Setting EVCS " + this.parent.alias() + " charge power to " + power + " W");
				this.parent.setDisplayText(power + " W");

				this.parent._setSetChargePowerLimit(power);
				this.nextPowerWrite = LocalDateTime.now().plusSeconds(this.parent.getWriteInterval());
				this.lastTarget = power;
			} else {
				this.logDebug("Failed to set charge Power to " + this.parent.alias());
			}
		} catch (Exception e) {
			OpenemsComponent.logWarn(this.parent, this.log,
					"Sending the charge power limit failed [" + this.parent.id() + "]: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private Integer lastEnergySession = null;
	private LocalDateTime nextEnergySessionWrite = LocalDateTime.MIN;

	/**
	 * Sets the nextValue of the SET_ENERGY_LIMIT channel.
	 */
	private void setEnergyLimit() {

		Optional<Integer> valueOpt = this.parent.getSetEnergyLimitChannel().getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {
			Integer energyLimit = valueOpt.get();

			/*
			 * Only if the target has changed or a time has passed.
			 */
			if (!energyLimit.equals(this.lastEnergySession)
					|| this.nextEnergySessionWrite.isBefore(LocalDateTime.now())) {

				this.parent._setSetEnergyLimit(energyLimit);
				this.logDebug("Setting EVCS " + this.parent.alias() + " Energy Limit in this Session to [" + energyLimit
						+ " Wh]");
				this.lastEnergySession = energyLimit;
				this.nextEnergySessionWrite = LocalDateTime.now().plusSeconds(this.parent.getWriteInterval());
			}
		}
	}

	private String lastDisplay = null;
	private LocalDateTime nextDisplayWrite = LocalDateTime.MIN;

	/**
	 * Sets the display text from SET_DISPLAY channel.
	 * 
	 * <p>
	 */
	private void setDisplay() {
		Optional<String> valueOpt = this.parent.getSetDisplayTextChannel().getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {
			String text = valueOpt.get();

			/*
			 * Only if the display text has changed or a time has passed.
			 */
			if (!text.equals(this.lastDisplay) || this.nextDisplayWrite.isBefore(LocalDateTime.now())) {

				boolean sentSuccessfully;
				try {
					sentSuccessfully = this.parent.applyDisplayText(text);

					if (sentSuccessfully) {
						this.nextDisplayWrite = LocalDateTime.now().plusSeconds(this.parent.getWriteInterval());
						this.lastDisplay = text;
					}
				} catch (Exception e) {
					OpenemsComponent.logWarn(this.parent, this.log,
							"Setting the display text failed [" + this.parent.id() + "]: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Set the current ChargeState depending on the Evcs Status.
	 */
	private void setChargeState() {
		ChargeState chargeState = ChargeState.UNDEFINED;
		Status status = this.parent.getStatusChannel().getNextValue().asEnum();

		switch (status) {
		case CHARGING:
			chargeState = ChargeState.CHARGING;
			break;
		case CHARGING_FINISHED:
		case CHARGING_REJECTED:
		case ENERGY_LIMIT_REACHED:
		case ERROR:
		case STARTING:
		case READY_FOR_CHARGING:
		case NOT_READY_FOR_CHARGING:
			chargeState = ChargeState.NOT_CHARGING;
			break;
		case UNDEFINED:
			chargeState = ChargeState.UNDEFINED;
			break;
		}
		this.parent.getChargeStateHandler().applyNewChargeState(chargeState);
	}

	private void logDebug(String message) {
		if (this.parent.getConfiguredDebugMode()) {
			OpenemsComponent.logInfo(this.parent, this.log, message);
		}
	}

	/**
	 * Set if you want to use the fixed min/max hardware powers or the min/max hardware powers to calculate the power setpoint.
	 * 
	 * @param applyPerPhase true, if you want to use fixed min/max hardware powers
	 */
	public void applyChargePowerPerPhase(boolean applyPerPhase) {
		this.applyChargePowerPerPhase = applyPerPhase;
	}
	
}
