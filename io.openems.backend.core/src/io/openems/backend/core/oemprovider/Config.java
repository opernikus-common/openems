package io.openems.backend.core.oemprovider;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core OEM Provider", //
		description = "The global OEM Provider.")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core OEM Provider";

	@AttributeDefinition(name = "Verbose", description = "Show verbose output (blacklisted channels).")
	boolean verbose() default false;

}
