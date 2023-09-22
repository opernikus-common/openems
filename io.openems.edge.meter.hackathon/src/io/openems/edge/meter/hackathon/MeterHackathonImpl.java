package io.openems.edge.meter.hackathon;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Hackathon", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterHackathonImpl extends AbstractOpenemsModbusComponent
		implements ElectricityMeter, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	public MeterHackathonImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values() //
		);

		// Automatically calculate sum values from L1/L2/L3
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, new FC4ReadInputRegistersTask(602, Priority.LOW, //
				m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(602))), //
				new FC4ReadInputRegistersTask(608, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(608)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(610))), //
				new FC4ReadInputRegistersTask(612, Priority.LOW, //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new FloatDoublewordElement(612)), //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(614)) //
				// following 2 channel not needed since it's calculated by callback
				// m(ElectricityMeter.ChannelId.VOLTAGE, new FloatDoublewordElement(616)),
				// m(ElectricityMeter.ChannelId.CURRENT, new FloatDoublewordElement(618)),
				), new FC4ReadInputRegistersTask(702, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(702)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(704)), //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(706)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(708)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(710)), //
						m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(712))), //
				new FC4ReadInputRegistersTask(714, Priority.LOW, //
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(714)), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(716)), //
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(718)), //
						m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(720)), //
						m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(722)), //
						m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(724)), //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, new FloatDoublewordElement(726)), //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, new FloatDoublewordElement(728)), //
						m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, new FloatDoublewordElement(730)), //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, new FloatDoublewordElement(732)), //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, new FloatDoublewordElement(734)), //
						m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, new FloatDoublewordElement(736))) //
		);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}
}
