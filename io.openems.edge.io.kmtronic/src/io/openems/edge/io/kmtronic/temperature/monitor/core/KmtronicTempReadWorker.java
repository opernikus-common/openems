package io.openems.edge.io.kmtronic.temperature.monitor.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractCycleWorker;

public class KmtronicTempReadWorker extends AbstractCycleWorker {

    private final KmtronicTempCoreImpl parent;

    public KmtronicTempReadWorker(KmtronicTempCoreImpl parent) {
	this.parent = parent;
    }

    @Override
    public void activate(String name) {
	super.activate(name);
    }

    @Override
    public void deactivate() {
	super.deactivate();
    }

    @Override
    protected void forever() throws InterruptedException {

	var rawData = new DataRawTemp();
	try {
	    rawData = this.sendRequest("http://" + this.parent.getIpAdress() + "/status.xml", "GET");
	} catch (OpenemsNamedException e) {
	    this.parent.getStateChannel().setNextValue(Level.FAULT);
	    e.printStackTrace();
	}
	for (var i = 0; i < this.parent.numberOfSensors; i++) {
	    this.parent.channel("IdSensor" + i).setNextValue(rawData.getId(i));
	    this.parent.channel("NameSensor" + i).setNextValue(rawData.getName(i));
	    this.parent.channel("TempSensor" + i).setNextValue(rawData.getTemp(i));
	}
	this.parent.getStateChannel().setNextValue(Level.OK);
    }

    /**
     * Sends a get or set request to the temperature monitor.
     *
     *
     * @param urlString     used URL
     * @param requestMethod requested method
     * @return a JsonObject or JsonArray
     */
    private DataRawTemp sendRequest(String urlString, String requestMethod) throws OpenemsNamedException {
	try {
	    // this.parent.debugLog("Request " + urlString);
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
		return UtilsXmlParser.parseData(body);
	    }
	    throw new OpenemsException("Error while reading from XML. Response code: " + status + ". " + body);
	} catch (OpenemsNamedException | IOException e) {
	    throw new OpenemsException(
		    "Unable to read from XML. " + e.getClass().getSimpleName() + ": " + e.getMessage());
	}
    }

}
