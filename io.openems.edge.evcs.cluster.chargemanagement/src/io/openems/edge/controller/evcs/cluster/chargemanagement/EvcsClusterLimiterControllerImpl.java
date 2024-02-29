package io.openems.edge.controller.evcs.cluster.chargemanagement;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * The EvcsClusterLimiterControllerImpl is responsible to continuously update
 * the following channels:
 *
 * <p>
 * <ul>
 * <li>MaxPowerLimit - max theoretically power limit on this limiter.
 * <li>ResidualPower - free power available on this limiter.
 * <li>SafeOperationMode - true if we are within defined limits.
 * <li>PhaseImbalance - phase imbalance (only set if this is the phase imbalance
 * responsible limiter).
 * <li>PhaseImbalanceCurrent - phase imbalance current (only set if this is the
 * phase imbalance responsible limiter).
 * </ul>
 *
 * <p>
 * Hints:
 *
 * <p>
 * -at no time we should reach config.fuseLimit()
 *
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs.ClusterLimiter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EvcsClusterLimiterControllerImpl extends AbstractOpenemsComponent
		implements EvcsClusterLimiter, Controller, OpenemsComponent {

	private final MeterHandler meterHandler;
	private Config config;
	private Integer limiterId;

	@Reference
	protected ConfigurationAdmin configAdmin;

	@Reference
	private ElectricityMeter meter;

	public EvcsClusterLimiterControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				EvcsClusterLimiter.ChannelId.values() //
		);
		this.meterHandler = new MeterHandler();
	}

	@Activate
	protected void activate(ComponentContext compContext, Config config) {
		this.config = config;
		super.activate(compContext, config.id(), config.alias(), config.enabled());
		this.modify(config);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.meterHandler.deactivate();
	}

	@Modified
	private void modified(ComponentContext compContext, Config config) {
		this.config = config;
		super.modified(compContext, config.id(), config.alias(), config.enabled());
		this.modify(config);
	}

	private void modify(Config config) {
		if (OpenemsComponent.updateReferenceFilter(this.configAdmin, this.servicePid(), "meter", config.meter_id())) {
			return;
		}
		if (!config.enabled()) {
			return;
		}
		try {
			this.limiterId = Integer.parseInt(this.config.id().replaceAll("[\\D]", ""));
		} catch (Exception e) {
			this.limiterId = -1;
		}
		// TODO
		// -https://dev.oems.energy/grafana/d/fd98a0cd-0e7a-4261-ad56-5c2eb0fdac07/evcs-cluster-fairshare?orgId=1&from=1700719258646&to=1700719541432
		// 2 ladeseaulen sind da, eine mit sehr hoher schieflast schiesst uns ueber rot
		// -> endlosschleife
		// ->
		// Regeln für Abstand TargetPower zu SafetyLimit (= fuseLimit-fuseSafetyOffset):
		// Muss mindestens: Phasenschieflast +1 sein.
		// Vorschlag: phasenschieflast nicht 20 sondern 14A = 2 einphasige Ladesaeulen
		// sind erlaubt
		// fuseSafetyLimit 15A
		// daraus folgt, wir brauchen eine Absicherung von 32A mindenstens

		this.meterHandler.activate(this, this.meter, config);
	}

	@Override
	public void run() throws OpenemsNamedException {
		if (!this.config.enabled()) {
			return;
		}

		// TODO note this channels are delayed by one core cycle compared to the
		// channels within the MeterHandler
		var meterOk = this.meterHandler.isMeterOk();
		this._setMeterError(!meterOk);
		if (!meterOk) {
			this._setSafeOperationMode(false);
			this._setFreeAvailablePower(0);
			return;
		}

		if (this.isPhaseImbalanceLimiter()) {
			var phaseImbalance = this.meterHandler.getPhaseImbalance();
			this._setPhaseImbalance(phaseImbalance);
			this._setPhaseImbalanceCurrent(this.meterHandler.getImbalanceCurrent());
		}

		var safeOperationMode = this.meterHandler.getSafeOperationMode();
		this._setSafeOperationMode(safeOperationMode);

		// var residualPower = this.meterHandler.getFreeAndAvailablePower();
		// this._setFreeAvailablePower(residualPower);

		this._setConfigWarning(this.meterHandler.isStateConfigurationWarning());

		/*
		 * TODO -in readme.adoc dokumentieren
		 */

	}

	@Override
	public boolean isPhaseImbalanceLimiter() {
		return this.config.isPhaseImbalanceLimiter();
	}

	@Override
	public Integer getLimiterId() {
		return this.limiterId;
	}

	@Override
	public String debugLog() {
		return this.getFreeAvailablePower() + ", max " //
				+ this.getTransportCapacity() + ", " //
				+ this.getSafeOperationMode() + ", " //
				+ this.getPhaseImbalance().asCamelCase() //
		;
	}

}
