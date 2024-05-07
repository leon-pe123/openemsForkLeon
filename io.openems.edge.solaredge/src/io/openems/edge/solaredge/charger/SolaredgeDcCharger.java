package io.openems.edge.solaredge.charger;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.solaredge.enums.ActiveInactive;

public interface SolaredgeDcCharger extends EssDcCharger, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Current from or to the battery on the DC side.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: Ampere
		 * </ul>
		 */
		CURRENT_BATT_DC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Scale factor for the current generated by the PV system.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: Factor
		 * </ul>
		 */
		CURRENT_DC_SCALE(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Current generated by the PV system.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: Ampere
		 * </ul>
		 */
		CURRENT_DC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * DC-Discharge Power.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>This is the instantaneous power to or from the battery
		 * </ul>
		 */
		DC_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Debug channel to set the PV power limit.
		 */
		DEBUG_SET_PV_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),

		/**
		 * Grid-Mode.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer/Enum
		 * <li>Range: 0=Undefined, 1=On-Grid, 2=Off-Grid
		 * </ul>
		 */
		GRID_MODE(Doc.of(GridMode.values()) //
				.persistencePriority(PersistencePriority.HIGH) //
		),

		/**
		 * AdvancedPowerControl. Used to limit inverter output
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer/Enum
		 * <li>Range: 0=Inactive, 1=Active
		 * </ul>
		 */
		POWER_CONTROL_ENABLED(Doc.of(ActiveInactive.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
		),

		/**
		 * Stores the total consumption energy value at system startup.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Long
		 * <li>Unit: Cumulated Watt Hours
		 * </ul>
		 */
		ORIGINAL_ACTIVE_CONSUMPTION_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS)),

		/**
		 * Stores the total production energy value at system startup.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Long
		 * <li>Unit: Cumulated Watt Hours
		 * </ul>
		 */
		ORIGINAL_ACTIVE_PRODUCTION_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS)),

		/**
		 * Power generated by the PV system.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: Watt
		 * </ul>
		 */
		POWER_DC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Scale factor for the power generated by the PV system.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: Factor
		 * </ul>
		 */
		POWER_DC_SCALE(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Production Power.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>This is the AC-Production Power coming out of the Inverter
		 * 
		 * It should be ACTUAL_POWER (PV Production Power) minus
		 * DC_DISCHARGE_POWER/"instantaneous_power" (+/-)
		 * </ul>
		 */
		PRODUCTION_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Limits PV power generation. Note: This channel is shared between multiple
		 * chargers. Use should be coordinated accordingly.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: Watt
		 * </ul>
		 */
		SET_PV_POWER_LIMIT(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_SET_PV_POWER_LIMIT)),

		/**
		 * Active Power Limit in Percent
		 * 
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: Percent
		 * </ul>
		 */
		SET_PV_POWER_LIMIT_PERCENT(new IntegerDoc() //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.WRITE_ONLY)),

		/**
		 * Commits the active power limit. Needs to be set after changing new power
		 * limit
		 * 
		 * 1 - Commit
		 * 
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: NONE
		 * </ul>
		 */
		COMMIT_PV_POWER_LIMIT(new IntegerDoc() //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.WRITE_ONLY)),

		/**
		 * Current configured active power limit
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: Watt
		 * </ul>
		 */
		MAX_ACTIVE_PV_POWER_LIMIT(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Current configured reactive power limit
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: VAR
		 * </ul>
		 */
		MAX_REACTIVE_PV_POWER_LIMIT(new IntegerDoc() //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Limits PV power generation. Note: This channel is shared between multiple
		 * chargers. Use should be coordinated accordingly.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: Watt
		 * </ul>
		 */
		ACTIVE_PV_ACTIVE_POWER_LIMIT(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Limits PV power generation. Note: This channel is shared between multiple
		 * chargers. Use should be coordinated accordingly.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: var
		 * </ul>
		 */
		ACTIVE_PV_REACTIVE_POWER_LIMIT(new IntegerDoc() //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Active Power Limit in Percent
		 * 
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: Percent
		 * </ul>
		 */
		ACTIVE_PV_POWER_LIMIT_PERCENT(new IntegerDoc() //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Voltage from the battery.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: Volt
		 * </ul>
		 */
		VOLTAGE_BATT_DC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Voltage output from the PV system.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: Volt
		 * </ul>
		 */
		VOLTAGE_DC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Scale-factor for DC voltage.
		 *
		 * <ul>
		 * <li>Interface: SolaredgeDcCharger
		 * <li>Type: Integer
		 * <li>Unit: Factor
		 * </ul>
		 */
		VOLTAGE_DC_SCALE(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#BATT_DC_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBattDcCurrentChannel() {
		return this.channel(ChannelId.CURRENT_BATT_DC);
	}

	/**
	 * Gets the Battery DC Current in Ampere.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBattDcCurrent() {
		return this.getBattDcCurrentChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#BATT_DC_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBattDcVoltageChannel() {
		return this.channel(ChannelId.VOLTAGE_BATT_DC);
	}

	/**
	 * Gets the Battery DC Voltage in Volts.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBattDcVoltage() {
		return this.getBattDcVoltageChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#DC_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcCurrentChannel() {
		return this.channel(ChannelId.CURRENT_DC);
	}

	/**
	 * Gets the DC Current generated by the PV system in Ampere.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcCurrent() {
		return this.getDcCurrentChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#DC_CURRENT_SCALE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcCurrentScaleChannel() {
		return this.channel(ChannelId.CURRENT_DC_SCALE);
	}

	/**
	 * Gets the Scale factor for DC Current generated by the PV system.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcCurrentScale() {
		return this.getDcCurrentScaleChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcDischargePowerChannel() {
		return this.channel(ChannelId.DC_DISCHARGE_POWER);
	}

	/**
	 * Gets the DC Discharge Power in Watts. Negative values for Charge; positive
	 * for Discharge.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcDischargePower() {
		return this.getDcDischargePowerChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#DC_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPowerChannel() {
		return this.channel(ChannelId.POWER_DC);
	}

	/**
	 * Gets the DC Power generated by the PV system in Watts.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcPower() {
		return this.getDcPowerChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#DC_POWER_SCALE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPowerScaleChannel() {
		return this.channel(ChannelId.POWER_DC_SCALE);
	}

	/**
	 * Gets the Scale factor for DC Power generated by the PV system.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcPowerScale() {
		return this.getDcPowerScaleChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#DC_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcVoltageChannel() {
		return this.channel(ChannelId.VOLTAGE_DC);
	}

	/**
	 * Gets the DC Voltage output from the PV system in Volts.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcVoltage() {
		return this.getDcVoltageChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#DC_VOLTAGE_SCALE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcVoltageScaleChannel() {
		return this.channel(ChannelId.VOLTAGE_DC_SCALE);
	}

	/**
	 * Gets the Scale factor for DC Voltage output from the PV system.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcVoltageScale() {
		return this.getDcVoltageScaleChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#ORIGINAL_ACTIVE_PRODUCTION_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getOriginalActiveProductionEnergyChannel() {
		return this.channel(ChannelId.ORIGINAL_ACTIVE_PRODUCTION_ENERGY);
	}

	/**
	 * Gets the total production energy value stored at system startup in Cumulated
	 * Watt Hours.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getOriginalActiveProductionEnergy() {
		return this.getOriginalActiveProductionEnergyChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#PRODUCTION_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getProductionPowerChannel() {
		return this.channel(ChannelId.PRODUCTION_POWER);
	}

	/**
	 * Gets the Production Power in Watts. This is the power delivered by the
	 * inverter.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getProductionPower() {
		return this.getProductionPowerChannel().value();
	}

	// Access the channel for MAX_ACTIVE_PV_POWER_LIMIT
	public default IntegerReadChannel getMaxActivePvPowerLimitChannel() {
		return this.channel(ChannelId.MAX_ACTIVE_PV_POWER_LIMIT);
	}

	// Getter for MAX_ACTIVE_PV_POWER_LIMIT
	public default Value<Integer> getMaxActivePvPowerLimit() {
		return this.getMaxActivePvPowerLimitChannel().value();
	}

	// Access the channel for MAX_REACTIVE_PV_POWER_LIMIT
	public default IntegerReadChannel getMaxReactivePvPowerLimitChannel() {
		return this.channel(ChannelId.MAX_REACTIVE_PV_POWER_LIMIT);
	}

	// Getter for MAX_REACTIVE_PV_POWER_LIMIT
	public default Value<Integer> getMaxReactivePvPowerLimit() {
		return this.getMaxReactivePvPowerLimitChannel().value();
	}

	// Access the channel for ACTIVE_PV_ACTIVE_POWER_LIMIT
	public default IntegerReadChannel getActivePvActivePowerLimitChannel() {
		return this.channel(ChannelId.ACTIVE_PV_ACTIVE_POWER_LIMIT);
	}

	// Getter for ACTIVE_PV_ACTIVE_POWER_LIMIT
	public default Value<Integer> getActivePvActivePowerLimit() {
		return this.getActivePvActivePowerLimitChannel().value();
	}

	// Access the channel for ACTIVE_PV_REACTIVE_POWER_LIMIT
	public default IntegerReadChannel getActivePvReactivePowerLimitChannel() {
		return this.channel(ChannelId.ACTIVE_PV_REACTIVE_POWER_LIMIT);
	}

	// Getter for ACTIVE_PV_ACTIVE_POWER_LIMIT
	public default Value<Integer> getActivePvReactivePowerLimit() {
		return this.getActivePvReactivePowerLimitChannel().value();
	}

	// ACTIVE_PV_POWER_LIMIT_PERCENT
	public default IntegerReadChannel getActivePvPowerLimitPercentChannel() {
		return this.channel(ChannelId.ACTIVE_PV_POWER_LIMIT_PERCENT);
	}

	// ACTIVE_PV_POWER_LIMIT_PERCENT
	public default Value<Integer> getActivePvPowerLimitPercent() {
		return this.getActivePvPowerLimitPercentChannel().value();
	}

	// SET_PV_POWER_LIMIT
	public default IntegerWriteChannel getPvPowerLimitChannel() {
		return this.channel(ChannelId.SET_PV_POWER_LIMIT);
	}

	// SET_PV_POWER_LIMIT
	public default void setPvPowerLimit(Integer value) {
		this.getPvPowerLimitChannel().setNextValue(value);
	}

	// Access the channel for SET_PV_POWER_LIMIT_PERCENT
	public default IntegerWriteChannel getPvPowerLimitPercentChannel() {
		return this.channel(ChannelId.SET_PV_POWER_LIMIT_PERCENT);
	}

	// Setter for SET_PV_POWER_LIMIT_PERCENT
	public default void setPvPowerLimitPercent(Integer value) throws OpenemsNamedException {
		this.getPvPowerLimitPercentChannel().setNextWriteValue(value);
	}

	// COMMIT_PV_POWER_LIMIT
	public default IntegerWriteChannel getCommitPvPowerLimitChannel() {
		return this.channel(ChannelId.COMMIT_PV_POWER_LIMIT);
	}

	// COMMIT_PV_POWER_LIMIT
	public default void commitPvPowerLimit(Integer value) throws OpenemsNamedException {
		this.getCommitPvPowerLimitChannel().setNextWriteValue(value);
	}

	// Access the channel for POWER_CONTROL_ENABLED correctly
	public default EnumWriteChannel getSetPvPowerControlModeChannel() {
		return this.channel(ChannelId.POWER_CONTROL_ENABLED);
	}

	// Retrieve the current mode of POWER_CONTROL_ENABLED as ActiveInactive enum
	public default ActiveInactive getSetPvPowerControlMode() {
		return this.getSetPvPowerControlModeChannel().value().asEnum();
	}

	// Setter for POWER_CONTROL_ENABLED
	public default void setPvPowerControlMode(ActiveInactive value) throws OpenemsNamedException {
		this.getSetPvPowerControlModeChannel().setNextWriteValue(value);
	}

	/**
	 * Sets Limits for PV-Production. The limitation refers to AC-side
	 * (PV-production + DC-Charging may exeed this value)
	 */
	void _calculateAndSetPvPowerLimit(int maxPvPower);

}
