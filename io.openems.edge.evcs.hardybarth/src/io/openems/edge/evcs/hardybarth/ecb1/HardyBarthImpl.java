package io.openems.edge.evcs.hardybarth.ecb1;

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

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.hardybarth.common.HardyBarthApi;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.HardyBarth.ECB1", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class HardyBarthImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, EventHandler, HardyBarth, Evcs, ManagedEvcs {

	private static final String API_ENDPOINT = "/api/v1";
	private static final float DETECT_PHASE_ACTIVITY		= 100; //Watt
	private int phasePattern = 0;
	
	protected final Logger log = LoggerFactory.getLogger(HardyBarthImpl.class);
	protected Config config;

	// API for main REST API functions
	protected HardyBarthApi api;

	// ReadWorker and WriteHandler: Reading and sending data to the EVCS
	private HardyBarthReadWorker readWorker;
	private HardyBarthWriteWorker writeWorker;

	private Float energyAtStartOfSession = null;
	private boolean evcsConnected = true;
	private boolean energyLimitReached = false;
	private boolean commErrorWrite = true;
	private boolean commErrorRead = true;


	@Reference
	private EvcsPower evcsPower;

	public HardyBarthImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				HardyBarth.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException{
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this._setChargingType(ChargingType.AC);
		
		int minHardwarePower = config.minHwCurrent() * 3 * 230; 
		if(this.config.minPowerOnePhaseOnly()) {
			minHardwarePower = config.minHwCurrent() * 230;
		}
		this._setMinimumHardwarePower(minHardwarePower);
		this._setMaximumHardwarePower(config.maxHwCurrent() * 3 * 230);
		this.updateRawOutMinCurrent(null);
		this.updateRawOutChargeCurrent(null);
		this._setPowerPrecision(230);
		this.energyLimitReached = false;

		if (config.enabled()) {
			this.api = new HardyBarthApi(config.ip(), config.port(), API_ENDPOINT, this);
			this.writeWorker = new HardyBarthWriteWorker(this);
			this.readWorker = new HardyBarthReadWorker(this, config.meterIndex(), config.chargeControlIndex());
			
			this.writeWorker.activate(config.id());
			this.readWorker.activate(config.id());
			
			this.writeWorker.triggerNextRun();
			this.readWorker.triggerNextRun();
			
			this.channel(Evcs.ChannelId.CHARGING_TYPE).setNextValue(ChargingType.AC);
			this.installEvcsHandlers();
			
			this._setPhases(null);
			
			//TODO POWER_PRECISION -> 230W = 1A
		}
	}


	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this._setPhases(null);

		if (this.readWorker != null) {
			this.readWorker.deactivate();
		}
		if (this.writeWorker != null) {
			this.writeWorker.deactivate();
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
			this.writeWorker.triggerNextRun();
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
	public void debugLog(String message, boolean writeLog) {
		//0 = deactivated, 1 = READ, 2 = write, 3 = all
		if (this.config.debug() < 1 || this.config.debug() > 3 ) {
			return;
		}
		if(writeLog && this.config.debug() < 2){
			return;
		}
		if(writeLog == false && this.config.debug() == 2 ) {
			return;
		}
		this.logInfo(this.log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}
	
	private void installEvcsHandlers() {
		
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS).onUpdate(
				(newValue) -> {
					if(newValue.isDefined()) {
						Float power = (Float) newValue.get();
						if(power.intValue() > 10) {
							this._setChargePower(power.intValue());
						}else {
							this._setChargePower(0);
						}
					}else {
						this._setChargePower(null);
					}
				});
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L1).onUpdate(
				(newValue) -> {
					if(newValue.isDefined()) {
						Float freq = (Float) newValue.get();
						 if( freq.intValue() > DETECT_PHASE_ACTIVITY) {
							 phasePattern |= 0x01;
						 }else {
							 phasePattern &= ~0x01;
						 }
					}else {
						phasePattern &= ~0x01;
					}
					this.updatePhases();
				});
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L2).onUpdate(
				(newValue) -> {
					if(newValue.isDefined()) {
						Float freq = (Float) newValue.get();
						 if( freq.intValue() > DETECT_PHASE_ACTIVITY) {
							 phasePattern |= 0x02;
						 }else {
							 phasePattern &= ~0x02;
						 }
					}else {
						phasePattern &= ~0x02;
					}
					this.updatePhases();
				});
		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L3).onUpdate(
				(newValue) -> {
					if(newValue.isDefined()) {
						Float freq = (Float) newValue.get();
						 if( freq.intValue() > DETECT_PHASE_ACTIVITY) {
							 phasePattern |= 0x04;
						 }else {
							 phasePattern &= ~0x04;
						 }
					}else {
						phasePattern &= ~0x04;
					}
					this.updatePhases();
				});

		this.channel(HardyBarth.ChannelId.RAW_ACTIVE_ENERGY_PLUS).onUpdate(
				(newValue) -> {
					if(newValue.isDefined()) {
						Float _kw = (Float) newValue.get();
						Float _w = _kw * 1000;
						this._setActiveConsumptionEnergy(_w.intValue());
						this._setEnergyTotal(_w.longValue());
						if(this.energyAtStartOfSession == null || this.evcsHasDisconnected()) {
							this.energyAtStartOfSession = _w;
						}
						if(this.isCharging()) {
							double thisSession = (_w-this.energyAtStartOfSession); //Wh
							this._setEnergySession((int)thisSession);
						}
						
					}else {
						this._setActiveConsumptionEnergy( null );
						this._setEnergyTotal( null );
					}
				});

		this.channel(HardyBarth.ChannelId.RAW_CHARGE_CONTROL_STATE).onUpdate(
				(newValue) -> {
					this.updateStateFromHardyBarthControlState(newValue);
				});

		//TODO
