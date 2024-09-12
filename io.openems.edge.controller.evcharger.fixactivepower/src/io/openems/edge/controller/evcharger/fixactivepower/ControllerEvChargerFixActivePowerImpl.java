package io.openems.edge.controller.evcharger.fixactivepower;

import java.time.Clock;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcharger.api.ManagedEvCharger;
import io.openems.edge.evcharger.api.data.Phases;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.EvCharger.FixActivePower", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEvChargerFixActivePowerImpl extends AbstractOpenemsComponent
		implements ControllerEvChargerFixActivePower, Controller, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private ManagedEvCharger evCharger;

	private Config config;
	private Phases phases;

	public ControllerEvChargerFixActivePowerImpl() {
		this(Clock.systemDefaultZone());
	}

	protected ControllerEvChargerFixActivePowerImpl(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEvChargerFixActivePower.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "managedEvCharger",
				config.managedEvCharger_id())) {
			return;
		}
		this.phases = Phases.fromValue(this.config.maxUsablePhases());
		this.evCharger.setSessionEnergyLimit(this.config.energySessionLimit());
		this.evCharger.setOperationMode(this.config.operationMode());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.evCharger.setActivePowerPerPhase(this.config.activePowerPerPhase());
		this.evCharger.setLimitPhases(this.phases.getValue());
	}
}
