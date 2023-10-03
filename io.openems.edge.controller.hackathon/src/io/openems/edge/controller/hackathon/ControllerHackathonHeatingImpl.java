package io.openems.edge.controller.hackathon;

import io.openems.edge.io.api.DigitalOutput;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.thermometer.api.Thermometer;

import java.util.Arrays;

/**
 * Simple Example of a Controller. Without much User input error testing etc.
 * When Temperature is below certain Threshold -> set DigitalOutputs to true
 * When reaching an upper threshold -> set Outputs to false. Very Simple. Just a
 * showcase.
 */

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Hackathon.Heating", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerHackathonHeatingImpl extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private Thermometer thermometer;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private DigitalOutput relay;

	private Config config;

	private boolean digitalOutValue = false;

	public ControllerHackathonHeatingImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "thermometer",
				config.thermometer_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "relay", config.relay_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsError.OpenemsNamedException {

		this.thermometer.getTemperature().ifPresent(this::setRelayStateDependingOnTemperature);

		Arrays.stream(this.relay.digitalOutputChannels()).forEach(channel -> {
			try {
				channel.setNextWriteValueFromObject(this.digitalOutValue);
			} catch (OpenemsError.OpenemsNamedException ignored) {
				// ignored, you should write a custom StateChannel and set the error here.
			}
		});
	}

	private void setRelayStateDependingOnTemperature(Integer value) {
		if (value <= this.config.enable_temperature()) {
			this.digitalOutValue = true;
		}
		if (value >= this.config.disable_temperature()) {
			this.digitalOutValue = false;
		}
	}
}
