package io.openems.edge.io.acthor;

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Heater.Ac.Thor", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class AcThorImpl extends AbstractOpenemsModbusComponent implements AcThor, EventHandler, ModbusComponent,
		ElectricityMeter, DigitalOutput, TimedataProvider, OpenemsComponent {

	private Config config = null;

	private final Logger log = LoggerFactory.getLogger(AcThorImpl.class);

	private final BooleanWriteChannel[] outputChannels;

	public AcThorImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				AcThor.ChannelId.values() //
		);
		this.outputChannels = new BooleanWriteChannel[] { //
				this.channel(AcThor.ChannelId.OUT_1), //
				this.channel(AcThor.ChannelId.OUT_2), //
				this.channel(AcThor.ChannelId.OUT_3) //
		};
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
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
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			try {
				var calculatedPower = this.calculatePower();
				this.setWritePower(calculatedPower);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "Unable to set power ex:" + e.getMessage());
			}
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateConsumptionEnergy.update(this.getActivePower().orElse(0));
			break;
		}
	}

	private Integer calculatePower() {
		var power = 0;
		var out1 = this.getOut1WriteChannel().getNextWriteValueAndReset().orElse(false);
		this.getOut1WriteChannel().setNextValue(out1);
		power += out1 ? this.config.powerStep() : 0;
		var out2 = this.getOut2WriteChannel().getNextWriteValueAndReset().orElse(false);
		this.getOut2WriteChannel().setNextValue(out2);
		power += out2 ? this.config.powerStep() : 0;
		var out3 = this.getOut3WriteChannel().getNextWriteValueAndReset().orElse(false);
		this.getOut3WriteChannel().setNextValue(out3);
		power += out3 ? this.config.powerStep() : 0;
		return power;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this,
				new FC3ReadRegistersTask(1000, Priority.HIGH, m(AcThor.ChannelId.POWER, new UnsignedWordElement(1000)),
						m(AcThor.ChannelId.TEMP_1, new UnsignedWordElement(1001)),
						m(AcThor.ChannelId.H_W_1_MAX, new UnsignedWordElement(1002)),
						m(AcThor.ChannelId.STATUS, new UnsignedWordElement(1003)),
						m(AcThor.ChannelId.POWER_TIMEOUT, new UnsignedWordElement(1004)),
						m(AcThor.ChannelId.BOOST_MODE, new UnsignedWordElement(1005)),
						m(AcThor.ChannelId.H_W_1_MIN, new UnsignedWordElement(1006)),
						m(AcThor.ChannelId.BOOST_TIME_1_START, new UnsignedWordElement(1007)),
						m(AcThor.ChannelId.BOOST_TIME_1_STOP, new UnsignedWordElement(1008))),
				// 1009-1011 Hour, minute, second

				new FC3ReadRegistersTask(1012, Priority.LOW,
						m(AcThor.ChannelId.BOOST_ACTIVATE, new UnsignedWordElement(1012)),
						m(AcThor.ChannelId.AC_THOR_NUMBER, new UnsignedWordElement(1013)),
						m(AcThor.ChannelId.MAX_POWER, new UnsignedWordElement(1014)),
						m(AcThor.ChannelId.TEMPCHIP, new UnsignedWordElement(1015)),
						m(AcThor.ChannelId.CONTROL_FIRMWARE_VERSION, new UnsignedWordElement(1016)),
						m(AcThor.ChannelId.PS_FIRMWARE_VERSION, new UnsignedWordElement(1017)),
						m(AcThor.ChannelId.SERIAL_NUMBER, new StringWordElement(1018, 8)),
						m(AcThor.ChannelId.BOOST_TIME_2_START, new UnsignedWordElement(1026)),
						m(AcThor.ChannelId.BOOST_TIME_2_STOP, new UnsignedWordElement(1027)),
						m(AcThor.ChannelId.CONTROL_FIRMWARE_SUB_VERSION, new UnsignedWordElement(1028)),
						new DummyRegisterElement(1029), // control firmware update available
						m(AcThor.ChannelId.TEMP_2, new UnsignedWordElement(1030)),
						m(AcThor.ChannelId.TEMP_3, new UnsignedWordElement(1031)),
						m(AcThor.ChannelId.TEMP_4, new UnsignedWordElement(1032))),
				// Temp5-Temp8 not available

				new FC3ReadRegistersTask(1037, Priority.LOW,
						m(AcThor.ChannelId.H_W_2_MAX, new UnsignedWordElement(1037)),
						m(AcThor.ChannelId.H_W_3_MAX, new UnsignedWordElement(1038)),
						m(AcThor.ChannelId.H_W_2_MIN, new UnsignedWordElement(1039)),
						m(AcThor.ChannelId.H_W_3_MIN, new UnsignedWordElement(1040)),
						m(AcThor.ChannelId.RH_1_MAX, new UnsignedWordElement(1041)),
						m(AcThor.ChannelId.RH_2_MAX, new UnsignedWordElement(1042)),
						m(AcThor.ChannelId.RH_3_MAX, new UnsignedWordElement(1043)),
						m(AcThor.ChannelId.RH_1_DAY_MIN, new UnsignedWordElement(1044)),
						m(AcThor.ChannelId.RH_2_DAY_MIN, new UnsignedWordElement(1045)),
						m(AcThor.ChannelId.RH_3_DAY_MIN, new UnsignedWordElement(1046)),
						m(AcThor.ChannelId.RH_1_NIGHT_MIN, new UnsignedWordElement(1047)),
						m(AcThor.ChannelId.RH_2_NIGHT_MIN, new UnsignedWordElement(1048)),
						m(AcThor.ChannelId.RH_3_NIGHT_MIN, new UnsignedWordElement(1049)),
						m(AcThor.ChannelId.NIGHT_FLAG, new UnsignedWordElement(1050))),

				new FC3ReadRegistersTask(1057, Priority.HIGH,
						m(AcThor.ChannelId.STRATIFACTION_FLAG, new UnsignedWordElement(1057)),
						m(AcThor.ChannelId.RELAY_1_STATUS, new UnsignedWordElement(1058)),
						new DummyRegisterElement(1059), // load state
						m(AcThor.ChannelId.LOAD_NOMINAL_POWER, new UnsignedWordElement(1060)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(1061),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedWordElement(1062),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AcThor.ChannelId.VOLTAGE_OUT, new UnsignedWordElement(1063)),
						m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(1064)),
						new DummyRegisterElement(1065, 1066), // Operation mode, Access level
						m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(1067),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedWordElement(1068),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AcThor.ChannelId.METER_POWER, new SignedWordElement(1069)), //
						new DummyRegisterElement(1070), // Control type
						m(AcThor.ChannelId.PMAX_ABS, new UnsignedWordElement(1071)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(1072),
								ElementToChannelConverter.SCALE_FACTOR_3),
						m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedWordElement(1073),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AcThor.ChannelId.P_OUT_1, new UnsignedWordElement(1074)),
						m(AcThor.ChannelId.P_OUT_2, new UnsignedWordElement(1075)),
						m(AcThor.ChannelId.P_OUT_3, new UnsignedWordElement(1076)),
						m(AcThor.ChannelId.OPERATION_STATE, new UnsignedWordElement(1077)),
						new DummyRegisterElement(1078, 1080),
						m(AcThor.ChannelId.DEVICE_STATE, new UnsignedWordElement(1081)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedWordElement(1082)),
						m(AcThor.ChannelId.SOLAR_PART_OF_DEVICE_POWER, new UnsignedWordElement(1083)),
						m(AcThor.ChannelId.GRID_PART_OF_DEVICE_POWER, new UnsignedWordElement(1084))),

				new FC6WriteRegisterTask(1000, m(AcThor.ChannelId.POWER, new UnsignedWordElement(1000))));
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.outputChannels;
	}

	@Override
	public String debugLog() {
		return this.getActivePower().asString();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}
