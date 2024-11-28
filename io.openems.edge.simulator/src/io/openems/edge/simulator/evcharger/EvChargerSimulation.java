package io.openems.edge.simulator.evcharger;

import io.openems.edge.common.filter.Pt1filter;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcharger.api.EvCharger;
import io.openems.edge.evcharger.api.data.Iec62196Status;

public class EvChargerSimulation {
	private DataSourceEventType eventType;
	private Integer vehiclePhases;
	private Integer vehicleMaxCurrent;
	private Pt1filter pt1FilterCurrentL1;
	private Pt1filter pt1FilterCurrentL2;
	private Pt1filter pt1FilterCurrentL3;
	private int rawCurrentL1 = 0;
	private int rawCurrentL2 = 0;
	private int rawCurrentL3 = 0;
	private boolean evChargerHasPhaseLimitationAvailable = false;
	private boolean chargCommFailed = false;
	private Iec62196Status status = Iec62196Status.NO_VEHICLE;

	public EvChargerSimulation() {
		this.reconfigure(1, 1, false);
	}

	/**
	 * Reconfigures the Simulation reaction delay.
	 * 
	 * @param cycleTime                          the core cycle time in ms.
	 * @param responsiveness                     the responsiveness in ms.
	 * @param chargerHasPhaseLimitationAvailable true if this charge can limit the
	 *                                           number of used phases, false else.
	 */
	public void reconfigure(int cycleTime, int responsiveness, boolean chargerHasPhaseLimitationAvailable) {
		this.eventType = DataSourceEventType.NO_VEHICLE;
		this.vehiclePhases = 3;
		this.vehicleMaxCurrent = 6;
		this.evChargerHasPhaseLimitationAvailable = chargerHasPhaseLimitationAvailable;

		this.pt1FilterCurrentL1 = new Pt1filter(responsiveness, cycleTime);
		this.pt1FilterCurrentL2 = new Pt1filter(responsiveness, cycleTime);
		this.pt1FilterCurrentL3 = new Pt1filter(responsiveness, cycleTime);
	}

	/**
	 * Update channel information based on new incoming events.
	 * 
	 * @param eventType         type of the event (see EvChargerEventType).
	 * @param vehiclePhases     number of used phases of the connected vehicle.
	 * @param vehicleMaxCurrent max current ability of the connected vehicle.
	 */
	public void update(DataSourceEventType eventType, Integer vehiclePhases, Integer vehicleMaxCurrent) {
		if (eventType == this.eventType && vehiclePhases == this.vehiclePhases
				&& vehicleMaxCurrent == this.vehicleMaxCurrent) {
			return;
		}
		// apply new vehicle situation
		this.eventType = eventType;
		this.vehiclePhases = vehiclePhases;
		this.vehicleMaxCurrent = vehicleMaxCurrent;
	}

	/**
	 * Applies the controllers request for current to this simulation element.
	 * 
	 * @param current            the current in mA.
	 * @param chargerPhasesLimit the expected number of phases used by this
	 *                           simulation
	 */
	public void applyCurrent(int current, int chargerPhasesLimit) {

		if (!this.evChargerHasPhaseLimitationAvailable) {
			chargerPhasesLimit = 3;
		}
		current = TypeUtils.min(current, this.vehicleMaxCurrent);
		if (this.eventType != DataSourceEventType.CHARGING) {
			current = 0;
		}
		var newCurrentL1 = this.pt1FilterCurrentL1.applyPt1Filter(current);
		var newCurrentL2 = 0;
		if (chargerPhasesLimit > 1 && this.vehiclePhases > 1) {
			newCurrentL2 = this.pt1FilterCurrentL2.applyPt1Filter(current);
		}
		var newCurrentL3 = 0;
		if (chargerPhasesLimit > 2 && this.vehiclePhases > 2) {
			newCurrentL3 = this.pt1FilterCurrentL3.applyPt1Filter(current);
		}
		var commFailed = false;
		switch (this.eventType) {
		case NO_VEHICLE -> {
			if (newCurrentL1 < EvCharger.DEFAULT_MIN_CURRENT) {
				this.status = Iec62196Status.NO_VEHICLE;
				newCurrentL1 = 0;
				newCurrentL2 = 0;
				newCurrentL3 = 0;
			}
		}
		case CHARGING -> {
			if (newCurrentL1 >= EvCharger.DEFAULT_MIN_CURRENT) {
				this.status = Iec62196Status.CHARGING;
			}
		}
		case ERROR -> {
			if (newCurrentL1 < EvCharger.DEFAULT_MIN_CURRENT) {
				this.status = Iec62196Status.ERROR;
				newCurrentL1 = 0;
				newCurrentL2 = 0;
				newCurrentL3 = 0;
			}
		}
		case COM_ERROR -> {
			commFailed = true;
		}
		default -> {
			this.status = Iec62196Status.ERROR;
		}
		}
		this.rawCurrentL1 = newCurrentL1;
		this.rawCurrentL2 = newCurrentL2;
		this.rawCurrentL3 = newCurrentL3;
		this.chargCommFailed = commFailed;
	}

	public Iec62196Status getRawStatus() {
		return this.status;
	}

	public int getRawCurrentL1() {
		return this.rawCurrentL1;
	}

	public int getRawCurrentL2() {
		return this.rawCurrentL2;
	}

	public int getRawCurrentL3() {
		return this.rawCurrentL3;
	}

	public boolean getChargingCommunicationFailed() {
		return this.chargCommFailed;
	}

	/**
	 * Gets the Charge Power computed by the three currents L1-L3.
	 * 
	 * @return the charge Power in W.
	 */
	public Integer getRawChargePower() {
		var cur = this.rawCurrentL1 + this.rawCurrentL2 + this.rawCurrentL3;
		return (cur * 230) / 1000;
	}

}
