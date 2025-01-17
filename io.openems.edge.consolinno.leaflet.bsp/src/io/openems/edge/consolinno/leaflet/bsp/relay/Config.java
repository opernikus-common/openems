package io.openems.edge.consolinno.leaflet.bsp.relay;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.consolinno.leaflet.bsp.core.enums.ModuleNumber;
import io.openems.edge.consolinno.leaflet.bsp.core.enums.RelaisPinNumber;

@ObjectClassDefinition(name = "Consolinno Leaflet BSP Relay", description = "One Relay on a Leaflet Relays Module that communicates over Modbus.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "relay0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "ModbusUnitId", description = "Unique Id for the ModbusUnit.")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Module", description = "Module where this Relay is plugged in.")
	ModuleNumber module() default ModuleNumber.MODULE_1;

	@AttributeDefinition(name = "Position", description = "Relay Number.")
	RelaisPinNumber position() default RelaisPinNumber.RELAIS_3;

	@AttributeDefinition(name = "Normally Closed", description = "tick if this Relay is Normally Closed")
	boolean normallyClosed() default false;

	@AttributeDefinition(name = "LeafletCore-ID", description = "Unique Id of the LeafletCore, this Module is attached to.")
	String leaflet_id() default "bsp0";

	@AttributeDefinition(name = "Leaflet target filter", description = "This is auto-generated by 'LeafletCore-ID'.")
	String leafletCore_target() default "(enabled=true)";

	@AttributeDefinition(name = "Modbus target filter", description = "This is auto-generated by 'Modbus-ID'.")
	String Modbus_target() default "(enabled=true)";

	String webconsole_configurationFactory_nameHint() default "Relay [{id}]";

}
