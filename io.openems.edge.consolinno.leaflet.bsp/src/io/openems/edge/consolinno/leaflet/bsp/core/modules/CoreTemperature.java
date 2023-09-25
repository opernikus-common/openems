package io.openems.edge.consolinno.leaflet.bsp.core.modules;

import io.openems.edge.consolinno.leaflet.bsp.core.LeafletCore;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;

public interface CoreTemperature extends OpenemsComponent {
    /**
     * Return channel for the detected Temperature Modules.
     *
     * @return Integer TemperatureModules Channel
     */
    default Channel<Integer> getTemperatureModulesChannel() {
	return this.channel(LeafletCore.ChannelId.TEMPERATURE_MODULES);
    }

    /**
     * Return detected Temperature Modules.
     *
     * @return Integer of detected Temperature Modules
     */
    default int getTemperatureModules() {
	if (this.getTemperatureModulesChannel().value().isDefined()) {
	    return this.getTemperatureModulesChannel().value().get();
	} else if (this.getTemperatureModulesChannel().getNextValue().isDefined()) {
	    return this.getTemperatureModulesChannel().getNextValue().get();
	} else {
	    return -1;
	}
    }

}
