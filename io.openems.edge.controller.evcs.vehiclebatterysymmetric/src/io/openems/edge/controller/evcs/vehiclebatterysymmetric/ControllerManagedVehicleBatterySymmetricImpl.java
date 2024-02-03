package io.openems.edge.controller.evcs.vehiclebatterysymmetric;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.timer.Timer;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.core.timer.TimerManager;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.evcs.api.ManagedVehicleBattery;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs.Vehicle.Battery.Symmetric", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerManagedVehicleBatterySymmetricImpl extends AbstractOpenemsComponent
		implements ControllerManagedVehicleBatterySymmetric, ElectricityMeter, Controller, OpenemsComponent {

	private Config config;

	@Reference
	private ConfigurationAdmin configAdmin;

	@Reference
	private Sum sum;

	@Reference
	private TimerManager timerManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricEss ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedVehicleBattery evcs;

	private boolean allowCharging;
	private boolean allowDischarging;

	private Timer chargingCounter;
	private Timer dischargingCounter;

	public ControllerManagedVehicleBatterySymmetricImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerManagedVehicleBatterySymmetric.ChannelId.values(), ElectricityMeter.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		if (OpenemsComponent.updateReferenceFilter(this.configAdmin, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(this.configAdmin, this.servicePid(), "evcs", config.evcs_id())) {
			return;
		}
		this.chargingCounter = this.timerManager.getTimerByCoreCycles(//
				this.channel(ControllerManagedVehicleBatterySymmetric.ChannelId.CHARGING_CYCLE_COUNTER), 10);
		this.dischargingCounter = this.timerManager.getTimerByCoreCycles(//
				this.channel(ControllerManagedVehicleBatterySymmetric.ChannelId.DISCHARGING_CYCLE_COUNTER), 10);

		this.addListener();

		/*
		 * TODO react to config update int minChargePower() default 4200; int
		 * maxChargePower() default 11000; int maxDischargePower() default 11000; int
		 * dischargePower() default 1000; handle InfoStateChannels
		 */

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void addListener() {
		this.evcs.getActivePowerChannel().onUpdate(value -> {
			if (value.isDefined() && value.get().intValue() < 0) {
				this._setActivePower(-value.get());
			} else {
				this._setActivePower(0);
			}
		});
	}

	@Override
	public void run() throws OpenemsNamedException {

		if (this.evcs.getStatus() == Status.NOT_READY_FOR_CHARGING) {
			this.channel(ControllerManagedVehicleBatterySymmetric.ChannelId.CHARGING_BLOCKED).setNextValue(Level.OK);
			this.channel(ControllerManagedVehicleBatterySymmetric.ChannelId.DISCHARGING_BLOCKED).setNextValue(Level.OK);
			return;
		}

		var chargingAllowed = this.ess.getSoc().getOrError() > this.config.minEssSoc()
				&& this.evcs.getSoc().orElse(0) < this.config.maxVehicleSoc()
				// ESS should not discharge more than 2000W without the EVCS
				&& this.ess.getActivePower().getOrError() - this.evcs.getChargePower().orElse(4200) < 2000;

		if (chargingAllowed == this.allowCharging) {
			this.chargingCounter.reset();
		}

		if (this.chargingCounter.check()) {
			this.allowCharging = chargingAllowed;
			this.chargingCounter.reset();
		}

		var dischargingAllowed = this.ess.getSoc().getOrError() < this.config.maxEssSoc()
				&& this.evcs.getSoc().orElse(0) > this.config.minVehicleSoc();

		if (dischargingAllowed == this.allowDischarging) {
			this.dischargingCounter.reset();
		}

		if (this.dischargingCounter.check()) {
			this.allowDischarging = dischargingAllowed;
			this.dischargingCounter.reset();
		}

		Integer chargePower = 0;

		this._setMode(this.config.mode());

		// TODO etwas verzögerung zwischen dem umschalten von oldMode nach newMode, am
		// besten mit 0-durchgang der Leistung am Auto

		switch (this.config.mode()) {
		case OFF -> {
			this.allowCharging = false;
			this.allowDischarging = false;
			this.evcs._setBatteryMode(false);
		}
		case CHARGE -> {
			this.allowDischarging = false;
			chargePower = this.config.minChargePower();
			this.evcs._setBatteryMode(false);
		}
		case DISCHARGE -> {
			this.allowCharging = false;
			chargePower = -this.config.dischargePower(); // negative power for discharge
			this.evcs._setBatteryMode(true);
		}
		case AUTOMATIC -> {
			chargePower = this.calculateChargePower();
			if (chargePower < 0) {
				this.evcs._setBatteryMode(true);
			} else {
				this.evcs._setBatteryMode(false);
			}
		}
		}
		this.updateInfoStates();
		this.apply(chargePower);
	}

	/**
	 * Calculates the requested charge power.
	 *
	 * @return negative values for discharge, positive values for charge
	 */
	private Integer calculateChargePower() {
		var essPowerWithoutEvcs = this.sum.getEssActivePower().orElse(0) - this.evcs.getChargePower().orElse(0);
		if (essPowerWithoutEvcs < -this.config.minChargePower()) {
			return this.config.minChargePower();
		}
		if (essPowerWithoutEvcs > 0) {
			return -(essPowerWithoutEvcs / 2);
		}
		return 0;
	}

	private void apply(int chargePower) throws OpenemsNamedException {

		if (this.allowDischarging && chargePower <= 0) {
			// assuming Battery Mode
			// SetActivePower: neg Val -> charg of battery, pos Val -> discharg
			this.evcs.setActivePowerWriteValue(-chargePower);
			this.getDebugReqActivePowerChannel().setNextValue(-chargePower);
			this.evcs.setChargePowerLimit(null);
			return;
		}
		if (this.allowCharging && chargePower >= 0) {
			// request charge
			if (this.evcs.getBatteryMode().orElse(false)) {
				// in Battery Mode
				this.evcs.setActivePowerWriteValue(-chargePower);
				this.getDebugReqActivePowerChannel().setNextValue(-chargePower);
				this.evcs.setChargePowerLimit(null);
			} else {
				// in EVCS Mode
				chargePower = Math.max(0, chargePower);
				this.evcs.setChargePowerLimit(chargePower);
				this.evcs.setActivePowerWriteValue(null);
				this.getDebugReqActivePowerChannel().setNextValue(chargePower);
			}
			return;
		}

		// SetActivePower: neg Val -> charg of battery, pos Val -> discharg
		this.evcs.setActivePowerWriteValue(0);
		this.evcs.setChargePowerLimit(0);
		this.getDebugReqActivePowerChannel().setNextValue(0);
	}

	private void updateInfoStates() {
		this._setChargingBlocked(this.config.mode() == Mode.CHARGE && !this.allowCharging);
		this._setChargingBlocked(this.config.mode() == Mode.DISCHARGE && !this.allowDischarging);
	}

	@Override
	public String debugLog() {
		return "BatMode: " + this.evcs.getBatteryMode() + ", Mode: " + this.getMode() + ", SOC: " + this.evcs.getSoc()
				+ ", ReqPower: " + this.getDebugReqActivePowerChannel().value() + ", ActivePower: "
				+ this.getActivePower();
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}

}
