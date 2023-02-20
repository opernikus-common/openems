package io.openems.edge.heater.chp.dachs.meter;

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
		this.dachs.getElectricProductionPowerChannel().onUpdate(this::updatePowerChannel);

		this.dachs.channel(DachsGlt.ChannelId.ELECTRICAL_WORK).onUpdate((newValue) -> {
			Double dValue = TypeUtils.getAsType(OpenemsType.DOUBLE, newValue);
			this.getActiveProductionEnergyChannel().setNextValue(dValue == null ? null : (long) (dValue * 1000));
		});
	}

	protected void updatePowerChannel(Value<Integer> newValue) {
		this._setActivePower(newValue.get());
		newValue.asOptional().ifPresentOrElse(value -> {
			value /= 3;
			this._setActivePowerL1(value);
			this._setActivePowerL2(value);
			this._setActivePowerL3(value);
		}, () -> {
			this._setActivePowerL1(null);
			this._setActivePowerL2(null);
			this._setActivePowerL3(null);
		});
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
