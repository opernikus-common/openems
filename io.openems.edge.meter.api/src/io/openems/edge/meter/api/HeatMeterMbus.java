package io.openems.edge.meter.api;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;

/**
 * A HeatMeter, an expansion of the Meter interface.
 * It stores the Power Reading and the Energy, as well as a returnTemp.
 */
public interface HeatMeterMbus extends MeterMbus {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Reading Power of the HeatMeter.
         * <ul>
         * <li> Interface: HeatMeter
         * <li> Type: Long
         * <li> Unit: Kilowatt
         * </ul>
         */
        READING_POWER(Doc.of(OpenemsType.LONG).unit(Unit.KILOWATT)),

        /**
         * Total Consumed Energy.
         *
         * <ul>
         * <li>Interface: HeatMeter
         * <li>Type: Long
         * <li>Unit: WattHours
         * </ul>
         */
        READING_ENERGY(Doc.of(OpenemsType.LONG).unit(Unit.KILOWATT_HOURS)),

        /**
         * Return Temp.
         *
         * <ul>
         * <li>Interface: HeatMeter
         * <li>Type: Long
         * <li>Unit: DegreeCelsius
         * </ul>
         */
        RETURN_TEMP(Doc.of(OpenemsType.LONG).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Flow Temp.
         *
         * <ul>
         * <li>Interface: HeatMeter
         * <li>Type: Long
         * <li>Unit: DecidegreeCelsius
         * </ul>
         */
        FLOW_TEMP(Doc.of(OpenemsType.LONG).unit(Unit.DEZIDEGREE_CELSIUS)),

        /**
         * Flow Rate.
         *
         * <ul>
         * <li>Interface: HeatMeter
         * <li>Type: Float
         * <li>Unit: CubicMeterPerHour
         * </ul>
         */
        FLOW_RATE(Doc.of(OpenemsType.DOUBLE).unit(Unit.CUBICMETER_PER_HOUR));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }


    }

    /**
     * Get the Return Temp Channel of this HeatMeter.
     *
     * @return the Channel
     */
    default Channel<Long> getReturnTempChannel() {
        return this.channel(ChannelId.RETURN_TEMP);
    }

    /**
     * Get the Flow Temp Channel of this HeatMeter.
     *
     * @return the Channel
     */
    default Channel<Long> getFlowTempChannel() {
        return this.channel(ChannelId.FLOW_TEMP);
    }


    /**
     * Get the Reading channel of this HeatMeter.
     *
     * @return the Channel.
     */
    default Channel<Long> getReadingPowerChannel() {
        return this.channel(ChannelId.READING_POWER);
    }

    /**
     * Gets the Total Consumed Energy Channel of this Meter.
     *
     * @return the Channel.
     */
    default Channel<Long> getReadingEnergyChannel() {
        return this.channel(ChannelId.READING_ENERGY);
    }


}