//		SET_CHARGE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
//		SET_CHARGE_POWER_LIMIT_WITH_FILTER(new IntegerDoc() //
//		IS_CLUSTERED(Doc.of(OpenemsType.BOOLEAN) //
//		SET_DISPLAY_TEXT(Doc.of(OpenemsType.STRING) //
//		SET_CHARGE_POWER_REQUEST(Doc.of(OpenemsType.INTEGER) //
//		SET_ENERGY_LIMIT(Doc.of(OpenemsType.INTEGER) //
		
	}

	
	private void updatePhases() {
		Status stat = this.getStatus();
		if(stat.isUndefined()) {
			this._setPhases(null);
			return;
		}
		switch(stat) {
		case UNDEFINED:
		case STARTING:
		case NOT_READY_FOR_CHARGING:
		case ERROR:
		case CHARGING_REJECTED:
		case ENERGY_LIMIT_REACHED:
		case CHARGING_FINISHED:
			this._setPhases(null);
			return;
		case READY_FOR_CHARGING:
		case CHARGING:
			break;
		}
		int bitCount = Integer.bitCount(phasePattern);
		this._setPhases(bitCount);
	}
	
	
	private void updateStateFromHardyBarthControlState(Value<?> newValue) {
		/**
		 * No documentation for the states so far. 
		 * 
		 * Old States of the Hardy Barth Salia are documented:
		 * <ul>
		 * <li>A = Free (no EV connected)
		 * <li>B = EV connected, no charging (pause state)
		 * <li>C = Charging
		 * <li>D = Charging (With ventilation)
		 * <li>E = Deactivated Socket
		 * <li>F = Failure
		 * </ul>
		 * 
		 * eCB1:
		 * During charging we see the following phases:  
		 * <ul>
		 * <li>A
		 * <li>B2
		 * <li>C
		 * <li>B2
		 * </ul>
		 *    
		 * The following States are seen on eCB1-Charging stations:
		 *    U, A, C, B2
	     *
		 * Reverse Engineered states:
		 * <ul>
		 * <li>A  - Free (no EV Connected)
		 * <li>B1 - EV connected, not ready for charging (http://astat-emc.pl/wp-content/uploads/pro-typ1-ba_gb.pdf)
		 * <li>B2 - EV connected, no Charging
		 * <li>C  - Charging
		 * <li>A’ - charging suspended, but we are still charging
		 * </ul>
		 *    
		 * TODO: Handle State: U -> no Charge Controller
		 * 
		 * 
		 * States A, B, C, D and E can be simulated in accordance with IEC 61851
		 */
		
		Status rawStatus = Status.UNDEFINED;
		if(newValue.isDefined()) {
			switch (newValue.get().toString()) {
			case "A":
				rawStatus = Status.NOT_READY_FOR_CHARGING;
				break;
			case "B":  //?
				rawStatus = Status.CHARGING_REJECTED;
				break;
			case "B2": //?
				rawStatus = Status.CHARGING_FINISHED;
				if(this.energyLimitReached) {
					rawStatus = Status.ENERGY_LIMIT_REACHED;
				}
				break;
			case "C":			//charging
			case "D":			//charging with ventilation
			case "A’":			//charging suspended, but we are still charging
				rawStatus = Status.CHARGING;
				if(this.energyLimitReached) {
					rawStatus = Status.ENERGY_LIMIT_REACHED;
				}
				break;
			case "E":
				rawStatus = Status.ERROR;
				break;
			default:
				break;
				//TOOD handle OpenEMS states
				//STARTING
				//NOT_READY_FOR_CHARGING
				//READY_FOR_CHARGING
				//ERROR
				//CHARGING_REJECTED
				//ENERGY_LIMIT_REACHED
				//CHARGING_FINISHED							
			}
		}
		this._setStatus(rawStatus);
	}
	

	public void setEnergyLimitReached(boolean energyLimitReached) {
		this.energyLimitReached = energyLimitReached;
	}

	
	private boolean evcsHasDisconnected() {
		try {
			@SuppressWarnings("unchecked")
			Value<Boolean> conn = (Value<Boolean>) this.channel(HardyBarth.ChannelId.RAW_CHARGE_CONTROL_CONNECTED).value();
			if(conn.get().booleanValue() != evcsConnected ) {
				evcsConnected = conn.get().booleanValue(); 
				this.logInfo(this.log, "Connection State has changed to " + evcsConnected);
				return true;
			}
		}catch(Exception e) {		}
		return false;
	}
	
	private boolean isCharging() {
		try {
			String cs = this.channel(HardyBarth.ChannelId.RAW_CHARGE_CONTROL_STATE).getNextValue().asString();
			if(cs != null && cs.compareTo("C")==0) {
				return true;
			}
		}catch(Exception e) {		}
		return false;
	}

	@Override
	public String debugLog() {
		String cs = this.channel(HardyBarth.ChannelId.RAW_CHARGE_CONTROL_STATE).getNextValue().asString();
		String chPoLimit = this.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT).getNextValue().asString();
		String conn = this.channel(HardyBarth.ChannelId.RAW_CHARGE_CONTROL_CONNECTED).getNextValue().asString();
		return "Limit:" + chPoLimit 
				+ "," + this.getStatus().getName() 
				+ "," + this.getState() 
				+ "," + cs 
				+ "," + this.getChargePower()
				+ ",Plugged:" + conn;
	}

	public void updateRawOutChargeCurrent(Integer current) {
		this.channel(HardyBarth.ChannelId.RAW_OUT_CHARGE_MAX_CURRENT).setNextValue(current);
	}

	public void updateRawOutMinCurrent(Integer current) {
		this.channel(HardyBarth.ChannelId.RAW_OUT_MIN_CURRENT).setNextValue(current);
	}

	public void updateTracepoint(Integer tracepointId) {
		this.channel(HardyBarth.ChannelId.TRACEPOINT_WRITE_HANDLER).setNextValue(tracepointId);
	}

	
	public void updateTraceCommandStartStop(boolean isStartCommand) {
		this.channel(HardyBarth.ChannelId.TRACE_COMMAND_START_STOP).setNextValue(isStartCommand);
	}

	private void updateComFailed() {
		this._setChargingstationCommunicationFailed(this.commErrorRead || this.commErrorWrite);
		if(this.getChargingstationCommunicationFailed().orElse(true)){
			this.getStateChannel().setNextValue(Level.FAULT);
		}else {
			this.getStateChannel().setNextValue(Level.OK);
		}
	}

	public void setCommunicationFailedRead(boolean error) {
		this.commErrorRead = error;
		this.updateComFailed();
	}
	

	public void setCommunicationFailedWrite(boolean error) {
		this.commErrorWrite = error;
		this.updateComFailed();
	}
}
 