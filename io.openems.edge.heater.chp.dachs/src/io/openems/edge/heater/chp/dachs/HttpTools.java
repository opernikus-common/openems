package io.openems.edge.heater.chp.dachs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

import io.openems.common.exceptions.OpenemsException;

public class HttpTools {

	private final String url;
	private final String basicAuth;

	public HttpTools(String url, String user, String pwd) {
		this.url = url;
		this.basicAuth = this.getBasicAuth(user, pwd);
	}

	private String getBasicAuth(String user, String pwd) {
		String gltpass = user + ":" + pwd;
		return "Basic " + new String(Base64.getEncoder().encode(gltpass.getBytes()));
	}

	/**
	 * Send read request to server.
	 *
	 * @param key the request string.
	 * @return the answer string.
	 */
	public String readKey(String key) throws OpenemsException {
		try {
			URL url = new URL(this.url + "/getKey?k=" + key);
			StringBuilder buf = new StringBuilder();
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("Authorization", this.basicAuth);
			con.setRequestMethod("GET");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			var status = con.getResponseCode();

			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				buf.append(line).append("/n");
			}
			reader.close();
			if (status < HttpURLConnection.HTTP_MULT_CHOICE) {
				return buf.toString();
			}
			throw new OpenemsException("Invalid HTTP Status Code " + status);

		} catch (Exception e) {
			throw new OpenemsException("Dachs read: Invalid Server Response for " + key + " ex: " + e.getMessage());
		}

	}

	/**
	 * Send write request to server.
	 *
	 * @param key the request string.
	 * @return the answer string.
	 */
	public String writeKeys(String key) throws OpenemsException {
		try {
			URL url = new URL(this.url + "/setKeys");
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("Authorization", this.basicAuth);
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			con.setRequestMethod("POST");
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setUseCaches(false);
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			con.setRequestProperty("Content-Length", String.valueOf(key.length()));

			OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
			writer.write(key);
			writer.flush();
			writer.close();

			StringBuilder buf = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				buf.append(line).append("\n");
			}
			reader.close();
			var status = con.getResponseCode();
			if (status <  HttpURLConnection.HTTP_MULT_CHOICE) {
				return buf.toString();
			}
			throw new OpenemsException("Invalid HTTP Status Code " + status);

		} catch (Exception e) {
			throw new OpenemsException("Dachs write: Invalid Server Response for " + key + " ex: " + e.getMessage());
		}
	}
}
