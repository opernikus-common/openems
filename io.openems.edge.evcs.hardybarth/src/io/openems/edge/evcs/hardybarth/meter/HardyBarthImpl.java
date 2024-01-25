package io.openems.edge.evcs.hardybarth.meter;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.hardybarth.common.HardyBarthApi;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.HardyBarth.Meter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class HardyBarthImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, EventHandler, HardyBarth, AsymmetricMeter, SymmetricMeter {

	private static final String API_ENDPOINT = "/api/v1";
	
	protected final Logger log = LoggerFactory.getLogger(HardyBarthImpl.class);
	protected Config config;

	// API for main REST API functions
	protected HardyBarthApi api;
	// ReadWorker and WriteHandler: Reading and sending data to the EVCS
	private HardyBarthReadWorker readWorker;

	public HardyBarthImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				HardyBarth.ChannelId.values() //
		);
	}
	
	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException{
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		if (config.enabled()) {
			this.api = new HardyBarthApi(config.ip(), config.port(), API_ENDPOINT);
			
			this.readWorker = new HardyBarthReadWorker(this);
			
			// Reading the given values
			this.readWorker.activate(config.id());
			this.readWorker.triggerNextRun();
			this.installEvcsHandlers();
			
		}
	}


	@Deactivate
	protected void deactivate() {
		super.deactivate();

		if (this.readWorker != null) {
			this.readWorker.deactivate();
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.readWorker.triggerNextRun();
			break;
		}
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
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}
	
	
	private int computePower(HardyBarth.ChannelId channelNamePlus, HardyBarth.ChannelId channelNameMinus) {
		try {
			@SuppressWarnings("unchecked")
			Value<Float> powerPlus = (Value<Float>) this.channel(channelNamePlus).value();
			@SuppressWarnings("unchecked")
			Value<Float> powerMinus = (Value<Float>) this.channel(channelNameMinus).value();
			
			if(powerPlus.isDefined() && powerMinus.isDefined()) {
				Float plus = powerPlus.get();
				Float minus = powerMinus.get();

				Float together = -1 *Math.abs(minus) + Math.abs(plus);
				/**
				 * <li>Range: negative values for Consumption (power that is 'leaving the
				 * system', e.g. feed-to-grid); positive for Production (power that is 'entering
				 */
				
				if(this.config.invert()) {
					return Math.round(-1* together);
				}
				return Math.round(together);
			}
		}catch(Exception e) {
			this.logError(this.log, "ex onUpdateActivePower " + e.getMessage());
			//TODO
//			this.getStateChannel()._setNextValue(Level.ERROR);
		}
		return 0;
	}
	
	
	private void installEvcsHandlers() {
		
		AsymmetricMeter.initializePowerSumChannels(this);
		
//		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS).onUpdate(
//				(newValue) -> {
//					this._setActivePower(
//							this.computePower(
//									HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS, 
//									HardyBarth.ChannelId.RAW_ACTIVE_POWER_MINUS)
//							);
//				});
//		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_POWER_MINUS).onUpdate(
//				(newValue) -> {
//					this._setActivePower(
//							this.computePower(
//									HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS, 
//									HardyBarth.ChannelId.RAW_ACTIVE_POWER_MINUS)
//							);
//				});
//		
		//asymmetric

		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L1).onUpdate(
				(newValue) -> {
					this._setActivePowerL1(
							this.computePower(
									HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L1, 
									HardyBarth.ChannelId.RAW_ACTIVE_POWER_MINUS_L1)
							);
				});
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_POWER_MINUS_L1).onUpdate(
				(newValue) -> {
					this._setActivePowerL1(
							this.computePower(
									HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L1, 
									HardyBarth.ChannelId.RAW_ACTIVE_POWER_MINUS_L1)
							);
				});

		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L2).onUpdate(
				(newValue) -> {
					this._setActivePowerL2(
							this.computePower(
									HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L2, 
									HardyBarth.ChannelId.RAW_ACTIVE_POWER_MINUS_L2)
							);
				});
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_POWER_MINUS_L2).onUpdate(
				(newValue) -> {
					this._setActivePowerL2(
							this.computePower(
									HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L2, 
									HardyBarth.ChannelId.RAW_ACTIVE_POWER_MINUS_L2)
							);
				});
		
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L3).onUpdate(
				(newValue) -> {
					this._setActivePowerL3(
							this.computePower(
									HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L3, 
									HardyBarth.ChannelId.RAW_ACTIVE_POWER_MINUS_L3)
							);
				});
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_POWER_MINUS_L3).onUpdate(
				(newValue) -> {
					this._setActivePowerL3(
							this.computePower(
									HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L3, 
									HardyBarth.ChannelId.RAW_ACTIVE_POWER_MINUS_L3)
							);
				});


		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_VOLTAGE_L1).onUpdate(
				(newValue) -> { 
					if(newValue.isDefined()) {
						this._setVoltageL1( Math.round((Float)newValue.get()));
					}else {
						this._setVoltageL1(null);
					}
					
				});
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_VOLTAGE_L2).onUpdate(
				(newValue) -> { 
					if(newValue.isDefined()) {
						this._setVoltageL2( Math.round((Float)newValue.get()));
					}else {
						this._setVoltageL2(null);
					}
					
				});
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_VOLTAGE_L3).onUpdate(
				(newValue) -> { 
					if(newValue.isDefined()) {
						this._setVoltageL3( Math.round((Float)newValue.get()));
					}else {
						this._setVoltageL3(null);
					}
					
				});

		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_CURRENT_L1).onUpdate(
				(newValue) -> { 
					if(newValue.isDefined()) {
						this._setCurrentL1(Math.round((Float)newValue.get()));
					}else {
						this._setCurrentL1(null);
					}
					
				});
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_CURRENT_L2).onUpdate(
				(newValue) -> { 
					if(newValue.isDefined()) {
						this._setCurrentL2(Math.round((Float)newValue.get()));
					}else {
						this._setCurrentL2(null);
					}
					
				});
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_CURRENT_L3).onUpdate(
				(newValue) -> { 
					if(newValue.isDefined()) {
						this._setCurrentL3(Math.round((Float)newValue.get()));
					}else {
						this._setCurrentL3(null);
					}
				});

		

		//TODO REACTIVE_POWER_L1
		//TODO REACTIVE_POWER_L2
		//TODO REACTIVE_POWER_L3
		
		
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_ENERGY_MINUS).onUpdate(
				(newValue) -> {
					if(newValue.isDefined()) {
						//convert kWh to Wh
						int watt = Math.round((Float)newValue.get())* 1000; 
						if(this.config.invert()) {
							watt *= -1;
						}
						this._setActiveProductionEnergy(watt);
					}
				});
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_ENERGY_PLUS).onUpdate(
				(newValue) -> {
					if(newValue.isDefined()) {
						//convert kWh to Wh
						int watt = Math.round((Float)newValue.get())* 1000; 
						if(this.config.invert()) {
							watt *= -1;
						}
						this._setActiveConsumptionEnergy(watt);
					}
				});
		
		
		
		//symmetric
		//TODO FREQUENCY
		//TODO REACTIVE_POWER

		

		
	}


	@Override
	public String debugLog() {
		return this.getState() +"," + this.getActivePower();
	}


}
