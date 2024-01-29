package io.openems.edge.evcs.keba.kecontact;

import io.openems.edge.evcs.api.Priority;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "EVCS KEBA KeContact", //
		description = "Implements the KEBA KeContact P20/P30 electric vehicle charging station.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	// oEMS added
	@AttributeDefinition(name = "Dip Switch Info", description = "Hides the info about Dip Switch Settings.")
	boolean dipSwitchInfo() default true;

	@AttributeDefinition(name = "Charge Priority", description = "Priority in comparison to other chargepoints.")
	Priority priority() default Priority.LOW;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the charging station.", required = true)
	String ip() default "192.168.25.11";

	@AttributeDefinition(name = "Minimum power", description = "Minimum current of the Charger in mA.", required = true)
	int minHwCurrent() default 6000;

	@AttributeDefinition(name = "Use display?", description = "Activates the KEBA display to show the current power or states.", required = true)
	boolean useDisplay() default true;

	String webconsole_configurationFactory_nameHint() default "EVCS KEBA KeContact [{id}]";
}
