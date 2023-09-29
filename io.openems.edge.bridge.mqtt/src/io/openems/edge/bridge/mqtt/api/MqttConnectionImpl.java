package io.openems.edge.bridge.mqtt.api;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import io.openems.edge.bridge.mqtt.api.worker.MqttWorker;

public class MqttConnectionImpl implements MqttCallbackExtended {

	private static final int BASIC_KEEP_ALIVE_INTERVAL = 300;
	protected final MqttWorker parent;
	private final MemoryPersistence persistence = new MemoryPersistence();
	private final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
	private MqttClient mqttClient;

	public MqttConnectionImpl(MqttWorker parent) {
		this.parent = parent;
	}

	/**
	 * Creates the MQTT Connection w.o. connecting.
	 * 
	 * @param url          the Broker Url
	 * @param clientId     the clientId, usually autogenerated.
	 * @param user         the username
	 * @param password     the password
	 * @param userRequired is a UserRequired
	 * @param version      the MqttVersion usually
	 *                     {@link MqttConnectOptions#MQTT_VERSION_3_1_1}.
	 * @throws MqttException on MQTT Error.
	 */
	public void createMqttConnection(String url, String clientId, String user, String password, boolean userRequired,
			int version) throws MqttException {
		this.mqttClient = new MqttClient(url, clientId, this.persistence);
		if (userRequired) {
			this.mqttConnectOptions.setUserName(user);
			this.mqttConnectOptions.setPassword(password.toCharArray());
		}
		this.mqttConnectOptions.setCleanSession(false);
		this.mqttConnectOptions.setKeepAliveInterval(BASIC_KEEP_ALIVE_INTERVAL);
		this.mqttConnectOptions.setConnectionTimeout(10);
		this.mqttConnectOptions.setMqttVersion(version);
		//this.mqttConnectOptions.setHttpsHostnameVerificationEnabled(false);
		//this.mqttConnectOptions.setSocketFactory(SSLSocketFactory.getDefault());
		this.mqttClient.setCallback(this);
		this.mqttConnectOptions.setAutomaticReconnect(true);
	}

	/**
	 * Connects the mqttClient to the Broker.
	 * 
	 * @throws MqttException on Connection issues
	 *                       {@link MqttException#getReasonCode()}.
	 */
	public void connect() throws MqttException {
		this.mqttClient.connect(this.mqttConnectOptions);
	}

	/**
	 * Disconnects the Connection. Happens on deactivation. Only for internal usage.
	 *
	 * @throws MqttException if something is wrong with the MQTT Connection.
	 */
	public void disconnect() throws MqttException {
		this.mqttClient.disconnect();
	}

	@Override
	public void connectComplete(boolean b, String s) {
		this.parent.notifySuccessConnection();
	}

	@Override
	public void connectionLost(Throwable throwable) {
		this.parent.connectionLost(throwable);
	}

	@Override
	public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
		this.parent.updateTopicPayload(topic, new String(mqttMessage.getPayload(), StandardCharsets.UTF_8));
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
		// TODO maybe
	}

	/**
	 * Checks if the Connection is still available.
	 *
	 * @return a Boolean.
	 */
	public boolean isConnected() {
		return this.mqttClient.isConnected();
	}

	/**
	 * Unsubscribes the MQTT client from a topic.
	 * 
	 * @param topic the topicname (e.g. telemetry/myTopic)
	 * @throws MqttException on e.g. connectionError, already unsubscribed
	 */
	public void unsubscribeFromTopic(String topic) throws MqttException {
		this.mqttClient.unsubscribe(topic);
	}

	/**
	 * Subscribes to the given Topic via the {@link #mqttClient}.
	 * 
	 * @param topic the topicName
	 * @throws MqttException on ConnectionError e.g.
	 *                       {@link MqttException#REASON_CODE_SUBSCRIBE_FAILED}
	 */
	public void subscribeToTopic(String topic) throws MqttException {
		this.mqttClient.subscribe(topic);
	}

	/**
	 * Publishes an MqttMessage with the given {@link Topic}.
	 * 
	 * @param topic the Topic Object containing the TopicName and Payload
	 * @throws MqttException on Mqtt Failure e.g.
	 *                       {@link MqttException#REASON_CODE_BROKER_UNAVAILABLE}.
	 */
	public void publish(Topic topic) throws MqttException {
		MqttMessage publishMessage;
		publishMessage = new MqttMessage(topic.getPayload().getPayloadMessage().getBytes(StandardCharsets.UTF_8));
		publishMessage.setQos(topic.getQos());
		publishMessage.setRetained(true);
		this.mqttClient.publish(topic.getTopicName(), publishMessage);

	}
}