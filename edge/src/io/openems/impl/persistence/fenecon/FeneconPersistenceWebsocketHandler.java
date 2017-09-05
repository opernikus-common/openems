/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.persistence.fenecon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.thing.Thing;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.core.Config;
import io.openems.core.ConfigFormat;
import io.openems.core.ThingRepository;

/**
 * Handles a Websocket connection to a browser, femsserver,...
 *
 * @author stefan.feilmeier
 */
public class FeneconPersistenceWebsocketHandler {

	private Logger log = LoggerFactory.getLogger(FeneconPersistenceWebsocketHandler.class);

	/**
	 * Executor for subscriptions task
	 */
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	/**
	 * Holds subscribers to current data
	 */
	private final HashMap<String, CurrentDataWorker> currentDataSubscribers = new HashMap<>();

	/**
	 * Holds the websocket connection
	 */
	protected final WebSocket websocket;

	/**
	 * Holds subscribers to system log
	 */
	private final Set<String> logSubscribers = new HashSet<>();

	/**
	 * Executor for system log task
	 */
	private final ExecutorService logExecutor = Executors.newCachedThreadPool();

	public FeneconPersistenceWebsocketHandler(WebSocket websocket) {
		this.websocket = websocket;
	}

	/**
	 * OnOpen event of websocket.
	 */
	public void onOpen() {
		// on open: send current status of all channels
		sendCurrentStatusOfAllChannels();
	}

	/**
	 * Message event of websocket. Handles a new message.
	 */
	public void onMessage(JsonObject jMessage) {
		log.info(jMessage.toString());

		// get message id -> used for reply
		Optional<JsonArray> jIdOpt = JsonUtils.getAsOptionalJsonArray(jMessage, "id");

		// prepare reply (every reply is going to be merged into this object with this unique message id)
		Optional<JsonObject> jReplyOpt = Optional.empty();

		/*
		 * Config
		 */
		Optional<JsonObject> jConfigOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "config");
		if (jConfigOpt.isPresent()) {
			jReplyOpt = JsonUtils.merge(jReplyOpt, //
					config(jConfigOpt.get()) //
			);
		}

