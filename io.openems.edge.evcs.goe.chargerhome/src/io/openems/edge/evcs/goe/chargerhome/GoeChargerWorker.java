package io.openems.edge.evcs.goe.chargerhome;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.evcs.api.ChargeState;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;

public class GoeChargerWorker extends AbstractCycleWorker {

	private final EvcsGoeChargerHomeImpl parent;
	private final GoeApi goeapi;

	private int maxCurrent;
	private int lastCurrent;
	private int lastEnergySession;

	public GoeChargerWorker(EvcsGoeChargerHomeImpl goeCharger, GoeApi goeapi) {
		this.parent = goeCharger;
		this.goeapi = goeapi;

		this.maxCurrent = this.parent.config.maxHwCurrent();
		this.lastEnergySession = 0;
	}

	@Override
	protected void forever() throws Throwable {
		var json = this.goeapi.getStatus();
		if (json == null) {
			this.parent.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(true);

		} else {
			try {
				// Is Active
				var alw = JsonUtils.getAsInt(json, "alw");
				this.parent.channel(EvcsGoeChargerHome.ALLOW_CHARGING).setNextValue(alw);

				// General information
				this.parent.channel(EvcsGoeChargerHome.SERIAL).setNextValue(JsonUtils.getAsString(json, "sse"));
				this.parent.channel(EvcsGoeChargerHome.FIRMWARE).setNextValue(JsonUtils.getAsString(json, "fwv"));

				// Current status
				var status = JsonUtils.getAsInt(json, "car");
				this.parent.channel(EvcsGoeChargerHome.STATUS_GOE).setNextValue(status);
				this.parent.channel(Evcs.ChannelId.STATUS).setNextValue(this.convertGoeStatus(status));

				// Detailed charge information
				var activeCurrent = JsonUtils.getAsInt(json, "amp") * 1000;
				this.parent.channel(EvcsGoeChargerHome.CURR_USER).setNextValue(activeCurrent);

				var nrg = JsonUtils.getAsJsonArray(json, "nrg");
				this.parent.channel(EvcsGoeChargerHome.VOLTAGE_L1).setNextValue(JsonUtils.getAsInt(nrg.get(0)));
				this.parent.channel(EvcsGoeChargerHome.VOLTAGE_L2).setNextValue(JsonUtils.getAsInt(nrg.get(1)));
				this.parent.channel(EvcsGoeChargerHome.VOLTAGE_L3).setNextValue(JsonUtils.getAsInt(nrg.get(2)));
				this.parent.channel(Evcs.ChannelId.CURRENT_L1).setNextValue(JsonUtils.getAsInt(nrg.get(4)) * 100);
				this.parent.channel(Evcs.ChannelId.CURRENT_L2).setNextValue(JsonUtils.getAsInt(nrg.get(5)) * 100);
				this.parent.channel(Evcs.ChannelId.CURRENT_L3).setNextValue(JsonUtils.getAsInt(nrg.get(6)) * 100);
				var power = JsonUtils.getAsInt(nrg.get(11));
				this.parent.channel(EvcsGoeChargerHome.ACTUAL_POWER).setNextValue(power * 10);
				this.parent.channel(Evcs.ChannelId.CHARGE_POWER).setNextValue(power * 10);

				var phases = this.convertGoePhase(JsonUtils.getAsInt(json, "pha"));
				this.parent._setPhases(phases);

				this.updateHardwareLimits(JsonUtils.getAsInt(json, "cbl") * 1000, phases);

				// Energy
				this.parent.channel(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY)
						.setNextValue(JsonUtils.getAsInt(json, "eto") * 100);
				this.parent.channel(Evcs.ChannelId.ENERGY_SESSION)
						.setNextValue(JsonUtils.getAsInt(json, "dws") * 10 / 3600);

				// Error
				this.parent.channel(EvcsGoeChargerHome.ERROR).setNextValue(JsonUtils.getAsString(json, "err"));
				this.parent.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(false);

				// Set the power and energy
				this.setPower();
				this.setEnergySession();

			} catch (OpenemsNamedException e) {

				this.parent.debugLog("worker exception " + e.getMessage());
				// TODO: some log output would be nyce .. instead of looking for poor
				// ol'debugger again
				this.parent.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(true);
			}
		}

	}

	private void updateHardwareLimits(int cableCurrent, int phases) {

		this.maxCurrent = cableCurrent > 0 && cableCurrent < this.parent.config.maxHwCurrent() //
				? cableCurrent //
				: this.parent.config.maxHwCurrent();

		this.parent._setFixedMinimumHardwarePower(this.parent.config.minHwCurrent() / 1000 * phases * 230);
		// TODO: in case of an error before, MaximumHardWarePower remains
		// at an invalid value. Not read-able by the controller then. Please
		// provide one by default
		this.parent._setFixedMaximumHardwarePower(this.maxCurrent / 1000 * phases * 230);
	}

