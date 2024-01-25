package io.openems.edge.evcs.hardybarth.ecb1;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "EVCS Hardy Barth eCB1", //
		description = "Implements the Hardy Barth - electric vehicle charging station (Protocol: eCB1).")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode (0 = deactivated, 1 = READ, 2 = write, 3 = all")
	int debug() default 0;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the charging station.", required = true)
	String ip() default "127.0.0.1";

	@AttributeDefinition(name = "Port", description = "The Port to access the API of the charging station.", required = true)
	int port() default 80;

	@AttributeDefinition(name = "Meter Index", description = "The index of the meter JSON Object to be used (see URL: /api/v1/all)", required = true)
	int meterIndex() default 1;
	
	@AttributeDefinition(name = "Charge Index", description = "The index of the chargecontrol JSON Object to be used (see URL: /api/v1/all)", required = true)
	int chargeControlIndex() default 0;

	
	@AttributeDefinition(name = "Minimum power", description = "Minimum current of the Charger in A.", required = true)
	int minHwCurrent() default 6;

	@AttributeDefinition(name = "Maximum power", description = "Maximum current of the Charger in A.", required = true)
	int maxHwCurrent() default 32;

	@AttributeDefinition(name = "MP One Phase only?", description = "true, if minimum power is for 1 Phase only, on false minimum power is for all 3 phases?")
	boolean minPowerOnePhaseOnly() default false;
	
	@AttributeDefinition(name = "StartStopDelay", description = "Delay in seconds between two subsequent START/STOP command calls", required = true)
	int commandStartStopDelay() default 60;

	String webconsole_configurationFactory_nameHint() default "EVCS Hardy Barth eCB1 [{id}]";

}