package io.openems.edge.simulator.datasource.csv.eventbased;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator DataSource: CSV Event based", //
		description = "This service provides CSV-Input data for simulated charging stations.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "datasource0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Source", description = "A CSV-Input containing an optional title line and a series of values.")
	Source source();

	String webconsole_configurationFactory_nameHint() default "Simulator DataSource: CSV Event based [{id}]";
}
