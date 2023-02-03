package io.openems.edge.heater.chp.dachs;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.heater.api.HeaterState;

/** methods to access the dachs and prepare/parse in/outgoing data. */
public class DachsDevice {

	private static final int NORMAL_OPERATION_RPM_THRESHOLD = 1800;

	private DachsGltImpl parent;
	private HttpTools httptools;

	private boolean parseReadResultWarning = false;

	protected DachsDevice(DachsGltImpl parent) {
		this.parent = parent;
		this.httptools = new HttpTools(this.parent.getConfig().url(), this.parent.getConfig().username(),
				this.parent.getConfig().password());
	}

	protected void readValuesAndMapToChannel() throws OpenemsException {
		// Important Note: we can -NOT- request all values in one request, because DACHS
		// in "nachtabsenkung" will fail on this request (even if "nachtabsenkung" is
		// disabled again)

		this.parseReadResultWarning = false;
		for (var channelId : DachsGlt.ChannelId.values()) {
			if (channelId.getKey() != null) {
				var key = channelId.getKey();
				String valueKey = this.httptools.readKey(key);

				switch (channelId.doc().getType()) {
				case BOOLEAN:
					this.parseAndSetBooleanChannel(channelId, valueKey);
					break;
				case INTEGER:
					this.parseAndSetIntegerChannel(channelId, valueKey);
					break;
				case DOUBLE:
					this.parseAndSetDoubleChannel(channelId, valueKey);
					break;
				case STRING:
					this.parseAndSetStringChannel(channelId, valueKey);
					break;
				default:
					return;
				}

				if (this.parent.getConfig().verbose()) {
					this.parent.logInfo("Request: " + key);
					this.parent.logInfo("Response: " + valueKey);
				}
				try {
					Thread.sleep(60);
				} catch (InterruptedException e) {
					;
				}
			}
		}

		this.parent.channel(DachsGlt.ChannelId.WARNING).setNextValue(this.parent.getStateWarning().getValue() > 0);
		this.parent.channel(DachsGlt.ChannelId.ERROR).setNextValue(this.parent.getStateError().getValue() > 0);

		this.updateGeneralState();

		this.parent._setReadResultWarning(this.parseReadResultWarning);
	}

	private void parseAndSetBooleanChannel(DachsGlt.ChannelId channelId, String response) {
		try {
			String res = this.extractValue(response);
			Boolean value = Boolean.parseBoolean(res);
			this.parent.channel(channelId).setNextValue(value);
		} catch (Exception e) {
			this.parent.channel(channelId).setNextValue(null);
			this.parseReadResultWarning = true;
		}
	}

	private void parseAndSetIntegerChannel(DachsGlt.ChannelId channelId, String response) {
		try {
			String res = this.extractValue(response);
			Integer value = Integer.parseInt(res);
			this.parent.channel(channelId).setNextValue(channelId.applyConverter(value));
		} catch (Exception e) {
			this.parent.channel(channelId).setNextValue(null);
			this.parseReadResultWarning = true;
		}
	}

	private void parseAndSetDoubleChannel(DachsGlt.ChannelId channelId, String response) {
		try {
			String res = this.extractValue(response);
			Double value = Double.parseDouble(res);
			this.parent.channel(channelId).setNextValue(channelId.applyConverter(value));
		} catch (Exception e) {
			this.parent.channel(channelId).setNextValue(null);
			this.parseReadResultWarning = true;
		}
	}

	private void parseAndSetStringChannel(DachsGlt.ChannelId channelId, String response) {
		try {
			String res = this.extractValue(response);
			this.parent.channel(channelId).setNextValue(res);
		} catch (Exception e) {
			this.parent.channel(channelId).setNextValue(null);
			this.parseReadResultWarning = true;
		}
	}

	private String extractValue(String response) {
		String res = response.replaceAll(".*\\=", "").trim();
		return res.substring(0, res.indexOf("/n"));
	}

	private void updateGeneralState() {
		if (this.parseReadResultWarning) {
			this.parent._setHeaterState(HeaterState.UNDEFINED);
		} else if (this.parent.getRpm().orElse(0) > NORMAL_OPERATION_RPM_THRESHOLD) {
			this.parent._setHeaterState(HeaterState.RUNNING);
		} else if (this.parent.getHeatRunRequested().orElse(0) > 0) {
			this.parent._setHeaterState(HeaterState.STARTING_UP_OR_PREHEAT);
		} else if (this.parent.getRunClearance().orElse(0) > 0) {
			this.parent._setHeaterState(HeaterState.STANDBY);
		} else {
			this.parent._setHeaterState(HeaterState.BLOCKED_OR_ERROR);
		}
	}

	/**
	 * Checks if the heater is available.
	 * 
	 * @return true, if the heater can be or is activated.
	 */
	protected boolean available() {
		HeaterState hs = this.parent.getHeaterState();
		return (hs == HeaterState.RUNNING || hs == HeaterState.STANDBY || hs == HeaterState.STARTING_UP_OR_PREHEAT);
	}

	protected void activateDachs() throws OpenemsException {
		String result = this.httptools
				.writeKeys("Stromf_Ew.Anforderung_GLT.bAktiv=1&Stromf_Ew.Anforderung_GLT.bAnzahlModule=1");

		if (this.parent.getConfig().verbose()) {
			this.parent.logInfo(result);
		}
		// TODO result auswerten: use HttpTools.extractValueFromMessage
	}

	protected void deactivateDachs() throws OpenemsException {
		String result = this.httptools.writeKeys("Stromf_Ew.Anforderung_GLT.bAktiv=0");

		if (this.parent.getConfig().verbose()) {
			this.parent.logInfo(result);
		}
		// TODO result auswerten: use HttpTools.extractValueFromMessage
	}

}
