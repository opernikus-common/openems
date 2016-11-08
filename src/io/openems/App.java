/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
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
package io.openems;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.exception.ConfigException;
import io.openems.core.Config;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		log.info("OpenEMS started");

		// Get config directory
		Path configPath = Paths.get("/etc", "openems.d");
		if (!configPath.toFile().exists()) {
			configPath = Paths.get("D:", "fems", "openems", "etc", "openems.d");
		}
		if (!configPath.toFile().exists()) {
			throw new ConfigException("No config directory found!");
		}

		// Load config
		Config config = new Config(configPath);
		config.parseConfigFiles();
		log.info("OpenEMS config loaded");

		// Start vertx
		// Vertx vertx = Vertx.vertx();
		// Deploy REST-Api verticle
		// vertx.deployVerticle(new RestApi());

		// Thread.sleep(3000);

		// Databus databus = Databus.getInstance();
		// log.info("ess0/soc: " + databus.getValue("ess0", "Soc"));

		// vertx.deployVerticle(new WebsocketApi());
		// vertx.deployVerticle(new WebSocketClient());
	}
}
