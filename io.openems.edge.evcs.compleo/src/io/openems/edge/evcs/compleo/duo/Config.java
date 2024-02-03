package io.openems.edge.evcs.compleo.duo;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.evcs.api.PhaseRotation;

@ObjectClassDefinition(//
		name = "EVCS Compleo Duo ", //
		description = "Implementation for the Compleo Duo charging station")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcs0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 255;

	@AttributeDefinition(name = "Model", description = "What model is the charging station?")
	Plug plug() default Plug.PLUG_1;

	@AttributeDefinition(name = "Minimum current", description = "Minimum current of charger in mA.")
	int minHwCurrent() default 6000;

	@AttributeDefinition(name = "Maximum current", description = "Maximum current of charger in mA.")
	int maxHwCurrent() default 16000;

	@AttributeDefinition(name = "Phase rotation", description = "The way in which the phases are physically swapped")
	PhaseRotation phaseRotation() default PhaseRotation.L1_L2_L3;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	String webconsole_configurationFactory_nameHint() default "EVCS Compleo Eco 20 [{id}]";

}
