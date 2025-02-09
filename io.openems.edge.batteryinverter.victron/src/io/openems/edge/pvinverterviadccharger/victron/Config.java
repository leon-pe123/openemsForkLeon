package io.openems.edge.pvinverterviadccharger.victron;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.MeterType;

@ObjectClassDefinition(//
	name = "Victron PV inverter via BlueSolar DC Charger", //
	description = "Implements the Victron BlueSolar DC Charger as PV-inverter.")
public @interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "charger0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "Victron BlueSolar DC Charger";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Meter-Type", description = "Meter-Type of this Meter (only provides the meter types PRODUCTION).")
    MeterType type() default MeterType.PRODUCTION;

    @AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
    String modbus_id() default "modbus0";

    @AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device. Defaults to '229' for Victron BlueSolar Modbus/TCP.")
    int modbusUnitId() default 229;

    @AttributeDefinition(name = "Modbus target filter", description = "This is auto-generated by 'Modbus-ID'.")
    String Modbus_target() default "(enabled=true)";

    String webconsole_configurationFactory_nameHint() default "Victron PV inverter via BlueSolar DC Charger [{id}]";

}
