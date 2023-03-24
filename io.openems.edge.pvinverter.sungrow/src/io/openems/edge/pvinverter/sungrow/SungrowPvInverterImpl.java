package io.openems.edge.pvinverter.sungrow;

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
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
	name = "PV-Inverter.Sungrow", //
	immediate = true, //
	configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SungrowPvInverterImpl extends AbstractOpenemsModbusComponent implements ManagedSymmetricPvInverter,
	SymmetricMeter, AsymmetricMeter, SungrowPvInverter, ModbusComponent, OpenemsComponent {

    private static final ElementToChannelConverter ON_OFF_CONVERTER = new ElementToChannelConverter((value) -> {
	if ((Integer) value == 0xAA) {
	    return true;
	}
	return false;
    }, (value) -> {
	if ((Boolean) value) {
	    return 0xAA;
	}
	return 0xEE;
    });

    public SungrowPvInverterImpl() {
	super(//
		OpenemsComponent.ChannelId.values(), //
		ModbusComponent.ChannelId.values(), //
		ManagedSymmetricPvInverter.ChannelId.values(), //
		SymmetricMeter.ChannelId.values(), //
		AsymmetricMeter.ChannelId.values(), //
		SungrowPvInverter.ChannelId.values() //
	);
    }

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
	super.setModbus(modbus);
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsException {
	if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
		"Modbus", config.modbus_id())) {
	    return;
	}
    }

    @Deactivate
    protected void deactivate() {
	super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
	
	return new ModbusProtocol(this, new FC4ReadInputRegistersTask(4989, Priority.HIGH, //
		m(SungrowPvInverter.ChannelId.SERIAL_NUMBER, new StringWordElement(4989, 10)), //
		new DummyRegisterElement(4999), // Device Type Code
		m(SungrowPvInverter.ChannelId.NOMINAL_OUTPUT_POWER, new UnsignedWordElement(5000), //
			ElementToChannelConverter.SCALE_FACTOR_2), //
		new DummyRegisterElement(5001), // Outout type
		m(SungrowPvInverter.ChannelId.DAILY_ENERGY, new UnsignedWordElement(5002), //
			ElementToChannelConverter.SCALE_FACTOR_2), //
		m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY,
			new UnsignedDoublewordElement(5003).wordOrder(WordOrder.LSWMSW), //
			ElementToChannelConverter.SCALE_FACTOR_3), //
		m(SungrowPvInverter.ChannelId.TOTAL_RUNNING_TIME,
			new UnsignedDoublewordElement(5005).wordOrder(WordOrder.LSWMSW)), //
		m(SungrowPvInverter.ChannelId.INTERNAL_TEMPERATURE, new SignedWordElement(5007)), //
		m(SungrowPvInverter.ChannelId.APPARENT_POWER,
			new UnsignedDoublewordElement(5008).wordOrder(WordOrder.LSWMSW)), //
		m(SungrowPvInverter.ChannelId.DC_VOLTAGE_1, new UnsignedWordElement(5010), //
			ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
		m(SungrowPvInverter.ChannelId.DC_CURRENT_1, new UnsignedWordElement(5011), //
			ElementToChannelConverter.SCALE_FACTOR_2), //
		m(SungrowPvInverter.ChannelId.DC_VOLTAGE_2, new UnsignedWordElement(5012), //
			ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
		m(SungrowPvInverter.ChannelId.DC_CURRENT_2, new UnsignedWordElement(5013), //
			ElementToChannelConverter.SCALE_FACTOR_2), //
		m(SungrowPvInverter.ChannelId.DC_VOLTAGE_3, new UnsignedWordElement(5014), //
			ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
		m(SungrowPvInverter.ChannelId.DC_CURRENT_3, new UnsignedWordElement(5015), //
			ElementToChannelConverter.SCALE_FACTOR_2), //
		m(SungrowPvInverter.ChannelId.DC_POWER,
			new UnsignedDoublewordElement(5016).wordOrder(WordOrder.LSWMSW)), //
		m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(5018), //
			ElementToChannelConverter.SCALE_FACTOR_2), //
		m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(5019), //
			ElementToChannelConverter.SCALE_FACTOR_2), //
		m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(5020), //
			ElementToChannelConverter.SCALE_FACTOR_2), //
		m(AsymmetricMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(5021), //
			ElementToChannelConverter.SCALE_FACTOR_2), //
		m(AsymmetricMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(5022), //
			ElementToChannelConverter.SCALE_FACTOR_2), //
		m(AsymmetricMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(5023), //
			ElementToChannelConverter.SCALE_FACTOR_2), //
		new DummyRegisterElement(5024, 5029), // Reserved
		m(SymmetricMeter.ChannelId.ACTIVE_POWER,
			new UnsignedDoublewordElement(5030).wordOrder(WordOrder.LSWMSW)), //
		m(SymmetricMeter.ChannelId.REACTIVE_POWER,
			new SignedDoublewordElement(5032).wordOrder(WordOrder.LSWMSW)), //
		m(SungrowPvInverter.ChannelId.POWER_FACTOR, new SignedWordElement(5034)), //
		new DummyRegisterElement(5035, 5036), // 5035: Frequency, read from another register, 5036: reserved
		m(SungrowPvInverter.ChannelId.WORK_STATE, new UnsignedWordElement(5037)) //
	),

		new FC4ReadInputRegistersTask(5145, Priority.LOW, //
			m(SungrowPvInverter.ChannelId.NEGATIVE_VOLTAGE_TO_THE_GROUND, new SignedWordElement(5145), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
			m(SungrowPvInverter.ChannelId.BUS_VOLTAGE, new UnsignedWordElement(5146), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
			m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedWordElement(5147), //
				ElementToChannelConverter.SCALE_FACTOR_1) //
		),

		new FC3ReadRegistersTask(5006, Priority.LOW, //
			m(SungrowPvInverter.ChannelId.POWER_LIMITATION_SWITCH, new UnsignedWordElement(5006), //
				ON_OFF_CONVERTER), //
			m(SungrowPvInverter.ChannelId.POWER_LIMITATION_SETTING, new UnsignedWordElement(5007), //
				ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //

		new FC3ReadRegistersTask(5038, Priority.HIGH, //
			m(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, new UnsignedWordElement(5038), //
				ElementToChannelConverter.SCALE_FACTOR_2)), //

		new FC6WriteRegisterTask(5038, m(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT, //
			new UnsignedWordElement(5038), ElementToChannelConverter.SCALE_FACTOR_2)));
    }

    @Override
    public String debugLog() {
	return "L:" + this.getActivePower().asString();
    }
}
