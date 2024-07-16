package io.openems.edge.pvinverter.sma;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
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
}
