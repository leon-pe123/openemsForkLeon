package io.openems.edge.pvinverter.fronius;

import java.util.Map;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S123_WMaxLim_Ena;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.pvinverter.sunspec.AbstractSunSpecPvInverter;
import io.openems.edge.pvinverter.sunspec.Phase;
import io.openems.edge.pvinverter.sunspec.SunSpecPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.Fronius", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class PvInverterFroniusImpl extends AbstractSunSpecPvInverter implements PvInverterFronius, SunSpecPvInverter,
		ManagedSymmetricPvInverter, ElectricityMeter, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave {

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) // from 40002

			// .put(DefaultSunSpecModel.S_103, Priority.LOW) //
			.put(DefaultSunSpecModel.S_113, Priority.LOW) // from 40069
			.put(DefaultSunSpecModel.S_120, Priority.LOW) // from 40159
			.put(DefaultSunSpecModel.S_123, Priority.LOW) // from 40237
			.build();

	private static final int READ_FROM_MODBUS_BLOCK = 1;

	private final Logger log = LoggerFactory.getLogger(PvInverterFroniusImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public PvInverterFroniusImpl() {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				SunSpecPvInverter.ChannelId.values(), //
				PvInverterFronius.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.readOnly(),
				config.modbusUnitId(), this.cm, "Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK, Phase.ALL)) {
			return;
		}
	}

	@Override
	public void setActivePowerLimit(int value) throws OpenemsNamedException {

		EnumWriteChannel wMaxLimEnaChannel;
		if (this.isSunSpecInitializationCompleted()) {
			// Get Power Limitation Enabled WriteChannel
			wMaxLimEnaChannel = this.getSunSpecChannelOrError(DefaultSunSpecModel.S123.W_MAX_LIM_ENA);
		} else {
			log.info("SunSpec model not completely intialized. Skipping PV Limiter");
			return;
		}

		this.getActivePowerLimitChannel().setNextWriteValue(value);

		// has to be written every time
		wMaxLimEnaChannel.setNextWriteValue(S123_WMaxLim_Ena.ENABLED);

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode));
	}
}
