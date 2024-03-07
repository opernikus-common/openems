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
 * The EvcsClusterLimiterControllerImpl computes channels needed by the cluster
 * chargemanagement.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Evcs.ClusterLimiter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
// TODO inkonistentes Naming: Interface beinhaltet nicht "Controller", Impl schon
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
			this.checkPhaseImbalance();
		}

		this.checkSafeOperationMode();

		this._setConfigWarning(this.meterHandler.isStateConfigurationWarning());
	}

	/**
	 * Checks the phase imbalance and sets corresponding channels.
	 */
	private void checkPhaseImbalance() {
		var phaseImbalance = this.meterHandler.getPhaseImbalance();
		this._setPhaseImbalance(phaseImbalance);
		this._setPhaseImbalanceCurrent(this.meterHandler.getImbalanceCurrent());
	}

	/**
	 * Checks if any of the phase currents is above the fuse limit and sets
	 * respective channel values.
	 */
	private void checkSafeOperationMode() {
		var safeOperationMode = this.meterHandler.getSafeOperationMode();
		// TODO Kann das den oben gesetzten Wert (if (!meterOk)...) überschreiben?
		this._setSafeOperationMode(safeOperationMode);
		this._setPhaseLimitExceededL1(!this.meterHandler.safeOperationModeL1());
		this._setPhaseLimitExceededL2(!this.meterHandler.safeOperationModeL2());
		this._setPhaseLimitExceededL3(!this.meterHandler.safeOperationModeL3());
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
				+ "Transport Capacity: " + this.getTransportCapacity() + ", " //
				+ "Safe Operation Mode: " + this.getSafeOperationMode() + ", " //
				+ "Phase Imbalance: " + this.getPhaseImbalance().asCamelCase() //
		;
	}

}
