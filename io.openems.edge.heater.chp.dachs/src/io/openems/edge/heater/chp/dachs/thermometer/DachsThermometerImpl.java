package io.openems.edge.heater.chp.dachs.thermometer;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;
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

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heater.chp.dachs.DachsGlt;
import io.openems.edge.thermometer.api.Thermometer;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Heater.Chp.Dachs.Thermometer", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class DachsThermometerImpl extends AbstractOpenemsComponent implements Thermometer, OpenemsComponent {

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private DachsGlt parent;

	private ThermometerType type;

	public DachsThermometerImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				Thermometer.ChannelId.values());
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.type = config.type();
		this.addCopyListener();
	}

	private void addCopyListener() {
		this.parent.channel(this.type.copyChannel).onUpdate((value) -> {
			//safe casting with TypeUtils
			this._setTemperature(TypeUtils.getAsType(OpenemsType.INTEGER, value));
		});
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

}
