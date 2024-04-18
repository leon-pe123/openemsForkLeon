package io.openems.edge.solaredge.hybrid.ess;

import java.util.ArrayList;
import java.util.List;
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
import io.openems.edge.common.event.EdgeEventConstants;
import org.osgi.service.metatype.annotations.Designate;
import com.google.common.collect.ImmutableMap;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
//import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.solaredge.enums.ControlMode;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import io.openems.edge.solaredge.enums.AcChargePolicy;
import io.openems.edge.solaredge.enums.ChargeDischargeMode;
import io.openems.edge.solaredge.charger.SolaredgeDcCharger;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "SolarEdge.Hybrid.ESS", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE) //

@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})

public class SolarEdgeHybridEssImpl extends AbstractSunSpecEss implements SolarEdgeHybridEss, ManagedSymmetricEss,
		SymmetricEss, HybridEss, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave, TimedataProvider {

	private static final int READ_FROM_MODBUS_BLOCK = 1;
	private final List<SolaredgeDcCharger> chargers = new ArrayList<>();

	// Hardware-Limits
	protected static final int HW_MAX_APPARENT_POWER = 10000;
	protected static final int HW_ALLOWED_CHARGE_POWER = -5000;
	protected static final int HW_ALLOWED_DISCHARGE_POWER = 5000;
	
	

	// AC-side
	// private final CalculateEnergyFromPower calculateAcChargeEnergyCalculated =
	// new CalculateEnergyFromPower(this,
	// SolarEdgeHybridEss.ChannelId.ACTIVE_CHARGE_ENERGY_CALCULATED);

	private final CalculateEnergyFromPower calculateAcChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);

	// Active discharge Energy comes from SunSpec-Register
	// private final CalculateEnergyFromPower calculateAcDischargeEnergy = new
	// CalculateEnergyFromPower(this,
	// SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	// DC-side
	private final CalculateEnergyFromPower calculateDcChargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_CHARGE_ENERGY);

	private final CalculateEnergyFromPower calculateDcDischargeEnergy = new CalculateEnergyFromPower(this,
			HybridEss.ChannelId.DC_DISCHARGE_ENERGY);

	private int cycleCounter = 60;
	// private boolean sunspecInit = false; // Experimental. Gets true if
	// SunspecInitialization is through

	// Calculate moving average over last x values - we use 5 here
	private AverageCalculator acPowerAverageCalculator = new AverageCalculator(5);
	private AverageCalculator activePowerWantedAverageCalculator = new AverageCalculator(5);

	private Config config;

	private int originalActivePowerWanted;
	private int activePowerLimit;  // Limit for the whole system including PC surplus power
	//private int pvPower;
	private String errorOnAverage = "";

	@Reference
	private Power power;

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.LOW) //
			.put(DefaultSunSpecModel.S_203, Priority.LOW) //
			.put(DefaultSunSpecModel.S_802, Priority.LOW) //
			.build();
	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private volatile Timedata timedata = null;

	public SolarEdgeHybridEssImpl() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				SolarEdgeHybridEss.ChannelId.values());

		addStaticModbusTasks(this.getModbusProtocol());

	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id(), READ_FROM_MODBUS_BLOCK)) {
			return;
		}
		this.config = config;

		this.installListener();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public void addCharger(SolaredgeDcCharger charger) {
		this.chargers.add(charger);
	}

	@Override
	public void removeCharger(SolaredgeDcCharger charger) {
		this.chargers.remove(charger);
	}

	@Override
	public String getModbusBridgeId() {
		return this.config.modbus_id();
	}

	@Override
	public void applyPower(int activePowerWanted, int reactivePowerWanted) throws OpenemsNamedException {
		cycleCounter++;
		errorOnAverage = "";
		

		


		this.originalActivePowerWanted = activePowerWanted;
		this.activePowerWantedAverageCalculator.addValue(activePowerWanted);

		// If the difference between actual PowerWanted and the moving average over the
		// last 5 values is to high - don´t apply ChargePower directly
		if (Math.abs(activePowerWantedAverageCalculator.getAverage() - activePowerWanted) > 500) {
			errorOnAverage = "| Error On Average: Wanted " + activePowerWanted + "/Avg: "
					+ activePowerWantedAverageCalculator.getAverage();
			activePowerWanted = activePowerWantedAverageCalculator.getAverage();

		}

		this._setChargePowerWanted(activePowerWanted);

		// Read-only mode -> switch to max. self consumption automatic
		if (this.config.readOnlyMode()) {
			if (cycleCounter >= 10) {
				cycleCounter = 0;
				// Switch to automatic mode
				this._setControlMode(ControlMode.SE_CTRL_MODE_MAX_SELF_CONSUMPTION);
			}
			return;
		} else {

			int maxDischargeContinuesPower = getMaxDischargeContinuesPower().orElse(0); // Positive for Discharge
			int maxChargeContinuesPower = getMaxChargeContinuesPower().orElse(0) * -1; // Negative for charging

			this._setControlMode(ControlMode.SE_CTRL_MODE_REMOTE); // Now the device can be remote controlled
			this._setAcChargePolicy(AcChargePolicy.SE_CHARGE_DISCHARGE_MODE_ALWAYS);

			// set Remote Control Modus if it´s not set or every 10 cycles. // ToDO: change
			// Counter to time-relative
			if (isControlModeRemote() == false || isStorageChargePolicyAlways() == false || cycleCounter >= 10) {

				// The next 2 are fallback values which should become active after the 60
				// seconds
				// timeout
				this._setChargeDischargeDefaultMode(ChargeDischargeMode.SE_CHARGE_POLICY_MAX_SELF_CONSUMPTION);
				this._setRemoteControlTimeout(60);

				cycleCounter = 0;

			}

			// Check if configured Limits are less than hardware limits
			if (config.DischargePowerLimit() < maxDischargeContinuesPower) {
				maxDischargeContinuesPower = config.DischargePowerLimit();
			}

			// Check if configured Limits are less than hardware limits
			if ((config.ChargePowerLimit() * -1) > maxChargeContinuesPower) {
				maxChargeContinuesPower = (config.ChargePowerLimit() * -1);
			}

			// We assume to be in RC-Mode
			_setAllowedChargePower(maxChargeContinuesPower);
			_setAllowedDischargePower(maxDischargeContinuesPower);

			if (activePowerWanted < 0) { // Negative Values are for charging
				if (maxChargeContinuesPower > activePowerWanted) {
					activePowerWanted = maxChargeContinuesPower;
				}
				this._setRemoteControlCommandMode(ChargeDischargeMode.SE_CHARGE_POLICY_PV_AC); // Mode for charging);
				this._setMaxChargePower((activePowerWanted * -1));// Values for register must be positive
				this._setMaxDischargePower(0);

			} else {
				if (maxDischargeContinuesPower < activePowerWanted) {
					activePowerWanted = maxDischargeContinuesPower;
				}

				this._setRemoteControlCommandMode(ChargeDischargeMode.SE_CHARGE_POLICY_MAX_EXPORT); // Mode for
																									// Discharging);
				this._setMaxDischargePower(activePowerWanted);
				this._setMaxChargePower(0);
			}

		}

	}

	private void setLimits() {
		_setMaxApparentPower(HW_MAX_APPARENT_POWER);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private boolean isControlModeRemote() {

		EnumReadChannel controlModeChannel = this.channel(SolarEdgeHybridEss.ChannelId.CONTROL_MODE);
		ControlMode controlMode = controlModeChannel.value().asEnum();

		return controlMode == ControlMode.SE_CTRL_MODE_REMOTE;

	}

	private boolean isStorageChargePolicyAlways() {
		EnumReadChannel acChargePolicyChannel = this.channel(SolarEdgeHybridEss.ChannelId.AC_CHARGE_POLICY);
		AcChargePolicy acChargePolicy = acChargePolicyChannel.value().asEnum();

		return acChargePolicy == AcChargePolicy.SE_CHARGE_DISCHARGE_MODE_ALWAYS;
	}

	/**
	 * Adds static modbus tasks.
	 * 
	 * @param protocol the {@link ModbusProtocol}
	 * @throws OpenemsException on error
	 */
	private void addStaticModbusTasks(ModbusProtocol protocol) throws OpenemsException {

		protocol.addTask(//
				new FC3ReadRegistersTask(0x9C93, Priority.HIGH, //
						//
						m(SolarEdgeHybridEss.ChannelId.POWER_AC, //
								new SignedWordElement(0x9C93)),
						m(SolarEdgeHybridEss.ChannelId.POWER_AC_SCALE, //
								new SignedWordElement(0x9C94))));

		protocol.addTask(//
				new FC3ReadRegistersTask(0x9CA4, Priority.HIGH, //

						m(SolarEdgeHybridEss.ChannelId.POWER_DC, //
								new SignedWordElement(0x9CA4)),
						m(SolarEdgeHybridEss.ChannelId.POWER_DC_SCALE, //
								new SignedWordElement(0x9CA5))));

		protocol.addTask(//
				// new FC3ReadRegistersTask(0xE142, Priority.LOW, //
				new FC3ReadRegistersTask(0xE144, Priority.LOW, //

						// m(HybridEss.ChannelId.DC_DISCHARGE_ENERGY, //
						// new FloatDoublewordElement(0xE142).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_CONTINUES_POWER, //
								new FloatDoublewordElement(0xE144).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_CONTINUES_POWER, //
								new FloatDoublewordElement(0xE146).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_PEAK_POWER, //
								new FloatDoublewordElement(0xE148).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_PEAK_POWER, //
								new FloatDoublewordElement(0xE14A).wordOrder(WordOrder.LSWMSW)),

						new DummyRegisterElement(0xE14C, 0xE16B), // Reserved
						m(SolarEdgeHybridEss.ChannelId.BATT_AVG_TEMPERATURE, //
								new FloatDoublewordElement(0xE16C).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.BATT_MAX_TEMPERATURE, //
								new FloatDoublewordElement(0xE16E).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.BATT_ACTUAL_VOLTAGE, //
								new FloatDoublewordElement(0xE170).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.BATT_ACTUAL_CURRENT, //
								new FloatDoublewordElement(0xE172).wordOrder(WordOrder.LSWMSW)),
						m(HybridEss.ChannelId.DC_DISCHARGE_POWER, // Instantaneous power to or from battery
								new FloatDoublewordElement(0xE174).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.INVERT),

						// Active Charge / Discharge energy are only valid until the next day/loading
						// cycle (not clear or verified)
						m(SolarEdgeHybridEss.ChannelId.BATT_LIFETIME_EXPORT_ENERGY, //
								new UnsignedQuadruplewordElement(0xE176).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.BATT_LIFETIME_IMPORT_ENERGY, //
								new UnsignedQuadruplewordElement(0xE17A).wordOrder(WordOrder.LSWMSW))

				));

		protocol.addTask(//
				new FC3ReadRegistersTask(0xE17E, Priority.LOW, //
						m(SolarEdgeHybridEss.ChannelId.BATT_MAX_CAPACITY, //
								new FloatDoublewordElement(0xE17E).wordOrder(WordOrder.LSWMSW)),
						m(SymmetricEss.ChannelId.CAPACITY, // Available capacity or "real" capacity
								new FloatDoublewordElement(0xE180).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.SOH, //
								new FloatDoublewordElement(0xE182).wordOrder(WordOrder.LSWMSW)),
						m(SymmetricEss.ChannelId.SOC, //
								new FloatDoublewordElement(0xE184).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.BATTERY_STATUS, //
								new UnsignedDoublewordElement(0xE186).wordOrder(WordOrder.LSWMSW))));

		protocol.addTask(//
				new FC3ReadRegistersTask(0xE004, Priority.HIGH, //
						m(SolarEdgeHybridEss.ChannelId.CONTROL_MODE, new UnsignedWordElement(0xE004)),
						m(SolarEdgeHybridEss.ChannelId.AC_CHARGE_POLICY, new UnsignedWordElement(0xE005)),
						m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_LIMIT,
								new FloatDoublewordElement(0xE006).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.STORAGE_BACKUP_LIMIT,
								new FloatDoublewordElement(0xE008).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.CHARGE_DISCHARGE_DEFAULT_MODE, new UnsignedWordElement(0xE00A)),
						m(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_TIMEOUT,
								new UnsignedDoublewordElement(0xE00B).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_COMMAND_MODE, new UnsignedWordElement(0xE00D)),
						m(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_POWER,
								new FloatDoublewordElement(0xE00E).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_POWER,
								new FloatDoublewordElement(0xE010).wordOrder(WordOrder.LSWMSW))));

		protocol.addTask(//
				new FC16WriteRegistersTask(0xE004,
						m(SolarEdgeHybridEss.ChannelId.SET_CONTROL_MODE, new SignedWordElement(0xE004)),
						m(SolarEdgeHybridEss.ChannelId.SET_AC_CHARGE_POLICY, new SignedWordElement(0xE005)), // Max.
																												// charge
																												// power.
																												// Negative
																												// values
						m(SolarEdgeHybridEss.ChannelId.SET_MAX_CHARGE_LIMIT,
								new FloatDoublewordElement(0xE006).wordOrder(WordOrder.LSWMSW)), // kWh or percent
						m(SolarEdgeHybridEss.ChannelId.SET_STORAGE_BACKUP_LIMIT,
								new FloatDoublewordElement(0xE008).wordOrder(WordOrder.LSWMSW)), // Percent of capacity
						m(SolarEdgeHybridEss.ChannelId.SET_CHARGE_DISCHARGE_DEFAULT_MODE,
								new UnsignedWordElement(0xE00A)), // Usually set to 1 (Charge PV excess only)
						m(SolarEdgeHybridEss.ChannelId.SET_REMOTE_CONTROL_TIMEOUT,
								new UnsignedDoublewordElement(0xE00B).wordOrder(WordOrder.LSWMSW)),
						m(SolarEdgeHybridEss.ChannelId.SET_REMOTE_CONTROL_COMMAND_MODE,
								new UnsignedWordElement(0xE00D)),
						m(SolarEdgeHybridEss.ChannelId.SET_MAX_CHARGE_POWER,
								new FloatDoublewordElement(0xE00E).wordOrder(WordOrder.LSWMSW)), // Max. charge power.
																									// Negative values
						m(SolarEdgeHybridEss.ChannelId.SET_MAX_DISCHARGE_POWER,
								new FloatDoublewordElement(0xE010).wordOrder(WordOrder.LSWMSW)) // Max. discharge power.
																								// Positive values
				));
	}

	private void installListener() {
		this.getCapacityChannel().onUpdate(value -> {
			Integer soc = this.getSoc().get();

			if (soc == null) {
				return;
			}
			if (soc > 0) {
				int useableCapacity = (int) Math.round((float) value.get() * (soc / 100f));
				this._setUseableCapacity(useableCapacity);

			}

		});

	}

	public void _setMyActivePower() {
		// ActivePower is the actual AC output including battery discharging
		int acPower = this.getAcPower().orElse(0);
		int acPowerScale = this.getAcPowerScale().orElse(0);
		double acPowerValue = acPower * Math.pow(10, acPowerScale);

		int dcPower = this.getDcPower().orElse(0);
		int dcPowerScale = this.getDcPowerScale().orElse(0);
		double dcPowerValue = dcPower * Math.pow(10, dcPowerScale);

		// The problem is that ac-power never gets negative (e.g. when charging battery
		// from grid)
		// so AC-Power can´t be used for further calculation.
		// We set DC-Power if: AC-Power is 0 AND DC-Power is negative
		// Yes, this look weird
		if (acPowerValue == 0 && dcPowerValue < 0) {
			acPowerValue = dcPowerValue;
		}

		acPowerAverageCalculator.addValue((int) acPowerValue);
		// Experimental!
		// to avoid scaling effects only values are valid that do not differ more than
		// 1000W
		if (Math.abs(acPowerAverageCalculator.getAverage() - acPowerValue) < 1000) {
			this._setActivePower((int) acPowerValue);
		}

		// ToDo
		this.activePowerLimit = 10000;// limit for the whole system including PV surplus
		//this.pvPower = this.getPvPower(chargers);
		if (this.activePowerLimit > acPowerValue) {
			this.handleSurplusPower();  // Power limitations have to be set
		}
		
	}

	@Override
	protected void onSunSpecInitializationCompleted() {
		// TODO Add mappings for registers from S1 and S103

		this.mapFirstPointToChannel(//
				ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S120.W_RTG);

		// AC-Output from the Inverter. Could be the combination from PV + battery
		/*
		 * this.mapFirstPointToChannel(// SymmetricEss.ChannelId.ACTIVE_POWER, //
		 * ElementToChannelConverter.DIRECT_1_TO_1, // DefaultSunSpecModel.S103.W);
		 */

		this.mapFirstPointToChannel(//
				SymmetricEss.ChannelId.REACTIVE_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S103.V_AR);
		this.setLimits();

		this.mapFirstPointToChannel(//
				SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S103.WH); // 103.WH holds value for lifetime production (battery + pv). Remember:
												// battery can also be loaded from AC/grid

		// sunspecInit = true;

	}

	@Override
	public String debugLog() {
		if (config.debugMode()) {
			return "SoC:" + this.getSoc().asString() //
					+ "|L:" + this.getActivePower().asString() //
					+ "|ChargeEnergy:"
					+ this.channel(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY).value().asStringWithoutUnit()
					+ "|DisChargeEnergy:"
					+ this.channel(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY).value().asStringWithoutUnit()

					+ "|DcDisChargePower:"
					+ this.channel(HybridEss.ChannelId.DC_DISCHARGE_POWER).value().asStringWithoutUnit()
					+ "|DcDisChargeEnergy:"
					+ this.channel(HybridEss.ChannelId.DC_DISCHARGE_ENERGY).value().asStringWithoutUnit()
					+ "|DcChargeEnergy:"
					+ this.channel(HybridEss.ChannelId.DC_CHARGE_ENERGY).value().asStringWithoutUnit()

					+ ";" + "|ControlMode "
					+ this.channel(SolarEdgeHybridEss.ChannelId.CONTROL_MODE).value().asStringWithoutUnit() //
					+ "|ChargePolicy "
					+ this.channel(SolarEdgeHybridEss.ChannelId.AC_CHARGE_POLICY).value().asStringWithoutUnit() //
					+ "|DefaultMode "
					+ this.channel(SolarEdgeHybridEss.ChannelId.CHARGE_DISCHARGE_DEFAULT_MODE).value()
							.asStringWithoutUnit() //
					+ "|RemoteControlMode "
					+ this.channel(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_COMMAND_MODE).value()
							.asStringWithoutUnit() //
					+ errorOnAverage + "\n|ChargePowerWantedAvg " + this.activePowerWantedAverageCalculator.getAverage()
					+ "|ChargePowerWantedOriginal " + this.originalActivePowerWanted + "|ChargePowerWanted "
					+ this.getChargePowerWanted().asString()

					+ "|Allowed Charge / Discharge Power " + this.getAllowedChargePower().asString() + " / "
					+ this.getAllowedDischargePower().asString()

					+ "\n|ChargePower "
					+ this.channel(SolarEdgeHybridEss.ChannelId.MAX_CHARGE_POWER).value().asStringWithoutUnit() //
					+ "|DischargePower "
					+ this.channel(SolarEdgeHybridEss.ChannelId.MAX_DISCHARGE_POWER).value().asStringWithoutUnit() //
					+ "|CommandTimeout "
					+ this.channel(SolarEdgeHybridEss.ChannelId.REMOTE_CONTROL_TIMEOUT).value().asStringWithoutUnit() //

					+ "|" + this.getGridModeChannel().value().asOptionString() //
					+ "|Feed-In:";
		} else
			return "SoC:" + this.getSoc().asString() //
					+ "|L:" + this.getActivePower().asString();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		// super.handleEvent(event);

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this._setMyActivePower();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this._setMyActivePower();
			this.setLimits();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this._setMyActivePower();
			this.calculateEnergy();
			break;
		}
	}



	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		//
		return 1;
	}

	@Override
	public boolean isManaged() {
		// return true;

		// Just for Testing
		return !this.config.readOnlyMode();
	}

