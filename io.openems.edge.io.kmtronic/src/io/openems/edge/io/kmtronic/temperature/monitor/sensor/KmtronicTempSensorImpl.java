package io.openems.edge.io.kmtronic.temperature.monitor.sensor;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.kmtronic.temperature.monitor.core.KmtronicTempCore;
import io.openems.edge.thermometer.api.Thermometer;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.KMtronic.Temperature.Sensor", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)

public class KmtronicTempSensorImpl extends AbstractOpenemsComponent
		implements KmtronicTempSensor, Thermometer, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private KmtronicTempCore core;

	public KmtronicTempSensorImpl() {
		super(OpenemsComponent.ChannelId.values(), KmtronicTempSensor.ChannelId.values(),
				Thermometer.ChannelId.values());
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsException, OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.installListeners(config.sensorNumber() - 1);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void installListeners(int sensorNumber) {
		this.core.channel("IdSensor" + sensorNumber).onUpdate(newValue -> {
			this._setSensorId((String) newValue.get());
		});
		this.core.channel("NameSensor" + sensorNumber).onUpdate(newValue -> {
			this._setSensorName((String) newValue.get());
		});
		this.core.channel("TempSensor" + sensorNumber).onUpdate(newValue -> {
			try {
				var floatValue = (Float) newValue.get();
				Integer intValue = Math.round(floatValue * 10);
				this._setTemperature(intValue);
			} catch (Exception e) {
				this._setTemperature(null);
			}
		});
	}

}
