package io.openems.edge.evcs.hardybarth.meter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.MeterType;

@ObjectClassDefinition(//
		name = "EVCS Hardy Barth Meter", //
		description = "Implements the Hardy Barth MP meter (Protocol: eCB1).")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this Meter?")
	MeterType type() default MeterType.PRODUCTION;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the charging station.", required = true)
	String ip() default "127.0.0.1";

	@AttributeDefinition(name = "Port", description = "The Port to access the API of the charging station.", required = true)
	int port() default 80;
	
	@AttributeDefinition(name = "Invert", description = "Invert consumption and production.", required = true)
	boolean invert() default false;

	String webconsole_configurationFactory_nameHint() default "EVCS Hardy Barth Meter [{id}]";

}