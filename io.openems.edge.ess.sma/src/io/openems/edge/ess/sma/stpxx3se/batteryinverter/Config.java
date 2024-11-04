package io.openems.edge.ess.sma.stpxx3se.batteryinverter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "ESS SMA Sunny Tripower SE Hybrid Inverter", //
		description = "Implements the SMA Sunny Tripower XX SE hybrid inverter.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "batteryInverter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus-Unit-ID", description = "Modbus Unit-ID (Unit-ID + 123).")
	int modbusUnitId() default 126;

	@AttributeDefinition(name = "Control mode", description = "Sets the Control mode. \"Internal\" allows no power setpoints, "
			+ "\"Remote\" allows full control by OpenEMS, and \"Smart\" uses the internal mode unless OpenEMS decides otherwise.")
	ControlMode controlMode() default ControlMode.SMART;

	@AttributeDefinition(name = "Modbus target filter", description = "This is auto-generated by 'Modbus-ID'.")
	String Modbus_target() default "(enabled=true)";

	String webconsole_configurationFactory_nameHint() default "ESS SMA Sunny Tripower SE Hybrid Inverter [{id}]";
}
