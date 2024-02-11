package io.openems.edge.simulator.evcs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Priority;

@ObjectClassDefinition(//
		name = "Simulator EVCS", //
		description = "This simulates a Electric Vehicle Charging Station using data provided by a data source.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Maximum power", description = "Maximum power of the charger in Watt.", required = true)
	int maxHwPower() default 22080;

	@AttributeDefinition(name = "Minimum power", description = "Minimum power of the charger in Watt.", required = true)
	int minHwPower() default 4140;

	@AttributeDefinition(name = "Phase rotation", description = "The way the phases are physically swapped")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	@AttributeDefinition(name = "Charge Priority", description = "Priority in comparison to other chargepoints.")
	Priority priority() default Priority.LOW;

	@AttributeDefinition(name = "Datasource-ID", description = "ID of Simulator Datasource.")
	String datasource_id() default "";

	String webconsole_configurationFactory_nameHint() default "Simulator EVCS [{id}]";

}