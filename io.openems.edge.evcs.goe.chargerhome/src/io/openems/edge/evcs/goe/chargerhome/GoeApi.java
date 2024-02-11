package io.openems.edge.evcs.goe.chargerhome;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class GoeApi {
	private final String ipAddress;
	private final int executeEveryCycle;
	private int cycleCount;
	private JsonObject jsonStatusCache;
	private final EvcsGoeChargerHomeImpl parent;

	public GoeApi(EvcsGoeChargerHomeImpl p) {
		this.ipAddress = p.config.ip();
		this.jsonStatusCache = null;
		this.parent = p;
		this.executeEveryCycle = p.config.executeCycle();
		this.cycleCount = this.executeEveryCycle - 1;
	}

	/**
	 * Gets the status from go-e on every config.executeCycle(). See
	 * https://github.com/goecharger
	 *
	 * @return the json object
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getStatus() throws OpenemsNamedException {

		try {
			if (++this.cycleCount % this.executeEveryCycle == 0) {
				// Execute every x-Cycle
				var url = "http://" + this.ipAddress + "/status";
				var json = this.sendRequest(url, "GET");
				this.jsonStatusCache = json;
				return json;
			}
			return this.jsonStatusCache;

		} catch (Exception e) {
			this.parent.debugLog("get Status ex: " + e.getMessage());
			throw new OpenemsNamedException(OpenemsError.GENERIC, "get Status ex: " + e.getMessage());
		}
	}

	/**
	 * Sets the activation status for go-e.
	 *
	 * <p>
	 * See https://github.com/goecharger.
	 *
	 * @param active boolean if the charger should be set to active
	 */
	public void setActive(boolean active) throws OpenemsException {

		try {
			Integer status = 0;
			if (active) {
				status = 1;
			}
			var url = "http://" + this.ipAddress + "/mqtt?payload=alw=" + Integer.toString(status);
			this.putRequest(url, "setActive failed " + active);
		} catch (Exception e) {
			this.parent.debugLog("setActive ex: " + e.getMessage());
			throw new OpenemsException(e.getMessage());
		}
	}

	/**
	 * Sets the Current in Ampere for go-e See https://github.com/goecharger.
	 *
	 * @param current current in mA
	 */
	public void setCurrent(int current) throws OpenemsNamedException {

		Integer currentAmpere = current / 1000;
		// use non-persistent data point "amx" in order to preserve
		// precious flash memory
		var url = "http://" + this.ipAddress + "/mqtt?payload=amx=" + Integer.toString(currentAmpere);
		this.putRequest(url, "setCurrent failed " + current);
	}

	/**
	 * Limit MaxEnergy for go-e See https://github.com/goecharger.
	 *
	 * @param limit maximum energy limit enabled
	 */
	public void limitMaxEnergy(boolean limit) throws OpenemsNamedException {

		var stp = 0;
		if (limit) {
			stp = 2;
		}
		var url = "http://" + this.ipAddress + "/mqtt?payload=stp=" + Integer.toString(stp);
		this.putRequest(url, "limitMaxEnergy failed " + limit);
	}

	/**
	 * Sets the MaxEnergy in 0.1 kWh for go-e See https://github.com/goecharger.
	 *
	 * @param maxEnergy maximum allowed energy
	 */
	public void setMaxEnergy(int maxEnergy) throws OpenemsNamedException {

		if (maxEnergy > 0) {
			this.limitMaxEnergy(true);
		} else {
			this.limitMaxEnergy(false);
		}
		var url = "http://" + this.ipAddress + "/mqtt?payload=dwo=" + Integer.toString(maxEnergy);
		this.putRequest(url, "setMaxEnergy failed " + maxEnergy);
	}

	private void putRequest(String url, String exText) throws OpenemsNamedException {

		try {
			this.sendRequest(url, "PUT");

		} catch (Exception e) {
			this.parent.debugLog(exText + " x: " + e.getMessage());
			throw new OpenemsNamedException(OpenemsError.GENERIC, exText + " x: " + e.getMessage());
		}
	}

	/**
	 * Sends a get or set request to the go-e API.
	 *
	 *
	 * @param urlString     used URL
	 * @param requestMethod requested method
	 * @return a JsonObject or JsonArray
	 */
	private JsonObject sendRequest(String urlString, String requestMethod) throws OpenemsNamedException {
		try {
			this.parent.debugLog("Request " + urlString);
			var url = new URL(urlString);
			var con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod(requestMethod);
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			var status = con.getResponseCode();
			String body;
			try (var in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				// Read HTTP response
				var content = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					content.append(line);
					content.append(System.lineSeparator());
				}
				body = content.toString();
			}
			if (status < 300) {
				// Parse response to JSON
				return JsonUtils.parseToJsonObject(body);
			}
			throw new OpenemsException("Error while reading from go-e API. Response code: " + status + ". " + body);
		} catch (OpenemsNamedException | IOException e) {
			throw new OpenemsException(
					"Unable to read from go-e API. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

}
