package io.openems.edge.heater.chp.dachs.meter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.MeterType;

@ObjectClassDefinition(//
		name = "Heater CHP Dachs Virtual Meter Threephase", //
		description = "Implements a CHP Virtual Threephase meter. ")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Core ID", description = "ID of the CHP Dachs Component.")
	String core_id() default "chp0";
	
	@AttributeDefinition(name = "Meter-Type", description = "Grid (default), Production, Consumption")
	MeterType type() default MeterType.PRODUCTION;
	
	String webconsole_configurationFactory_nameHint() default "Heater CHP Dachs Virtual Meter Threephase [{id}]";

}
