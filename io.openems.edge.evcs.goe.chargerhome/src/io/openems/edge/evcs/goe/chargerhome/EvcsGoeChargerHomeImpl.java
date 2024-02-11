package io.openems.edge.evcs.goe.chargerhome;

import java.net.UnknownHostException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Goe.ChargerHome", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class EvcsGoeChargerHomeImpl extends AbstractOpenemsComponent
		implements ManagedEvcs, Evcs, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(EvcsGoeChargerHomeImpl.class);
	private GoeApi goeapi = null;

	protected Config config;

	@Reference
	private EvcsPower evcsPower;

	private GoeChargerWorker worker;

	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	public EvcsGoeChargerHomeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				EvcsGoeChargerHome.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.channel(EvcsGoeChargerHome.ALIAS).setNextValue(config.alias());
		this.config = config;
		this._setChargingType(ChargingType.AC);
		this._setPowerPrecision(230);

		if (config.enabled()) {
			this.goeapi = new GoeApi(this);
			this.worker = new GoeChargerWorker(this, this.goeapi);
			this.worker.activate(config.id());
			this.worker.triggerNextRun();
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();

		if (this.worker != null) {
			this.worker.deactivate();
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.worker.triggerNextRun();
			break;
		default:
			break;
		}

	}

	@Override
	public String debugLog() {
		var chPoLimit = this.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT).getNextValue().asString();
		return "Limit:" + chPoLimit + "|" + this.getStatus().getName() + "|" + this.getState() + "|"
				+ this.getChargePower();
	}

	/**
	 * Debug Log.
	 * 
	 * <p>
	 * Logging only if the debug mode is enabled
	 * 
	 * @param message text that should be logged
	 */
	public void debugLog(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsException {
		this.goeapi.setActive(true);
		this._setSetChargePowerLimit(power);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws OpenemsException {
		this.goeapi.setActive(false);
		this._setSetChargePowerLimit(0);
		return true;
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 30;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return Math.round(this.config.minHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return Math.round(this.config.maxHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public void logDebug(String message) {
		if (this.getConfiguredDebugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public ChargeStateHandler getChargeStateHandler() {
		return this.chargeStateHandler;
	}
}
