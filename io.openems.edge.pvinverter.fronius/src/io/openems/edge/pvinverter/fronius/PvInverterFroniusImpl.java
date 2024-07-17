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
import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S123_WMaxLim_Ena;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.FloatReadChannel;

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
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
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
			.put(DefaultSunSpecModel.S_160, Priority.LOW) //
			.build();

	private static final int READ_FROM_MODBUS_BLOCK = 1;

	private final Logger log = LoggerFactory.getLogger(PvInverterFroniusImpl.class);

	@Reference
	private ConfigurationAdmin cm;
	
	private Config config;	

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
		this.config = config;
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

	public void PVDataHandler() throws OpenemsNamedException {

		if (this.isSunSpecInitializationCompleted()) {
			try {
				FloatReadChannel st1DcPowerChannel = this.channel(DefaultSunSpecModel.S160.MODULE_1_DCW.getChannelId());
				int st1DcPowerValue = st1DcPowerChannel.value().getOrError().intValue();
				this._setSt1DcPower(st1DcPowerValue);

				FloatReadChannel st2DcPowerChannel = this.channel(DefaultSunSpecModel.S160.MODULE_2_DCW.getChannelId());
				int st2DcPowerValue = st2DcPowerChannel.value().getOrError().intValue();
				this._setSt2DcPower(st2DcPowerValue);

				this.logDebug(this.log,
						"Reading Power Values DC1 / DC2: " + st1DcPowerValue + " / " + st2DcPowerValue + " W");
			} catch (InvalidValueException e) {
				this.logDebug(this.log, "NO DATA for Power Values DC1 / DC2: " + e.getMessage());
			}

			try {
				FloatReadChannel st1DcEnergyChannel = this
						.channel(DefaultSunSpecModel.S160.MODULE_1_DCWH.getChannelId());
				int st1DcEnergyValue = st1DcEnergyChannel.value().getOrError().intValue();
				this._setSt1DcEnergy(st1DcEnergyValue);

				FloatReadChannel st2DcEnergyChannel = this
						.channel(DefaultSunSpecModel.S160.MODULE_2_DCWH.getChannelId());
				int st2DcEnergyValue = st2DcEnergyChannel.value().getOrError().intValue();
				this._setSt2DcEnergy(st2DcEnergyValue);

				this.logDebug(this.log,
						"Reading Energy Values DC1 / DC2: " + st1DcEnergyValue + " / " + st2DcEnergyValue + " Wh");
			} catch (InvalidValueException e) {
				this.logDebug(this.log, "NO DATA for Energy Values DC1 / DC2: " + e.getMessage());
			}

			try {
				FloatReadChannel st1DcCurrentChannel = this
						.channel(DefaultSunSpecModel.S160.MODULE_1_DCA.getChannelId());
				int st1DcCurrentValue = st1DcCurrentChannel.value().getOrError().intValue();
				this._setSt1DcCurrent(st1DcCurrentValue);

				FloatReadChannel st2DcCurrentChannel = this
						.channel(DefaultSunSpecModel.S160.MODULE_2_DCA.getChannelId());
				int st2DcCurrentValue = st2DcCurrentChannel.value().getOrError().intValue();
				this._setSt2DcCurrent(st2DcCurrentValue);

				this.logDebug(this.log,
						"Reading Current Values DC1 / DC2: " + st1DcCurrentValue + " / " + st2DcCurrentValue + " A");
			} catch (InvalidValueException e) {
				this.logDebug(this.log, "NO DATA for Current Values DC1 / DC2: " + e.getMessage());
			}

			try {
				FloatReadChannel st1DcVoltageChannel = this
						.channel(DefaultSunSpecModel.S160.MODULE_1_DCV.getChannelId());
				int st1DcVoltageValue = st1DcVoltageChannel.value().getOrError().intValue();
				this._setSt1DcVoltage(st1DcVoltageValue);

				FloatReadChannel st2DcVoltageChannel = this
						.channel(DefaultSunSpecModel.S160.MODULE_2_DCV.getChannelId());
				int st2DcVoltageValue = st2DcVoltageChannel.value().getOrError().intValue();
				this._setSt2DcVoltage(st2DcVoltageValue);

				this.logDebug(this.log,
						"Reading Voltage Values DC1 / DC2: " + st1DcVoltageValue + " / " + st2DcVoltageValue + " V");
			} catch (InvalidValueException e) {
				this.logDebug(this.log, "NO DATA for Voltage Values DC1 / DC2: " + e.getMessage());
			}

		} else {
			log.info("SunSpec model not completely initialized. Skipping PVDataHandler");
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);

		switch (event.getTopic()) {

		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:

			try {

				this.PVDataHandler();
			} catch (OpenemsNamedException e) {
				log.warn("Cannot write S160 data yet");
			}

			break;
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode));
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}
}
