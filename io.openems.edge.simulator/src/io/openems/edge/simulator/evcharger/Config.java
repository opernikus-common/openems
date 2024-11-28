package io.openems.edge.simulator.evcharger;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.evcharger.api.data.PhaseRotation;

@ObjectClassDefinition(//
		name = "Simulator EVCharger", //
		description = "This simulates an Electric Vehicle Charger using data provided by a data source.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evCharger0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "Raw EvCharger";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Max Current", description = "Maximum current [mA].", required = true)
	int maxCurrent() default 32000;

	@AttributeDefinition(name = "Min Current", description = "Minimum current [mA].", required = true)
	int minCurrent() default 6000;

	@AttributeDefinition(name = "Responsiveness", description = "Delays the reaction of the simulated evcharger by this number of core cycles")
	int responsiveness() default 4;

	@AttributeDefinition(name = "Phase rotation", description = "The way the phases are physically swapped")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	@AttributeDefinition(name = "Phaselimitation available", description = "Should this simulation be able to limit the number of phases.")
	boolean phaseLimitationAvailable() default false;

	@AttributeDefinition(name = "Datasource-ID", description = "ID of Simulator Datasource.")
	String datasource_id() default "";

	// @AttributeDefinition(name = "Unique-Alias", description = "Unique Alias set
	// by the AppCenter. Can be ignored if configured manually.")
	// String unique_alias() default "";

	String webconsole_configurationFactory_nameHint() default "Simulator EVCharger [{id}]";

}