/*	
	private int getPvPower(List<SolaredgeDcCharger> chargers) {
		
		for (EssDcCharger charger : chargers) {
			pvPower += charger.getActualPower().orElse(0);
		}
		return pvPower;
	}
*/	

	
	/**
	 * Calculate the Energy values from DcDischargePower. This should be the
	 * charging power from or to battery.
	 * 
	 * negative values for Charge; positive for Discharge
	 */
	private void calculateEnergy() {
		// Calculate Energy

		// Actual Power DC from or to battery
		var activeDcPower = this.getDcDischargePower().get(); // Instantaneous power to or from battery

		if (activeDcPower == null) {
			// Not available
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);
		} else if (activeDcPower > 0) {
			// DisCharging Battery
			this.calculateDcChargeEnergy.update(0);
			this.calculateDcDischargeEnergy.update(activeDcPower);
		} else if (activeDcPower < 0) {
			// Charging Battery
			this.calculateDcChargeEnergy.update(activeDcPower * -1);
			this.calculateDcDischargeEnergy.update(0);
		} else { // UNDEFINED??
			this.calculateDcChargeEnergy.update(null);
			this.calculateDcDischargeEnergy.update(null);
		}

		/*
		 * Calculate AC Energy AC Energy out/in the ESS including: - AC-Production (from
		 * PV) - Battery Discharging (producing AC) - Sell to grid - Consumption
		 * 
		 * DC-Output, i.e. Charging the battery is NOT included
		 */
		var activeAcPower = this.getActivePower().get(); // AC-Power never gets negative. So it´s AC power out of the
															// ESS. Actually we don´t need the following calculation
		if (activeAcPower == null) {
			// Not available
			this.calculateAcChargeEnergy.update(null);
			// this.calculateAcDischargeEnergy.update(null); // Mapped to SunSpec register
			// 103Wh.Energy leaving the hybrid-system: battery & PV
		} else if (activeAcPower > 0) {
			// Discharge
			this.calculateAcChargeEnergy.update(0);
			// this.calculateAcDischargeEnergy.update(activeAcPower);
		} else if (activeAcPower < 0) {
			// Charge
			this.calculateAcChargeEnergy.update(activeAcPower * -1);
			// this.calculateAcDischargeEnergy.update(activeAcPower);
		} else {
			// Charge
			this.calculateAcChargeEnergy.update(0);
			// this.calculateAcDischargeEnergy.update(0);
		}
		
		
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricEss.getModbusSlaveNatureTable(accessMode), //
				HybridEss.getModbusSlaveNatureTable(accessMode), //
				this.getModbusSlaveNatureTable(accessMode)
						
				);
	}

	public void handleSurplusPower() {
		// ToDo: Handle Power limitations from controllers
	}
	
	@Override
	public Integer getSurplusPower() {
		// TODO Auto-generated method stub
		return null;
	}	
	
	
	
}
