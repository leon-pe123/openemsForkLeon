package io.openems.edge.io.siemenslogo;

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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;


@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.SiemensLogo", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SiemensLogoRelayImpl extends AbstractSiemensLogoRelay
		implements SiemensLogoRelay, DigitalOutput, DigitalInput, ModbusComponent, OpenemsComponent, ModbusSlave {

	private int writeOffset = 0;
	private int readOffset = 0;

	@Reference
	protected ConfigurationAdmin cm;

	private Config config;


	

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public SiemensLogoRelayImpl() {
		super(SiemensLogoRelay.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {

		this.writeOffset = this.config.modbusOffsetWriteAddress();
		this.readOffset = this.config.modbusOffsetReadAddress();
		
		return new ModbusProtocol(this, //
				// Read Inputs
				new FC1ReadCoilsTask(this.readOffset, Priority.HIGH, //
						m(SiemensLogoRelay.ChannelId.INPUT_1, new CoilElement(0 + this.readOffset)), //
						m(SiemensLogoRelay.ChannelId.INPUT_2, new CoilElement(1 + this.readOffset)), //
						m(SiemensLogoRelay.ChannelId.INPUT_3, new CoilElement(2 + this.readOffset)), //
						m(SiemensLogoRelay.ChannelId.INPUT_4, new CoilElement(3 + this.readOffset)), //
						m(SiemensLogoRelay.ChannelId.INPUT_5, new CoilElement(4 + this.readOffset)), //
						m(SiemensLogoRelay.ChannelId.INPUT_6, new CoilElement(5 + this.readOffset)), //
						m(SiemensLogoRelay.ChannelId.INPUT_7, new CoilElement(6 + this.readOffset)), //
						m(SiemensLogoRelay.ChannelId.INPUT_8, new CoilElement(7 + this.readOffset)) //						
				),
				
				
				/*
				 * For Read: Read Coils
				 */
				new FC1ReadCoilsTask(this.writeOffset, Priority.LOW, //
						m(SiemensLogoRelay.ChannelId.OUTPUT_1, new CoilElement(0 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.OUTPUT_2, new CoilElement(1 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.OUTPUT_3, new CoilElement(2 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.OUTPUT_4, new CoilElement(3 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.OUTPUT_5, new CoilElement(4 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.OUTPUT_6, new CoilElement(5 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.OUTPUT_7, new CoilElement(6 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.OUTPUT_8, new CoilElement(7 + this.writeOffset)) //
				),
				/*
				 * For Write: Write Single Coil
				 */
				new FC5WriteCoilTask(0 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.OUTPUT_1, new CoilElement(0 + this.writeOffset))), //
				new FC5WriteCoilTask(1 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.OUTPUT_2, new CoilElement(1 + this.writeOffset))), //
				new FC5WriteCoilTask(2 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.OUTPUT_3, new CoilElement(2 + this.writeOffset))), //
				new FC5WriteCoilTask(3 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.OUTPUT_4, new CoilElement(3 + this.writeOffset))), //
				new FC5WriteCoilTask(4 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.OUTPUT_5, new CoilElement(4 + this.writeOffset))), //
				new FC5WriteCoilTask(5 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.OUTPUT_6, new CoilElement(5 + this.writeOffset))), //
				new FC5WriteCoilTask(6 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.OUTPUT_7, new CoilElement(6 + this.writeOffset))), //
				new FC5WriteCoilTask(7 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.OUTPUT_8, new CoilElement(7 + this.writeOffset))) //
		);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(SiemensLogoRelay.class, accessMode, 100)//
						.channel(0 + this.writeOffset, SiemensLogoRelay.ChannelId.OUTPUT_1, ModbusType.UINT16) //
						.channel(1 + this.writeOffset, SiemensLogoRelay.ChannelId.OUTPUT_2, ModbusType.UINT16) //
						.channel(2 + this.writeOffset, SiemensLogoRelay.ChannelId.OUTPUT_3, ModbusType.UINT16) //
						.channel(3 + this.writeOffset, SiemensLogoRelay.ChannelId.OUTPUT_4, ModbusType.UINT16) //
						.channel(4 + this.writeOffset, SiemensLogoRelay.ChannelId.OUTPUT_5, ModbusType.UINT16) //
						.channel(5 + this.writeOffset, SiemensLogoRelay.ChannelId.OUTPUT_6, ModbusType.UINT16) //
						.channel(6 + this.writeOffset, SiemensLogoRelay.ChannelId.OUTPUT_7, ModbusType.UINT16) //
						.channel(7 + this.writeOffset, SiemensLogoRelay.ChannelId.OUTPUT_8, ModbusType.UINT16) //
						
						.channel(8 + this.readOffset, SiemensLogoRelay.ChannelId.INPUT_1, ModbusType.UINT16) //
						.channel(9 + this.readOffset, SiemensLogoRelay.ChannelId.INPUT_2, ModbusType.UINT16) //
						.channel(10 + this.readOffset, SiemensLogoRelay.ChannelId.INPUT_3, ModbusType.UINT16) //
						.channel(11 + this.readOffset, SiemensLogoRelay.ChannelId.INPUT_4, ModbusType.UINT16) //
						
						.channel(12 + this.readOffset, SiemensLogoRelay.ChannelId.INPUT_5, ModbusType.UINT16) //
						.channel(13 + this.readOffset, SiemensLogoRelay.ChannelId.INPUT_6, ModbusType.UINT16) //
						.channel(14 + this.readOffset, SiemensLogoRelay.ChannelId.INPUT_7, ModbusType.UINT16) //
						.channel(15 + this.readOffset, SiemensLogoRelay.ChannelId.INPUT_8, ModbusType.UINT16) //						
						
						.build()//
		);
	}

}
