package io.openems.edge.heater.chp.dachs.meter;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.heater.chp.dachs.DachsGlt;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Heater.Chp.Dachs.Meter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class DachsVirtualMeterThreephaseImpl extends AbstractOpenemsComponent
		implements AsymmetricMeter, SymmetricMeter, OpenemsComponent {

	protected Config config = null;

	private final Logger log = LoggerFactory.getLogger(DachsVirtualMeterThreephaseImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private DachsGlt dachs;

	public DachsVirtualMeterThreephaseImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException, OpenemsNamedException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!this.config.enabled()) {
			return;
		}

		this.mapEnergyAndPower();
		this.logInfo(this.log, "Activated");
	}

	@Deactivate
	protected void deactivate() {
		this.logInfo(this.log, "deactivation");
		super.deactivate();
	}

	private void mapEnergyAndPower() throws OpenemsException {
		this.dachs.getFlowTemperatureChannel().onUpdate((newValue) -> {
			this.updatePowerChannel(newValue);
		});

		this.dachs.channel(DachsGlt.ChannelId.ELECTRICAL_WORK).onUpdate((newValue) -> {
			Double dvalue = TypeUtils.getAsType(OpenemsType.DOUBLE, newValue);
			if (dvalue == null) {
				this.getActiveProductionEnergyChannel().setNextValue(null);
				return;
			}
			Long l = (long) (dvalue * 1000);
			this.getActiveProductionEnergyChannel().setNextValue(l);
		});

		this.getReactivePowerChannel().setNextValue(null);
		this.getReactivePowerL1Channel().setNextValue(null);
		this.getReactivePowerL2Channel().setNextValue(null);
		this.getReactivePowerL3Channel().setNextValue(null);
	}

	protected void updatePowerChannel(Value<Integer> newValue) {
		Integer value = TypeUtils.getAsType(OpenemsType.INTEGER, newValue);
		if (value == null) {
			this.getActivePowerChannel().setNextValue(null);
			return;
		}
		this.getActivePowerChannel().setNextValue(value);
		value /= 3;
		this.getActivePowerL1Channel().setNextValue(value);
		this.getActivePowerL2Channel().setNextValue(value);
		this.getActivePowerL3Channel().setNextValue(value);
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	public String debugLog() {
		int power = this.getActivePowerChannel().value().asOptional().orElse(0);
		return (power) + " W," + this.getMeterType().toString();
	}

}