	private Status convertGoeStatus(int status) {
		switch (status) {
		case 1: // ready for charging, car unplugged
			this.parent.getChargeStateHandler().applyNewChargeState(ChargeState.NOT_CHARGING);
			return Status.NOT_READY_FOR_CHARGING;
		case 2: // charging
			this.parent.getChargeStateHandler().applyNewChargeState(ChargeState.CHARGING);
			return Status.CHARGING;
		case 3: // waiting for car
			this.parent.getChargeStateHandler().applyNewChargeState(ChargeState.NOT_CHARGING);
			return Status.READY_FOR_CHARGING;
		case 4: // charging finished, car plugged
			this.parent.getChargeStateHandler().applyNewChargeState(ChargeState.NOT_CHARGING);
			return Status.CHARGING_FINISHED;
		default:
			this.parent.getChargeStateHandler().applyNewChargeState(ChargeState.UNDEFINED);
			return Status.UNDEFINED;
		}
	}

	/**
	 * Converts the binary input into the amount of phases that are used to charge.
	 *
	 * @param phase binary phase input
	 * @return amount of phases
	 */
	private int convertGoePhase(int phase) {
		var phasen = (byte) phase & 0b00111000;
		switch (phasen) {
		case 8: // 0b00001000: Phase 1 is active
			return 1;
		case 24: // 0b00011000: Phase 1+2 is active
			return 2;
		case 56: // 0b00111000: Phase1-3 are active
			return 3;
		default:
			return 0;
		}
	}

	/**
	 * Sets the current from SET_CHARGE_POWER channel.
	 *
	 * <p>
	 * Possible charge currents are between MinCurrent and MaxCurrent. Values below
	 * are set to zero and values above are set to the maximum.
	 */
	private void setPower() throws OpenemsNamedException {
		WriteChannel<Integer> energyLimitChannel = this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT);
		int energyLimit = energyLimitChannel.getNextValue().orElse(0);
		// Check energy limit
		if (energyLimit == 0 || energyLimit > this.parent.getEnergySession().orElse(0)) {
			WriteChannel<Integer> channel = this.parent.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT);
			var valueOpt = channel.getNextWriteValueAndReset();
			if (valueOpt.isPresent()) {
				var power = valueOpt.get();
				Channel<Integer> minimumHardwarePowerChannel = this.parent
						.channel(Evcs.ChannelId.MINIMUM_HARDWARE_POWER);

				// Charging under MINIMUM_HARDWARE_POWER isn't possible
				if (power < minimumHardwarePowerChannel.value().orElse(0)) {
					power = 0;
					this.updateActiveState(false);
				} else {
					this.updateActiveState(true);
				}
				Integer phases = this.parent.getPhases().getValue();
				Integer current = power * 1000 / phases / 230; /* voltage */

				/*
				 * Limits the charging value because goe knows only values between MinCurrent
				 * and MaxCurrent
				 */
				if (current > this.maxCurrent) {
					current = this.maxCurrent;
				}
				if (current < this.parent.config.minHwCurrent()) {
					current = this.parent.config.minHwCurrent();
				}

				if (this.lastCurrent != current) {
					this.goeapi.setCurrent(current);
					this.parent.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT).setNextValue(power);
					this.parent.channel(EvcsGoeChargerHome.CURR_USER).setNextValue(current);
				}
			}
		} else {
			this.parent.debugLog("Maximum energy limit reached");
			this.parent._setStatus(Status.ENERGY_LIMIT_REACHED);
			this.updateActiveState(false);
		}
	}

	private void updateActiveState(boolean active) throws OpenemsNamedException {
		var irc = this.parent.channel(EvcsGoeChargerHome.ALLOW_CHARGING);
		Integer allowCharging = ((IntegerReadChannel) irc).value().asOptional().orElse(0);
		if (allowCharging == 0) {
			if (active) {
				this.goeapi.setActive(active);
			}
		} else if (!active) {
			this.goeapi.setActive(active);
		}
	}

	/**
	 * Sets the Energy Limit for this session from SET_ENERGY_SESSION channel.
	 *
	 * <p>
	 * Allowed values for the command setenergy are 0; 1-65535 the value of the
	 * command is 0.1 Wh. The charging station will charge till this limit.
	 */
	private void setEnergySession() throws OpenemsNamedException {
		WriteChannel<Integer> channel = this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT);
		var valueOpt = channel.getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {
			var energyTarget = valueOpt.get();
			if (energyTarget < 0) {
				return;
			}

			/*
			 * limits the target value because go-e knows only values between 0 and 65535
			 * 0.1Wh
			 */
			energyTarget /= 100;
			energyTarget = energyTarget > 65535 ? 65535 : energyTarget;
			energyTarget = energyTarget > 0 && energyTarget < 1 ? 1 : energyTarget;
			if (!energyTarget.equals(this.lastEnergySession)) {
				// Set energy limit
				this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT).setNextValue(energyTarget * 100);
				this.parent.debugLog("Setting go-e " + this.parent.alias() + " Energy Limit in this Session to ["
						+ energyTarget / 10 + " kWh]");

				this.goeapi.setMaxEnergy(energyTarget);
				this.lastEnergySession = energyTarget;
			}
		}
	}
}
