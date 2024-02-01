package io.openems.edge.io.kmtronic.temperature.monitor.core;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "KMtronic Temperature Monitor", //
		description = "Implements the KMtronic temperature monitor")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "thermometer0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the device.")
	String ip();

	@AttributeDefinition(name = "Port", description = "The port of the device.")
	int port() default 80;

	@AttributeDefinition(name = "Number of sensors", description = "How many sensors are connected to the device?")
	int numberOfSensors() default 4;

	String webconsole_configurationFactory_nameHint() default "KMtronic Temperature Monitor [{id}]";
}
