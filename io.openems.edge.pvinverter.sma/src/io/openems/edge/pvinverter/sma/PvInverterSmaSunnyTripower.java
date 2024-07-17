package io.openems.edge.pvinverter.sma;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.pvinverter.sunspec.SunSpecPvInverter;

public interface PvInverterSmaSunnyTripower extends SunSpecPvInverter, ManagedSymmetricPvInverter, ElectricityMeter,
		ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * String 1 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST1_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 2 DC-Current.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: A
		 * <li>
		 * </ul>
		 */
		ST2_DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 1 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST1_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 2 DC-Energy.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: WattHours
		 * <li>
		 * </ul>
		 */
		ST2_DC_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 1 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST1_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 2 DC-Power.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		ST2_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 1 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST1_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * String 2 DC-Voltage.
		 *
		 * <ul>
		 * <li>Interface: PvInverter
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		ST2_DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),

		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public default IntegerReadChannel getSt1DcCurrentChannel() {
		return this.channel(ChannelId.ST1_DC_CURRENT);
	}

	public default Value<Integer> getSt1DcCurrent() {
		return this.getSt1DcCurrentChannel().value();
	}

	public default void _setSt1DcCurrent(int value) {
		this.getSt1DcCurrentChannel().setNextValue(value);
	}

	public default IntegerReadChannel getSt2DcCurrentChannel() {
		return this.channel(ChannelId.ST2_DC_CURRENT);
	}

	public default Value<Integer> getSt2DcCurrent() {
		return this.getSt2DcCurrentChannel().value();
	}

	public default void _setSt2DcCurrent(int value) {
		this.getSt2DcCurrentChannel().setNextValue(value);
	}

	public default IntegerReadChannel getSt1DcEnergyChannel() {
		return this.channel(ChannelId.ST1_DC_ENERGY);
	}

	public default Value<Integer> getSt1DcEnergy() {
		return this.getSt1DcEnergyChannel().value();
	}

	public default void _setSt1DcEnergy(int value) {
		this.getSt1DcEnergyChannel().setNextValue(value);
	}

	public default IntegerReadChannel getSt2DcEnergyChannel() {
		return this.channel(ChannelId.ST2_DC_ENERGY);
	}

	public default Value<Integer> getSt2DcEnergy() {
		return this.getSt2DcEnergyChannel().value();
	}

	public default void _setSt2DcEnergy(int value) {
		this.getSt2DcEnergyChannel().setNextValue(value);
	}

	public default IntegerReadChannel getSt1DcPowerChannel() {
		return this.channel(ChannelId.ST1_DC_POWER);
	}

	public default Value<Integer> getSt1DcPower() {
		return this.getSt1DcPowerChannel().value();
	}

	public default void _setSt1DcPower(int value) {
		this.getSt1DcPowerChannel().setNextValue(value);
	}

	public default IntegerReadChannel getSt2DcPowerChannel() {
		return this.channel(ChannelId.ST2_DC_POWER);
	}

	public default Value<Integer> getSt2DcPower() {
		return this.getSt2DcPowerChannel().value();
	}

	public default void _setSt2DcPower(int value) {
		this.getSt2DcPowerChannel().setNextValue(value);
	}

	public default IntegerReadChannel getSt1DcVoltageChannel() {
		return this.channel(ChannelId.ST1_DC_VOLTAGE);
	}

	public default Value<Integer> getSt1DcVoltage() {
		return this.getSt1DcVoltageChannel().value();
	}

	public default void _setSt1DcVoltage(int value) {
		this.getSt1DcVoltageChannel().setNextValue(value);
	}

	public default IntegerReadChannel getSt2DcVoltageChannel() {
		return this.channel(ChannelId.ST2_DC_VOLTAGE);
	}

	public default Value<Integer> getSt2DcVoltage() {
		return this.getSt2DcVoltageChannel().value();
	}

	public default void _setSt2DcVoltage(int value) {
		this.getSt2DcVoltageChannel().setNextValue(value);
	}
}
