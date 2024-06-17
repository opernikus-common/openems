package io.openems.edge.meter.api;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;

/**
 * The Nature of a GasMeter, it's an expansion of the existing Meter Interface.
 */
public interface GasMeterMbus extends MeterMbus {


    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Power.
         *
         * <ul>
         * <li>Interface: HeatMeterMbus
         * <li>Type: Integer
         * <li>Unit: Kilowatt
         * </ul>
         */
        READING_POWER(Doc.of(OpenemsType.LONG).unit(Unit.KILOWATT)),

        /**
         * The Percolation of the GasMeter.
         *
         * <ul>
         * <li>Interface: GasMeter
         * <li>Type: Integer
         * <li>Unit: CubicMeterPerSecond
         * </ul>
         */
        FLOW_RATE(Doc.of(OpenemsType.LONG).unit(Unit.CUBICMETER_PER_SECOND)),
        /**
         * Total Consumed Energy Cubic Meter.
         * <ul>
         *     <li>Interface: GasMeter
         *     <li>Type: Integer
         *     <li>Unit: CubicMeter
         * </ul>
         */
        TOTAL_CONSUMED_ENERGY_CUBIC_METER(Doc.of(OpenemsType.DOUBLE).unit(Unit.CUBIC_METER)),
        /**
         * Flow Temperature in Degree Celsius.
         * <ul>
         *     <li>Interface: GasMeter
         *     <li>Type: Integer
         *     <li>Unit: Degree Celsius
         * </ul>
         */
        FLOW_TEMP(Doc.of(OpenemsType.DOUBLE).unit(Unit.DEGREE_CELSIUS)),
        /**
         * Return Temperature in Degree Celsius.
         * <ul>
         *     <li>Interface: GasMeter
         *     <li>Type: Integer
         *     <li>Unit: Degree Celsius
         * </ul>
         */
        RETURN_TEMP(Doc.of(OpenemsType.DOUBLE).unit(Unit.DEGREE_CELSIUS));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }


    }

    /**
     * Gets the Percolation of the GasMeter.
     *
     * @return the Channel.
     */
    default Channel<Long> getFlowRateChannel() {
        return this.channel(ChannelId.FLOW_RATE);
    }

    /**
     * Gets the Total Consumed Energy.
     *
     * @return the Channel
     */
    default Channel<Double> getTotalConsumedEnergyCubicMeterChannel() {
        return this.channel(ChannelId.TOTAL_CONSUMED_ENERGY_CUBIC_METER);
    }

    /**
     * Gets the Flow Temperature Channel.
     *
     * @return the Channel
     */
    default Channel<Double> getFlowTempChannel() {
        return this.channel(ChannelId.FLOW_TEMP);
    }

    /**
     * Gets the return Temperature Channel.
     *
     * @return the Channel.
     */
    default Channel<Double> getReturnTemp() {
        return this.channel(ChannelId.RETURN_TEMP);
    }

    /**
     * Gets the POWER Channel of this Meter.
     *
     * @return the Channel
     */
    default Channel<Double> getPowerChannel() {
        return this.channel(ChannelId.READING_POWER);
    }


}