		/*
		 * Subscribe to currentData
		 */
		Optional<JsonObject> jCurrentDataOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "currentData");
		if (jCurrentDataOpt.isPresent() && jIdOpt.isPresent()) {
			jReplyOpt = JsonUtils.merge(jReplyOpt, //
					currentData(jIdOpt.get(), jCurrentDataOpt.get()) //
			);
		}

		/*
		 * Subscribe to log
		 */
		Optional<JsonObject> jLogOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "log");
		if (jLogOpt.isPresent() && jIdOpt.isPresent()) {
			jReplyOpt = JsonUtils.merge(jReplyOpt, //
					log(jIdOpt.get(), jLogOpt.get()) //
			);
		}

		// send reply
		if (jReplyOpt.isPresent()) {
			JsonObject jReply = jReplyOpt.get();
			if (jIdOpt.isPresent()) {
				jReply.add("id", jIdOpt.get());
			}
			WebSocketUtils.send(this.websocket, jReply);
		}
	}

	/**
	 * Handle "config" messages
	 *
	 * @param jConfig
	 * @return
	 */
	private synchronized Optional<JsonObject> config(JsonObject jConfig) {
		try {
			String mode = JsonUtils.getAsString(jConfig, "mode");

			if (mode.equals("query")) {
				/*
				 * Query current config
				 */
				String language = JsonUtils.getAsString(jConfig, "language");
				JsonObject jReplyConfig = Config.getInstance().getJson(ConfigFormat.OPENEMS_UI, language);
				return Optional.of(DefaultMessages.configQueryReply(jReplyConfig));
			}
		} catch (OpenemsException e) {
			log.warn(e.getMessage());
		}
		return Optional.empty();
	}

	/**
	 * Handle current data subscriptions
	 *
	 * @param j
	 */
	private synchronized Optional<JsonObject> currentData(JsonArray jId, JsonObject jCurrentData) {
		try {
			String mode = JsonUtils.getAsString(jCurrentData, "mode");

			if (mode.equals("subscribe")) {
				/*
				 * Subscribe to channels
				 */
				String messageId = jId.get(1).getAsString();

				// remove old worker if existed
				CurrentDataWorker worker = this.currentDataSubscribers.remove(messageId);
				if (worker != null) {
					worker.dispose();
				}
				// parse subscribed channels
				HashMultimap<String, String> channels = HashMultimap.create();
				JsonObject jSubscribeChannels = JsonUtils.getAsJsonObject(jCurrentData, "channels");
				for (Entry<String, JsonElement> entry : jSubscribeChannels.entrySet()) {
					String thing = entry.getKey();
					JsonArray jChannels = JsonUtils.getAsJsonArray(entry.getValue());
					for (JsonElement jChannel : jChannels) {
						String channel = JsonUtils.getAsString(jChannel);
						channels.put(thing, channel);
					}
				}
				if (!channels.isEmpty()) {
					// create new worker
					worker = new CurrentDataWorker(jId, channels, this.executor, this.websocket);
					this.currentDataSubscribers.put(messageId, worker);
				}
			}
		} catch (OpenemsException e) {
			log.warn(e.getMessage());
		}
		return Optional.empty();
	}

	/**
	 * Handle system log subscriptions
	 *
	 * @param j
	 */
	private synchronized Optional<JsonObject> log(JsonArray jId, JsonObject jLog) {
		try {
			String mode = JsonUtils.getAsString(jLog, "mode");
			String messageId = jId.get(1).getAsString();

			if (mode.equals("subscribe")) {
				/*
				 * Subscribe to system log
				 */
				this.logSubscribers.add(messageId);
			} else if (mode.equals("unsubscribe")) {
				/*
				 * Unsubscribe from system log
				 */
				this.logSubscribers.remove(messageId);
			}
		} catch (OpenemsException e) {
			log.warn(e.getMessage());
		}
		return Optional.empty();
	}

	// TODO handle config command
	// /**
	// * Set configuration
	// *
	// * @param j
	// */
	// private synchronized void configure(JsonElement jConfigsElement) {
	// try {
	// JsonArray jConfigs = JsonUtils.getAsJsonArray(jConfigsElement);
	// ThingRepository thingRepository = ThingRepository.getInstance();
	// for (JsonElement jConfigElement : jConfigs) {
	// JsonObject jConfig = JsonUtils.getAsJsonObject(jConfigElement);
	// String mode = JsonUtils.getAsString(jConfig, "mode");
	// if (mode.equals("update")) {
	// /*
	// * Channel Set mode
	// */
	// String thingId = JsonUtils.getAsString(jConfig, "thing");
	// String channelId = JsonUtils.getAsString(jConfig, "channel");
	// JsonElement jValue = JsonUtils.getSubElement(jConfig, "value");
	// Optional<Channel> channelOptional = thingRepository.getChannel(thingId, channelId);
	// if (channelOptional.isPresent()) {
	// Channel channel = channelOptional.get();
	// if (channel instanceof ConfigChannel<?>) {
	// /*
	// * ConfigChannel
	// */
	// ConfigChannel<?> configChannel = (ConfigChannel<?>) channel;
	// configChannel.updateValue(jValue, true);
	// Notification.send(NotificationType.SUCCESS,
	// "Successfully updated [" + channel.address() + "] to [" + jValue + "]");
	//
	// } else if (channel instanceof WriteChannel<?>) {
	// /*
	// * WriteChannel
	// */
	// WriteChannel<?> writeChannel = (WriteChannel<?>) channel;
	// writeChannel.pushWrite(jValue);
	// Notification.send(NotificationType.SUCCESS,
	// "Successfully set [" + channel.address() + "] to [" + jValue + "]");
	// }
	// } else {
	// throw new ConfigException("Unable to find " + jConfig.toString());
	// }
	// } else if (mode.equals("create")) {
	// /*
	// * Create new Thing
	// */
	// JsonObject jObject = JsonUtils.getAsJsonObject(jConfig, "object");
	// String parentId = JsonUtils.getAsString(jConfig, "parent");
	// String thingId = JsonUtils.getAsString(jObject, "id");
	// if (thingId.startsWith("_")) {
	// throw new ConfigException("IDs starting with underscore are reserved for internal use.");
	// }
	// if (thingRepository.getThingById(thingId).isPresent()) {
	// throw new ConfigException("Thing Id is already existing.");
	// }
	// String clazzName = JsonUtils.getAsString(jObject, "class");
	// Class<?> clazz = Class.forName(clazzName);
	// if (Device.class.isAssignableFrom(clazz)) {
	// // Device
	// Thing parentThing = thingRepository.getThing(parentId);
	// if (parentThing instanceof Bridge) {
	// Bridge parentBridge = (Bridge) parentThing;
	// Device device = thingRepository.createDevice(jObject);
	// parentBridge.addDevice(device);
	// Config.getInstance().writeConfigFile();
	// Notification.send(NotificationType.SUCCESS, "Device [" + device.id() + "] wurde erstellt.");
	// break;
	// }
	// }
	// } else if (mode.equals("delete")) {
	// /*
	// * Delete a Thing
	// */
	// String thingId = JsonUtils.getAsString(jConfig, "thing");
	// thingRepository.removeThing(thingId);
	// Config.getInstance().writeConfigFile();
	// Notification.send(NotificationType.SUCCESS, "Controller [" + thingId + "] wurde " + " gel�scht.");
	// } else {
	// throw new OpenemsException("Modus [" + mode + "] ist nicht implementiert.");
	// }
	// }
	// // Send new config
	// JsonObject jMetadata = new JsonObject();
	// // TODO jMetadata.add("config", Config.getInstance().getMetaConfigJson());
	// JsonObject j = new JsonObject();
	// j.add("metadata", jMetadata);
	// WebSocketUtils.send(this.websocket, j);
	// } catch (OpenemsException | ClassNotFoundException e) {
	// Notification.send(NotificationType.ERROR, e.getMessage());
	// }
	// }

	// TODO handle system command
	// /**
	// * System command
	// *
	// * @param j
	// */
	// private synchronized void system(JsonElement jSystemElement) {
	// JsonObject jNotification = new JsonObject();
	// try {
	// JsonObject jSystem = JsonUtils.getAsJsonObject(jSystemElement);
	// String mode = JsonUtils.getAsString(jSystem, "mode");
	// if (mode.equals("systemd-restart")) {
	// /*
	// * Restart systemd service
	// */
	// String service = JsonUtils.getAsString(jSystem, "service");
	// if (service.equals("fems-pagekite")) {
	// ProcessBuilder builder = new ProcessBuilder("/bin/systemctl", "restart", "fems-pagekite");
	// Process p = builder.start();
	// if (p.waitFor() == 0) {
	// log.info("Successfully restarted fems-pagekite");
	// } else {
	// throw new OpenemsException("restart fems-pagekite failed");
	// }
	// } else {
	// throw new OpenemsException("Unknown systemd-restart service: " + jSystemElement.toString());
	// }
	//
	// } else if (mode.equals("manualpq")) {
	// /*
	// * Manual PQ settings
	// */
	// String ess = JsonUtils.getAsString(jSystem, "ess");
	// Boolean active = JsonUtils.getAsBoolean(jSystem, "active");
	// if (active) {
	// Long p = JsonUtils.getAsLong(jSystem, "p");
	// Long q = JsonUtils.getAsLong(jSystem, "q");
	// if (this.controller == null) {
	// throw new OpenemsException("Local access only. Controller is null.");
	// }
	// this.controller.setManualPQ(ess, p, q);
	// Notification.send(NotificationType.SUCCESS,
	// "Leistungsvorgabe gesetzt: ess[" + ess + "], p[" + p + "], q[" + q + "]");
	// } else {
	// this.controller.resetManualPQ(ess);
	// Notification.send(NotificationType.SUCCESS, "Leistungsvorgabe gestoppt: ess[" + ess + "]");
	// }
	// } else {
	// throw new OpenemsException("Unknown system message: " + jSystemElement.toString());
	// }
	// } catch (OpenemsException | IOException | InterruptedException e) {
	// Notification.send(NotificationType.ERROR, e.getMessage());
	// }
	// }

	// TODO handle manual PQ
	// private void manualPQ(JsonElement j, AuthenticatedWebsocketHandler handler) {
	// try {
	// JsonObject jPQ = JsonUtils.getAsJsonObject(j);
	// if (jPQ.has("p") && jPQ.has("q")) {
	// long p = JsonUtils.getAsLong(jPQ, "p");
	// long q = JsonUtils.getAsLong(jPQ, "q");
	// this.controller.setManualPQ(p, q);
	// handler.sendNotification(NotificationType.SUCCESS, "Leistungsvorgabe gesetzt: P=" + p + ",Q=" + q);
	// } else {
	// // stop manual PQ
	// this.controller.resetManualPQ();
	// handler.sendNotification(NotificationType.SUCCESS, "Leistungsvorgabe zurückgesetzt");
	// }
	// } catch (ReflectionException e) {
	// handler.sendNotification(NotificationType.SUCCESS, "Leistungsvorgabewerte falsch: " + e.getMessage());
	// }
	// }

	// TODO handle channel commands
	// private void channel(JsonElement jChannelElement, AuthenticatedWebsocketHandler handler) {
	// try {
	// JsonObject jChannel = JsonUtils.getAsJsonObject(jChannelElement);
	// String thingId = JsonUtils.getAsString(jChannel, "thing");
	// String channelId = JsonUtils.getAsString(jChannel, "channel");
	// JsonElement jValue = JsonUtils.getSubElement(jChannel, "value");
	//
	// // get channel
	// Channel channel;
	// Optional<Channel> channelOptional = thingRepository.getChannel(thingId, channelId);
	// if (channelOptional.isPresent()) {
	// // get channel value
	// channel = channelOptional.get();
	// } else {
	// // Channel not found
	// throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
	// }
	//
	// // check for writable channel
	// if (!(channel instanceof WriteChannel<?>)) {
	// throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
	// }
	//
	// // set channel value
	// if (channel instanceof ConfigChannel<?>) {
	// // is a ConfigChannel
	// ConfigChannel<?> configChannel = (ConfigChannel<?>) channel;
	// try {
	// configChannel.updateValue(jValue, true);
	// log.info("Updated Channel [" + channel.address() + "] to value [" + jValue.toString() + "].");
	// handler.sendNotification(NotificationType.SUCCESS,
	// "Channel [" + channel.address() + "] aktualisiert zu [" + jValue.toString() + "].");
	// } catch (NotImplementedException e) {
	// throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Conversion not implemented");
	// }
	// } else {
	// // is a WriteChannel
	// handler.sendNotification(NotificationType.WARNING, "WriteChannel nicht implementiert");
	// }
	// } catch (ReflectionException e) {
	// handler.sendNotification(NotificationType.SUCCESS, "Leistungsvorgabewerte falsch: " + e.getMessage());
	// }
	// }

	/**
	 * Send a notification message/error to the websocket
	 *
	 * @param mesage
	 * @return true if successful, otherwise false
	 */
	// TODO send notification
	// public synchronized void sendNotification(NotificationType type, String message) {
	// JsonObject jNotification = new JsonObject();
	// jNotification.addProperty("type", type.name().toLowerCase());
	// jNotification.addProperty("message", message);
	// JsonObject j = new JsonObject();
	// j.add("notification", jNotification);
	// new Thread(() -> {
	// WebSocketUtils.send(websocket, j);
	// }).start();
	// }

	/**
	 * Send a log message to the websocket. This method is called by logback
	 *
	 * @param message2
	 * @param timestamp
	 */
	public void sendLog(long timestamp, String level, String source, String message) {
		if (this.logSubscribers.isEmpty()) {
			// nobody subscribed
			return;
		}
		for (String id : this.logSubscribers) {
			JsonArray jId = new JsonArray();
			jId.add("log");
			jId.add(id);
			JsonObject j = DefaultMessages.log(jId, timestamp, level, source, message);
			logExecutor.execute(() -> WebSocketUtils.send(websocket, j));
		}
	}

	/**
	 * On OnOpen-Event, send current status of all channels. This is additionally to FeneconPersistence's
	 * "channelChanged" event, where only changes are sent
	 */
	private void sendCurrentStatusOfAllChannels() {
		ThingRepository thingRepository = ThingRepository.getInstance();
		for (Thing thing : thingRepository.getThings()) {
			for (Channel channel : thingRepository.getChannels(thing)) {

			}
		}

		// TODO Auto-generated method stub
		System.out.println("Onopen...");
		JsonObject j = new JsonObject();
		j.addProperty("Hallo", "Welt");
		WebSocketUtils.send(this.websocket, j);
	}
}
