package io.openems.edge.heater.chp.dachs.thermometer;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Heater CHP Dachs Thermometer", //
		description = "Implements a thermometer of the Dachs Chp")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "thermometer0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Thermometer type", description = "What is measured by the thermometer?")
	ThermometerType type() default ThermometerType.FLOW_TEMPERATURE;

	String webconsole_configurationFactory_nameHint() default "Heater CHP Dachs Thermometer [{id}]";
}