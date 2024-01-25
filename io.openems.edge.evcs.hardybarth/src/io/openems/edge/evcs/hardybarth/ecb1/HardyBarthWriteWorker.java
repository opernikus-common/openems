package io.openems.edge.evcs.hardybarth.ecb1;

import java.util.Optional;

import org.slf4j.Logger;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.evcs.api.EvcsTimer;
import io.openems.edge.evcs.api.ManagedEvcs;

/**
 * Handles writes. Called in every cycle
 */
public class HardyBarthWriteWorker extends AbstractCycleWorker {
	private final HardyBarthImpl parent;
	private Integer chargeControlIndex;
	private int tracepoint;
	private int requestedPower = 0;
	private EvcsTimer startStopTimer;
	
	public HardyBarthWriteWorker(HardyBarthImpl parent) {
		super();
		this.parent = parent;
		this.log = parent.log;
		this.resetChannels();
		this.startStopTimer = new EvcsTimer(parent.config.commandStartStopDelay());
	}
	
	
	@Override
	protected void forever() throws OpenemsNamedException {
		this.tracepoint = 0;
		try {
			this.initHandler();
			this.requestManualMode();
			this.handlePowerRequest();
			this.parent.setCommunicationFailedWrite(false);
			this.tracepoint = 10;
			
		}catch(Exception e) {
			this.resetChannels();
			if(this.parent.config.debug() != 0) {
				this.log.error("Exception " + e.getMessage());
			}
		}
		this.parent.updateTracepoint(this.tracepoint);  //TODO remove before final check in
	}
	
	
	private void resetChannels() {
		this.tracepoint += 100;
		this.parent.setCommunicationFailedWrite(true);
		this.parent.updateRawOutChargeCurrent(null);
		this.parent.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT).setNextValue(null);
	}

	
	/**
	 * <ul>
	 * <li>checks if this charging station can be controlled by detecting chargeControlID
	 * <li> inits min hw current
	 * </ul>
	 */
	private void initHandler() throws OpenemsNamedException{
		if(this.chargeControlIndex != null) {
			return;
		}
		IntegerReadChannel channelChargeId = this.parent.channel(HardyBarth.ChannelId.RAW_CHARGE_CONTROL_ID);
		this.chargeControlIndex = channelChargeId.value().get();
		this.setMinCurrent(this.parent.config.minHwCurrent());
	}
	

	/**
	 * Set manual mode.
	 * 
	 * <p>
	 * Sets the chargemode to manual if not set.
	 */
	private void requestManualMode() throws OpenemsNamedException {
		Optional<String> val = this.parent._getChargeControlMode();
		if ( (!val.isPresent()) || 
				(!val.get().equals(Consts.ChargingMode.MANUAL.toString())) ) {
			this.sendCmdControlModeManual();
		}
		val = this._getPvMode();
		if ( (!val.isPresent()) || 
				(!val.get().equals(Consts.ChargingMode.MANUAL.toString())) ) {
			this.requestPvModeManual();
		}
		// else value is present AND equal to manual mode
	}

	private void requestPvModeManual() throws OpenemsNamedException {
		// All evcc of one evcs share the pvMode. Therefore there's no existent 
		// evcc procedure ("set evcc data xyz if not set"). There's no 
		// aggregation several evcc to one evcs either. So just request pvMode 
		// for the whole evcs,  but have this functionality separated, at least 
		this.sendCmdPvModeManual();
	}

	/**
	 * Sets the current from SET_CHARGE_POWER channel.
	 * 
	 * <p>
	 * Allowed loading current are between 6A and 32A. Invalid values are discarded.
	 * The value is also depending on the configured min and max current of the
	 * charging station.
	 */
	private void handlePowerRequest() throws OpenemsNamedException {

		int energyLimit = this.parent.getSetEnergyLimit().orElse(0);
		if (energyLimit == 0 || energyLimit > this.parent.getEnergySession().orElse(0)) {
			//energy limit not reached
			this.parent.setEnergyLimitReached(false);
			Optional<Integer> chargePowerLimit = this.parent.getSetChargePowerLimitChannel().getNextWriteValueAndReset();
			if ( chargePowerLimit.isPresent() ) {
				requestedPower = chargePowerLimit.get();
			}
			
		} else {
			//reached energy limit 
			this.parent.setEnergyLimitReached(true);
			requestedPower = 0;
		}
		//if requestedPower has not been updated, send the old requested power, thats why this variable is a class variable
		this.parent.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT).setNextValue(requestedPower);

		//convert to ampere and update charging station
		int phases = this.parent.getPhases().orElse(3);
		Integer crrnt = this.powerToCurrent(requestedPower, phases);
		this.requestCurrent(crrnt);
	}


	// Send charge power limit
	public void setMinCurrent(int current) throws OpenemsNamedException{
		this.sendCmdUpdateMinCurrent(current);
		this.parent.updateRawOutMinCurrent(current);
	}
	
	
	/**
	 * @param power in Watt
	 * @param phases
	 * @return current in Ampere
	 */
	private int powerToCurrent(int power, int phases) {
		if(phases == 0) {
			phases = 3;
		}

		// I = W/U, assume U=230V and there are several phases to consider, too
		int current = Long.valueOf(Math.round((power / 230.0) / phases)).intValue();

		// current is at most configured max value
		current = Math.min(this.parent.config.maxHwCurrent(), current);

		// ensure current is at least configured min value, 0 in 
		if (current < this.parent.config.minHwCurrent() ) {
			current = 0;
		}
		return current;
	}

	
	private void requestCurrent(int current) throws OpenemsNamedException {
		int phases = this.parent.getPhases().orElse(0);
		if ( current >= this.parent.config.minHwCurrent() ) {
			this.sendCmdUpdateCurrent(current);
			if(phases == 0) {
				//TODO maybe we should check parent.getStatus(); or RAW_CHARGE_CONTROL_CONNECTED also, before sending commands 
				this.sendCmdStart();
			}
		}else {
			if(phases > 0) {
				this.sendCmdUpdateCurrent(current);
				this.sendCmdStop();
			}
		}
	}
	
	
	private void sendCmdPvModeManual() throws OpenemsNamedException {
		this.tracepoint = 1;

		this.parent.api.sendPutRequest(
				Consts.Endpoint.PVMODE.toUri(),
				"pvmode", 
				Consts.ChargingMode.MANUAL.toString());

	}

	
	private void sendCmdControlModeManual() throws OpenemsNamedException {
		this.tracepoint = 2;
		this.parent.api.sendPutRequest(
				Consts.endpointChargeControlMode(this.chargeControlIndex),
				Consts.Key.MODE.toString(), 
				Consts.ChargingMode.MANUAL.toString());
	}
	
	
	// Send charge power limit
	private void sendCmdUpdateMinCurrent(int minCurrent) throws OpenemsNamedException {
		this.tracepoint = 3;
		this.parent.api.sendPutRequest(
				Consts.Endpoint.CHARGE_CONTROL.toUri() + 
				Consts.Endpoint.MIN_CURRENT.toUri(), 
				Consts.Key.MIN_CURRENT.toString(),
				Integer.valueOf(minCurrent).toString());
	}
	

	//adjust ampere
	private void sendCmdUpdateCurrent(int current) throws OpenemsNamedException {
		this.tracepoint = 4;
		this.parent.updateRawOutChargeCurrent(current);
		this.parent.api.sendPutRequest(
					Consts.Endpoint.CHARGE_CONTROLS.toUri() +
					"/" + chargeControlIndex +	
					Consts.Endpoint.MODE.toUri() + 
					Consts.Endpoint.MANUAL.toUri() + 
					Consts.Endpoint.AMPERE.toUri(), 
					Consts.Key.MANUAL_MODE_AMP.toString(),
					String.valueOf(current));
	}

	
	private void sendCmdStart() throws OpenemsNamedException {
		if(startStopTimer.hasExpired()) {
			this.tracepoint = 5;
			this.parent.updateTraceCommandStartStop(true);
			this.parent.api.sendPostRequest(
					Consts.Endpoint.CHARGE_CONTROLS.toUri() +
					"/" + String.valueOf(chargeControlIndex) +	
					Consts.Endpoint.START.toUri());
		}
	}

	
	private void sendCmdStop() throws OpenemsNamedException {
		if(startStopTimer.hasExpired()) {
			this.tracepoint = 6;
			this.parent.updateTraceCommandStartStop(false);
			this.parent.api.sendPostRequest(
					Consts.Endpoint.CHARGE_CONTROLS.toUri() +
					"/" + chargeControlIndex +	
					Consts.Endpoint.STOP.toUri());
		}
	}
	
	private Optional<String> _getPvMode() throws OpenemsNamedException {
		this.tracepoint = 7;

		JsonElement jel = this.parent.api.sendGetRequest("pvmode");
		try {
			if(jel.isJsonObject()) {
				String s = jel.getAsJsonObject().get("pvmode").getAsString();
				return Optional.of(s);
			}
		}catch(Exception e) {}
		return Optional.empty();
	}

	
	private final Logger log;
}
