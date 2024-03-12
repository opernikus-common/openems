package io.openems.edge.dccharger.victron;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
	name = "Victron BlueSolar DC Charger", //
	description = "Implements the Victron BlueSolar DC Chargers.")
public @interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "charger0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "Victron BlueSolar DC Charger";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
    String modbus_id() default "modbus0";

    @AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device. Defaults to '229' for Victron BlueSolar Modbus/TCP.")
    int modbusUnitId() default 229;

    @AttributeDefinition(name = "Modbus target filter", description = "This is auto-generated by 'Modbus-ID'.")
    String Modbus_target() default "(enabled=true)";

    String webconsole_configurationFactory_nameHint() default "Victron BlueSolar DC Charger [{id}]";

}
