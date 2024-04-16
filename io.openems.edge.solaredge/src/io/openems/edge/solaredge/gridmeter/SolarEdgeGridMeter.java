package io.openems.edge.solaredge.gridmeter;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Interface for SolarEdge Grid Meter.
 * This interface defines various channels and methods to interact with a SolarEdge Grid Meter.
 */
public interface SolarEdgeGridMeter extends OpenemsComponent {

    /**
     * Enumeration of Channel IDs for SolarEdge Grid Meter.
     */
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Grid Mode.
         * Indicates whether the system is On-Grid or Off-Grid.
         * <ul>
         * <li>Interface: SolarEdgeGridMeter
         * <li>Type: Integer/Enum
         * <li>Range: 0=Undefined, 1=On-Grid, 2=Off-Grid
         * </ul>
         */
        POWER(Doc.of(OpenemsType.INTEGER)
                .unit(Unit.WATT)
                .persistencePriority(PersistencePriority.HIGH)),

        // Power channels for each phase
        POWER_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH)),
        POWER_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH)),
        POWER_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).persistencePriority(PersistencePriority.HIGH)),

        /**
         * Power Scale.
         * Used to calculate PV production scaling.
         * <ul>
         * <li>Interface: SolarEdgeGridMeter
         * <li>Type: Integer
         * <li>Unit: W
         * </ul>
         */
        POWER_SCALE(Doc.of(OpenemsType.INTEGER).persistencePriority(PersistencePriority.HIGH));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    // Method definitions for accessing channel values
    default Value<Integer> getPower() {
        return this.getPowerChannel().value();
    }

    default IntegerReadChannel getPowerChannel() {
        return this.channel(ChannelId.POWER);
    }

    default Value<Integer> getPowerL1() {
        return this.getPowerL1Channel().value();
    }

    default IntegerReadChannel getPowerL1Channel() {
        return this.channel(ChannelId.POWER_L1);
    }

    default Value<Integer> getPowerL2() {
        return this.getPowerL2Channel().value();
    }

    default IntegerReadChannel getPowerL2Channel() {
        return this.channel(ChannelId.POWER_L2);
    }

    default Value<Integer> getPowerL3() {
        return this.getPowerL3Channel().value();
    }

    default IntegerReadChannel getPowerL3Channel() {
        return this.channel(ChannelId.POWER_L3);
    }

    default Value<Integer> getPowerScale() {
        return this.getPowerScaleChannel().value();
    }

    default IntegerReadChannel getPowerScaleChannel() {
        return this.channel(ChannelId.POWER_SCALE);
    }
}
