package io.openems.edge.goodwe.charger.twostring;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "GoodWe Charger Two-String", //
		description = "Implements a GoodWe PV string (for an MPPT of two strings).")

@Deprecated
@interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "charger0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Connected Charger", description = "PV port of the inverter that is used for this charger")
	PvPort pvPort() default PvPort.PV_1;

	@AttributeDefinition(name = "GoodWe ESS or Battery-Inverter", description = "ID of GoodWe Energy Storage System or Battery-Inverter.")
	String essOrBatteryInverter_id() default "batteryInverter0";

	@AttributeDefinition(name = "GoodWe ESS or Battery-Inverter target filter", description = "This is auto-generated by 'GoodWe ESS or Battery-Inverter'.")
	String essOrBatteryInverter_target() default "(enabled=true)";

	String webconsole_configurationFactory_nameHint() default "GoodWe Charger Two-String [{id}]";
}
