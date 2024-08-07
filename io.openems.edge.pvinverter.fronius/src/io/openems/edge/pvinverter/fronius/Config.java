package io.openems.edge.pvinverter.fronius;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "PV-Inverter Fronius", //
		description = "Implements the Fronius PV inverter.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "pvInverter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read-Only mode", description = "In Read-Only mode no power-limitation commands are sent to the inverter")
	boolean readOnly() default true;

	@AttributeDefinition(name = "Debug mode", description = "Extends Logging for debugging purposes")
	boolean debugMode() default false;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "SunSpec 160 Start Address", description = "Start address for SunSpec160 (individual DC string informations). Leave  0 for none. Default 40263 for Symo")
	int modbusBaseAddress() default 40264;

	@AttributeDefinition(name = "Modbus target filter", description = "This is auto-generated by 'Modbus-ID'.")
	String Modbus_target() default "(enabled=true)";

	String webconsole_configurationFactory_nameHint() default "PV-Inverter Fronius [{id}]";

}
