package io.openems.edge.io.kmtronic.temperature.monitor.sensor;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "KMtronic Temperature sensor", //
		description = "Implements a sensor of the KMtronic temperature monitor")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "sensor0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Sensor number", description = "Number of the sensor (between 1 and 4)")
	int sensorNumber() default 1;

	String webconsole_configurationFactory_nameHint() default "KMtronic Temperature sensor [{id}]";
}
