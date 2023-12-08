package io.openems.edge.meter.gmc.em2389;

import java.util.function.Consumer;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ElementToChannelScaleFactorConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.GMC.EM2389", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterGmcEm2389Impl extends AbstractOpenemsModbusComponent
		implements MeterGmcEm2389, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(MeterGmcEm2389Impl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	public MeterGmcEm2389Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterGmcEm2389.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		if (config.alternativePowerCalculation()) {
			this.installPowerListener();
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

	private void installPowerListener() {
		final Consumer<Value<Integer>> powerSum = ignore -> {
			this._setActivePower(TypeUtils.sum(//
					TypeUtils.multiply(//
							TypeUtils.divide(this.getCurrentL1Channel().getNextValue().get(), 1000), //
							TypeUtils.divide(this.getVoltageL1Channel().getNextValue().get(), 1000), //
							this.getSignL1().orElse(1)), //
					TypeUtils.multiply(//
							TypeUtils.divide(this.getCurrentL2Channel().getNextValue().get(), 1000), //
							TypeUtils.divide(this.getVoltageL2Channel().getNextValue().get(), 1000), //
							this.getSignL2().orElse(1)), //
					TypeUtils.multiply(//
							TypeUtils.divide(this.getCurrentL3Channel().getNextValue().get(), 1000), //
							TypeUtils.divide(this.getVoltageL3Channel().getNextValue().get(), 1000), //
							this.getSignL3().orElse(1)))); //
		};

		final Consumer<Value<Integer>> l1 = ignore -> {
			this._setActivePowerL1(TypeUtils.multiply(//
					TypeUtils.divide(this.getCurrentL1Channel().getNextValue().get(), 1000), //
					TypeUtils.divide(this.getVoltageL1Channel().getNextValue().get(), 1000), //
					this.getSignL1().orElse(1))); //
		};

		final Consumer<Value<Integer>> l2 = ignore -> {
			this._setActivePowerL2(TypeUtils.multiply(//
					TypeUtils.divide(this.getCurrentL2Channel().getNextValue().get(), 1000), //
					TypeUtils.divide(this.getVoltageL2Channel().getNextValue().get(), 1000), //
					this.getSignL2().orElse(1)));
		};

		final Consumer<Value<Integer>> l3 = ignore -> {
			this._setActivePowerL3(TypeUtils.multiply(//
					TypeUtils.divide(this.getCurrentL3Channel().getNextValue().get(), 1000), //
					TypeUtils.divide(this.getVoltageL3Channel().getNextValue().get(), 1000), //
					this.getSignL3().orElse(1)));
		};

		this.getCurrentL1Channel().onSetNextValue(l1);
		this.getCurrentL1Channel().onSetNextValue(powerSum);
		this.getCurrentL2Channel().onSetNextValue(l2);
		this.getCurrentL2Channel().onSetNextValue(powerSum);
		this.getCurrentL3Channel().onSetNextValue(l3);
		this.getCurrentL3Channel().onSetNextValue(powerSum);
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		var modbusProtocol = new ModbusProtocol(this);

		ModbusUtils.readInputElementOnce(modbusProtocol, new SignedWordElement(12), true).thenAccept(result -> { //
			var converter = new ElementToChannelScaleFactorConverter(result + 3); // converting from V to mV
			try {
				modbusProtocol.addTask(new FC4ReadInputRegistersTask(0, Priority.HIGH, //
						this.m(MeterGmcEm2389.ChannelId.VOLTAGE_L1_L2, new SignedWordElement(0), converter), //
						this.m(MeterGmcEm2389.ChannelId.VOLTAGE_L2_L3, new SignedWordElement(1), converter), //
						this.m(MeterGmcEm2389.ChannelId.VOLTAGE_L3_L1, new SignedWordElement(2), converter), //
						this.m(MeterGmcEm2389.ChannelId.VOLTAGE_BETWEEN_PHASES_MEAN, new SignedWordElement(3),
								converter), //
						this.m(ElectricityMeter.ChannelId.VOLTAGE_L1, new SignedWordElement(4), converter), //
						this.m(ElectricityMeter.ChannelId.VOLTAGE_L2, new SignedWordElement(5), converter), //
						this.m(ElectricityMeter.ChannelId.VOLTAGE_L3, new SignedWordElement(6), converter), //
						this.m(ElectricityMeter.ChannelId.VOLTAGE, new SignedWordElement(7), converter), //
						new DummyRegisterElement(8, 10), //
						this.m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(11), //
								ElementToChannelConverter.SCALE_FACTOR_1),
						this.m(MeterGmcEm2389.ChannelId.VOLTAGE_SF, new SignedWordElement(12))));
			} catch (OpenemsException e) {
				this.channel("ReadOnceError").setNextValue(true);
			}
		});

		ModbusUtils.readInputElementOnce(modbusProtocol, new SignedWordElement(108), true).thenAccept(result -> { //
			var converter = new ElementToChannelScaleFactorConverter(result + 3); // converting from A to mA
			try {
				modbusProtocol.addTask(new FC4ReadInputRegistersTask(100, Priority.HIGH, //
						this.m(ElectricityMeter.ChannelId.CURRENT_L1, new SignedWordElement(100), converter), //
						this.m(ElectricityMeter.ChannelId.CURRENT_L2, new SignedWordElement(101), converter), //
						this.m(ElectricityMeter.ChannelId.CURRENT_L3, new SignedWordElement(102), converter), //
						this.m(MeterGmcEm2389.ChannelId.MEAN_PHASE_CURRENT, new SignedWordElement(103), converter), //
						this.m(MeterGmcEm2389.ChannelId.CURRENT_N, new SignedWordElement(104), converter), //
						new DummyRegisterElement(105, 107), //
						this.m(MeterGmcEm2389.ChannelId.CURRENT_SF, new SignedWordElement(108))));
			} catch (OpenemsException e) {
				this.channel("ReadOnceError").setNextValue(true);
			}
		});
		if (!this.config.alternativePowerCalculation()) {
			ModbusUtils.readInputElementOnce(modbusProtocol, new SignedWordElement(212), true).thenAccept(result -> { //
				var converter = new ElementToChannelScaleFactorConverter(result);
				try {
					modbusProtocol.addTask(new FC4ReadInputRegistersTask(200, Priority.HIGH, //
							this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedWordElement(200), converter), //
							this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedWordElement(201), converter), //
							this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedWordElement(202), converter), //
							this.m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedWordElement(203), converter), //
							this.m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedWordElement(204), converter), //
							this.m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedWordElement(205), converter), //
							this.m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedWordElement(206), converter), //
							this.m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedWordElement(207), converter), //
							this.m(MeterGmcEm2389.ChannelId.POWER_FACTOR_L1, new SignedWordElement(208)), //
							this.m(MeterGmcEm2389.ChannelId.POWER_FACTOR_L2, new SignedWordElement(209)), //
							this.m(MeterGmcEm2389.ChannelId.POWER_FACTOR_L3, new SignedWordElement(210)), //
							this.m(MeterGmcEm2389.ChannelId.POWER_FACTOR, new SignedWordElement(211)), //
							this.m(MeterGmcEm2389.ChannelId.POWER_SF, new SignedWordElement(212))));

				} catch (OpenemsException e) {
					this.channel("ReadOnceError").setNextValue(true);
				}
			});
		} else {
			modbusProtocol.addTask(new FC4ReadInputRegistersTask(200, Priority.HIGH, //
					this.m(MeterGmcEm2389.ChannelId.SIGN_L1, new SignedWordElement(200),
							MeterGmcEm2389.SIGNUM_NON_ZERO), //
					this.m(MeterGmcEm2389.ChannelId.SIGN_L2, new SignedWordElement(201),
							MeterGmcEm2389.SIGNUM_NON_ZERO), //
					this.m(MeterGmcEm2389.ChannelId.SIGN_L3, new SignedWordElement(202),
							MeterGmcEm2389.SIGNUM_NON_ZERO))); //
		}

		ModbusUtils.readInputElementOnce(modbusProtocol, new SignedWordElement(310), true).thenAccept(result -> { //
			var converter = new ElementToChannelScaleFactorConverter(result);
			try {
				modbusProtocol.addTask(new FC4ReadInputRegistersTask(300, Priority.LOW, //
						this.m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(300), //
								converter), //
						this.m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(302), //
								converter), //
						this.m(MeterGmcEm2389.ChannelId.REACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(304), //
								converter), //
						this.m(MeterGmcEm2389.ChannelId.REACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(306), //
								converter), //
						this.m(MeterGmcEm2389.ChannelId.PRIMARY_ENERGY_FACTOR, new UnsignedDoublewordElement(308)),
						this.m(MeterGmcEm2389.ChannelId.ENERGY_SF, new SignedWordElement(310))));
			} catch (OpenemsException e) {
				this.channel("ReadOnceError").setNextValue(true);
			}
		});

		modbusProtocol.addTask(new FC4ReadInputRegistersTask(500, Priority.LOW,
				this.m(MeterGmcEm2389.ChannelId.OPERATING_HOURS, new UnsignedDoublewordElement(500))));

		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode) //
		);

	}
}
