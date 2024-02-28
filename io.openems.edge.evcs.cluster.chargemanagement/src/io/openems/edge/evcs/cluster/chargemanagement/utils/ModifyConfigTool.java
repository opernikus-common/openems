package io.openems.edge.evcs.cluster.chargemanagement.utils;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;

public class ModifyConfigTool {

	private static final String ALLOW_CHARGING = "allowCharging";

	/**
	 * Updates the configuration property "Allow charging".
	 *
	 * @param configAdmin      the configuration admin
	 * @param componentManager the component manager
	 * @param controllerId     the component id of the controller
	 * @param forceStateRed    whether or not to force state red, i.e. disallow
	 *                         charging
	 */
	public static void updateAllowCharging(ConfigurationAdmin configAdmin, ComponentManager componentManager,
			String controllerId, boolean forceStateRed) {
		OpenemsComponent component;
		try {
			component = componentManager.getComponent(controllerId);
		} catch (OpenemsNamedException e) {
			return;
		}
		var properties = component.getComponentContext().getProperties();

		// update component
		OpenemsComponent.updateConfigurationProperty(configAdmin, (String) properties.get(Constants.SERVICE_PID),
				ALLOW_CHARGING, forceStateRed);
	}

}
