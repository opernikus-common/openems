package io.openems.edge.consolinno.leaflet.bsp.core.modules;

import io.openems.edge.consolinno.leaflet.bsp.core.LeafletCore;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface CoreRelais extends OpenemsComponent {

    /**
     * Invert the Relay Functionality.
     *
     * @param moduleNumber Module Number specified on the Relay module
     * @param position     Position of the Relay on the Module
     */
    void invertRelay(int moduleNumber, int position);

    /**
     * Puts a relay back in regular mode. Is Called when a inverted Relay
     * deactivates.
     *
     * @param moduleNumber Module number specified on the Device
     * @param position     Position of the Relay on the module
     */
    void revertInversion(int moduleNumber, int position);

    /**
     * Return channel for the detected Relay Modules.
     *
     * @return Integer RelayModules Channel
     */
    default Channel<Integer> getRelayModulesChannel() {
	return this.channel(LeafletCore.ChannelId.RELAY_MODULES);
    }

    /**
     * Return detected Relay Modules.
     *
     * @return Integer of detected Relay Modules
     */
    default int getRelayModules() {
	if (this.getRelayModulesChannel().value().isDefined()) {
	    return this.getRelayModulesChannel().value().get();
	} else if (this.getRelayModulesChannel().getNextValue().isDefined()) {
	    return this.getRelayModulesChannel().getNextValue().get();
	} else {
	    return -1;
	}
    }

    /**
     * Return WriteChannel for the Inversion Configuration of the first Relay
     * Module.
     *
     * @return Integer WriteInvertRelayOneStatus WriteChannel
     */
    default WriteChannel<Integer> getWriteInvertRelayOneStatus() {
	return this.channel(LeafletCore.ChannelId.WRITE_RELAY_ONE_INVERT_STATUS);
    }

    /**
     * Return WriteChannel for the Inversion Configuration of the second Relay
     * Module.
     *
     * @return Integer WriteInvertRelayTwoStatus WriteChannel
     */
    default WriteChannel<Integer> getWriteInvertRelayTwoStatus() {
	return this.channel(LeafletCore.ChannelId.WRITE_RELAY_TWO_INVERT_STATUS);
    }

    /**
     * Return WriteChannel for the Inversion Configuration of the third Relay
     * Module.
     *
     * @return Integer WriteInvertRelayThreeStatus WriteChannel
     */
    default WriteChannel<Integer> getWriteInvertRelayThreeStatus() {
	return this.channel(LeafletCore.ChannelId.WRITE_RELAY_THREE_INVERT_STATUS);
    }

    /**
     * Return WriteChannel for the Inversion Configuration of the forth Relay
     * Module.
     *
     * @return Integer WriteInvertRelayFourStatus WriteChannel
     */
    default WriteChannel<Integer> getWriteInvertRelayFourStatus() {
	return this.channel(LeafletCore.ChannelId.WRITE_RELAY_FOUR_INVERT_STATUS);
    }

}
