package io.openems.edge.consolinno.leaflet.bsp.core;

import java.time.Instant;

import javax.naming.ConfigurationException;

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

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC2ReadInputsTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.leaflet.bsp.core.enums.ModuleNumber;
import io.openems.edge.consolinno.leaflet.bsp.core.enums.ModuleType;
import io.openems.edge.consolinno.leaflet.bsp.core.enums.OrangeLed;
import io.openems.edge.consolinno.leaflet.bsp.core.enums.RedLed;
import io.openems.edge.consolinno.leaflet.bsp.core.modules.Aio;
import io.openems.edge.consolinno.leaflet.bsp.core.modules.CoreAio;
import io.openems.edge.consolinno.leaflet.bsp.core.modules.CorePwm;
import io.openems.edge.consolinno.leaflet.bsp.core.modules.CoreRelais;
import io.openems.edge.consolinno.leaflet.bsp.core.modules.CoreTemperature;
import io.openems.edge.consolinno.leaflet.bsp.core.modules.Pwm;
import io.openems.edge.consolinno.leaflet.bsp.core.utils.DataUtilities;
import io.openems.edge.consolinno.leaflet.bsp.core.utils.ModuleRegister;
import io.openems.edge.io.api.bsp.BoardSupportPackage;
import io.openems.edge.io.api.bsp.LedState;

/**
 * Configurator for Consolinno Modbus modules. Reads the CSV Register source
 * file, sets the general Modbus Protocol and configures module specific values
 * (e.g Relay inversion status). This must be configured, otherwise
 * Leaflet-Modules/Devices within OpenEMS won't work.
 */

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Consolinno.Leaflet.Bsp.Core", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class LeafletCoreImpl extends AbstractOpenemsModbusComponent implements BoardSupportPackage, CoreRelais, CorePwm,
		CoreTemperature, CoreAio, LeafletCore, EventHandler, ModbusComponent, OpenemsComponent {

	private static final int INVERSION_CAPABLE_RELAY_COUNT = 4;
	private final int[] relayInverseRegisters = new int[INVERSION_CAPABLE_RELAY_COUNT];
	private static final ModuleRegister LEAFLET_AIO_CONNECTION_STATUS = new ModuleRegister(ModuleType.LEAFLET, 0, 11);
	private static final ModuleRegister LEAFLET_TMP_CONNECTION_STATUS = new ModuleRegister(ModuleType.LEAFLET, 0, 10);
	private static final ModuleRegister LEAFLET_REL_CONNECTION_STATUS = new ModuleRegister(ModuleType.LEAFLET, 0, 9);
	private static final ModuleRegister LEAFLET_PWM_CONNECTION_STATUS = new ModuleRegister(ModuleType.LEAFLET, 0, 8);
	private static final ModuleRegister LEAFLET_CONFIG_REGISTER = new ModuleRegister(ModuleType.LEAFLET, 0, 0);
	private final Logger log = LoggerFactory.getLogger(LeafletCoreImpl.class);

	// TODO check if leaflet hardware base board has one digital out channels. right
	// now this is unused.
	private final BooleanWriteChannel[] boardDigitalOutChannels;

	private Config config;
	private DataUtilities dataUtilities;
	private Aio aio;
	private Pwm pwm;
	private Instant systemStartTime;

	private boolean configFlag;
	private int relayOneInvertStatus;
	private int relayTwoInvertStatus;
	private int relayThreeInvertStatus;
	private int relayFourInvertStatus;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Sum sum;

	@Reference
	protected ConfigurationAdmin cm;

	public LeafletCoreImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				LeafletCore.ChannelId.values(), //
				BoardSupportPackage.ChannelId.values() //
		);
		this.boardDigitalOutChannels = new BooleanWriteChannel[] { this.getDigitalOut1WriteChannel() };
		this.systemStartTime = Instant.now();
		this.channel(BoardSupportPackage.ChannelId.UPTIME).setNextValue(0);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException, ConfigurationException {

		this.config = config;
		this.dataUtilities = new DataUtilities(this, config);
		this.aio = new Aio(this, this.dataUtilities);
		this.pwm = new Pwm(this, this.dataUtilities);
		if (!this.config.enabled()) {
			return;
		}
		this.getPeripheralsChannel().setNextValue(1);
		try {
			this.getPeripheralsChannel().setNextWriteValueFromObject(true);
		} catch (OpenemsError.OpenemsNamedException e) {
			this.log.info("Couldn't write into Internal peripherals Channel");
		}
		if (this.dataUtilities.checkFirmwareCompatibility()) {
			// Splits the big CSV Output into the different Modbus Types(OutputCoil,...)
			this.dataUtilities.splitArrayIntoType();
			// Sets the Register variables for the Configuration
			this.createRelayInverseRegisterArray();
			this.pwm.setPwmConfigurationAddresses();
			this.aio.setAioConfigurationAddresses();
			if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
					"Modbus", config.modbus_id())) {
				return;
			}
		} else {
			this.log.error("Firmware incompatible or not Running!");
			this.log.info("The Configurator will now deactivate itself.");
			throw new OpenemsException("Firmware incompatible or not running!");
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public int getModbusUnitId() {
		return this.config.modbusUnitId();
	}

	public String getModbusId() {
		return this.config.modbus_id();
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.boardDigitalOutChannels;
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		return new BooleanReadChannel[] { this.getDigitalIn1Channel(), this.getDigitalIn2Channel(),
				this.getDigitalIn3Channel(), this.getDigitalIn4Channel() };
	}

	/**
	 * Creates an Array with all of the Configuration Registers for inverting
	 * Relays.
	 */
	private void createRelayInverseRegisterArray() {
		for (int i = 0; i < INVERSION_CAPABLE_RELAY_COUNT; i++) {
			this.relayInverseRegisters[i] = this.dataUtilities.analogOutputHoldingRegisters
					.get(new ModuleRegister(ModuleType.REL, i + 1, 0));
		}
	}

	/**
	 * Gets the Modules which are present over modbus. Writes Configuration
	 * Registers.
	 *
	 * @return Writes the Information over the present Modules
	 */

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this,

				// TODO increase performance - do not start read tasks for each value
				// better read in complete map and then map the values to their corresponding
				// channels...

				// Read Module Connection Status
				new FC4ReadInputRegistersTask(
						this.dataUtilities.analogInputRegisters.get(LEAFLET_TMP_CONNECTION_STATUS), Priority.HIGH,
						m(LeafletCore.ChannelId.TEMPERATURE_MODULES,
								new UnsignedWordElement(
										this.dataUtilities.analogInputRegisters.get(LEAFLET_TMP_CONNECTION_STATUS)),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC4ReadInputRegistersTask(
						this.dataUtilities.analogInputRegisters.get(LEAFLET_REL_CONNECTION_STATUS), Priority.HIGH,
						m(LeafletCore.ChannelId.RELAY_MODULES,
								new UnsignedWordElement(
										this.dataUtilities.analogInputRegisters.get(LEAFLET_REL_CONNECTION_STATUS)),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC4ReadInputRegistersTask(
						this.dataUtilities.analogInputRegisters.get(LEAFLET_PWM_CONNECTION_STATUS), Priority.HIGH,
						m(LeafletCore.ChannelId.PWM_MODULES,
								new UnsignedWordElement(
										this.dataUtilities.analogInputRegisters.get(LEAFLET_PWM_CONNECTION_STATUS)),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC4ReadInputRegistersTask(
						this.dataUtilities.analogInputRegisters.get(LEAFLET_AIO_CONNECTION_STATUS), Priority.HIGH,
						m(LeafletCore.ChannelId.AIO_MODULES,
								new UnsignedWordElement(
										this.dataUtilities.analogInputRegisters.get(LEAFLET_AIO_CONNECTION_STATUS)),
								ElementToChannelConverter.DIRECT_1_TO_1)),

				new FC4ReadInputRegistersTask(
						this.dataUtilities.analogOutputHoldingRegisters.get(LEAFLET_CONFIG_REGISTER), Priority.HIGH,
						m(LeafletCore.ChannelId.READ_LEAFLET_CONFIG,
								new UnsignedWordElement(
										this.dataUtilities.analogOutputHoldingRegisters.get(LEAFLET_CONFIG_REGISTER)),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.dataUtilities.analogOutputHoldingRegisters.get(LEAFLET_CONFIG_REGISTER),
						m(LeafletCore.ChannelId.WRITE_LEAFLET_CONFIG,
								new UnsignedWordElement(
										this.dataUtilities.analogOutputHoldingRegisters.get(LEAFLET_CONFIG_REGISTER)),
								ElementToChannelConverter.DIRECT_1_TO_1)),

				// Relay invert Configuration
				new FC6WriteRegisterTask(this.relayInverseRegisters[0],
						m(LeafletCore.ChannelId.WRITE_RELAY_ONE_INVERT_STATUS,
								new SignedWordElement(this.relayInverseRegisters[0]),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.relayInverseRegisters[1],
						m(LeafletCore.ChannelId.WRITE_RELAY_TWO_INVERT_STATUS,
								new UnsignedWordElement(this.relayInverseRegisters[1]),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.relayInverseRegisters[2],
						m(LeafletCore.ChannelId.WRITE_RELAY_THREE_INVERT_STATUS,
								new UnsignedWordElement(this.relayInverseRegisters[2]),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.relayInverseRegisters[3],
						m(LeafletCore.ChannelId.WRITE_RELAY_FOUR_INVERT_STATUS,
								new UnsignedWordElement(this.relayInverseRegisters[3]),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				// PWM Frequency Configuration
				new FC6WriteRegisterTask(this.pwm.getPwmConfigRegisterOne(),
						m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_ONE,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterOne()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.pwm.getPwmConfigRegisterTwo(),
						m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_TWO,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterTwo()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.pwm.getPwmConfigRegisterThree(),
						m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_THREE,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterThree()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.pwm.getPwmConfigRegisterFour(),
						m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_FOUR,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterFour()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.pwm.getPwmConfigRegisterFive(),
						m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_FIVE,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterFive()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.pwm.getPwmConfigRegisterSix(),
						m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_SIX,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterSix()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.pwm.getPwmConfigRegisterSeven(),
						m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_SEVEN,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterSeven()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.pwm.getPwmConfigRegisterEight(),
						m(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_EIGHT,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterEight()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				// AIO Configuration
				new FC6WriteRegisterTask(this.aio.getAioConfigRegisterOne(),
						m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_ONE,
								new SignedWordElement(this.aio.getAioConfigRegisterOne()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.aio.getAioConfigRegisterTwo(),
						m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_TWO,
								new UnsignedWordElement(this.aio.getAioConfigRegisterTwo()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.aio.getAioConfigRegisterThree(),
						m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_THREE,
								new UnsignedWordElement(this.aio.getAioConfigRegisterThree()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.aio.getAioConfigRegisterFour(),
						m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_FOUR,
								new UnsignedWordElement(this.aio.getAioConfigRegisterFour()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.aio.getAioConfigRegisterFive(),
						m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_FIVE,
								new SignedWordElement(this.aio.getAioConfigRegisterFive()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.aio.getAioConfigRegisterSix(),
						m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_SIX,
								new UnsignedWordElement(this.aio.getAioConfigRegisterSix()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(this.aio.getAioConfigRegisterSeven(),
						m(LeafletCore.ChannelId.WRITE_AIO_CONFIG_SEVEN,
								new UnsignedWordElement(this.aio.getAioConfigRegisterSeven()),
								ElementToChannelConverter.DIRECT_1_TO_1)),

				new FC3ReadRegistersTask(this.pwm.getPwmConfigRegisterOne(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_ONE,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterOne()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.pwm.getPwmConfigRegisterTwo(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_TWO,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterTwo()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.pwm.getPwmConfigRegisterThree(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_THREE,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterThree()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.pwm.getPwmConfigRegisterFour(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_FOUR,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterFour()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.pwm.getPwmConfigRegisterFive(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_FIVE,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterFive()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.pwm.getPwmConfigRegisterSix(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_SIX,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterSix()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.pwm.getPwmConfigRegisterSeven(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_SEVEN,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterSeven()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.pwm.getPwmConfigRegisterEight(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_PWM_FREQUENCY_EIGHT,
								new UnsignedWordElement(this.pwm.getPwmConfigRegisterEight()),
								ElementToChannelConverter.DIRECT_1_TO_1)),

				new FC3ReadRegistersTask(this.aio.getAioConfigRegisterOne(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_AIO_CONFIG_ONE,
								new SignedWordElement(this.aio.getAioConfigRegisterOne()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.aio.getAioConfigRegisterTwo(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_AIO_CONFIG_TWO,
								new UnsignedWordElement(this.aio.getAioConfigRegisterTwo()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.aio.getAioConfigRegisterThree(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_AIO_CONFIG_THREE,
								new UnsignedWordElement(this.aio.getAioConfigRegisterThree()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.aio.getAioConfigRegisterFour(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_AIO_CONFIG_FOUR,
								new UnsignedWordElement(this.aio.getAioConfigRegisterFour()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.aio.getAioConfigRegisterFive(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_AIO_CONFIG_FIVE,
								new SignedWordElement(this.aio.getAioConfigRegisterFive()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.aio.getAioConfigRegisterSix(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_AIO_CONFIG_SIX,
								new UnsignedWordElement(this.aio.getAioConfigRegisterSix()),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.aio.getAioConfigRegisterSeven(), Priority.LOW,
						m(LeafletCore.ChannelId.READ_AIO_CONFIG_SEVEN,
								new UnsignedWordElement(this.aio.getAioConfigRegisterSeven()),
								ElementToChannelConverter.DIRECT_1_TO_1)),

				new FC3ReadRegistersTask(this.relayInverseRegisters[0], Priority.LOW,
						m(LeafletCore.ChannelId.READ_RELAY_ONE_INVERT_STATUS,
								new UnsignedWordElement(this.relayInverseRegisters[0]),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.relayInverseRegisters[1], Priority.LOW,
						m(LeafletCore.ChannelId.READ_RELAY_TWO_INVERT_STATUS,
								new UnsignedWordElement(this.relayInverseRegisters[1]),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.relayInverseRegisters[2], Priority.LOW,
						m(LeafletCore.ChannelId.READ_RELAY_THREE_INVERT_STATUS,
								new UnsignedWordElement(this.relayInverseRegisters[2]),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC3ReadRegistersTask(this.relayInverseRegisters[3], Priority.LOW,
						m(LeafletCore.ChannelId.READ_RELAY_FOUR_INVERT_STATUS,
								new UnsignedWordElement(this.relayInverseRegisters[3]),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC5WriteCoilTask(0,
						m(LeafletCore.ChannelId.PERIPHERALS, new CoilElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC6WriteRegisterTask(1,
						m(LeafletCore.ChannelId.LED, new UnsignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1)),
				new FC2ReadInputsTask(3, Priority.HIGH,
						m(BoardSupportPackage.ChannelId.DIGITAL_IN1, new CoilElement(3))),
				new FC2ReadInputsTask(4, Priority.HIGH,
						m(BoardSupportPackage.ChannelId.DIGITAL_IN2, new CoilElement(4))),
				new FC2ReadInputsTask(5, Priority.HIGH,
						m(BoardSupportPackage.ChannelId.DIGITAL_IN3, new CoilElement(5))),
				new FC2ReadInputsTask(6, Priority.HIGH,
						m(BoardSupportPackage.ChannelId.DIGITAL_IN4, new CoilElement(6)))

		);
	}

	@Override
	public void checkModulePresent(ModuleType moduleType, ModuleNumber module, int mReg, String componentId)
			throws OpenemsException {
		if (this.dataUtilities.modbusModuleCheckout(moduleType, module.value, mReg, componentId) == false) {
			throw new OpenemsException("Invalid configuration. Module " + moduleType.name() + " Position "
					+ module.value + " Position " + mReg + " is not allowed");
		}
	}

	@Override
	public int registerModule(ModuleType moduleType, ModuleNumber module, int mReg) throws OpenemsException {
		// public int getFunctionAddress(ModuleType moduleType, int moduleNumber, int
		// mReg) throws OpenemsException {
		switch (moduleType) {
		case TMP:
			return this.dataUtilities.analogInputRegisters.get(new ModuleRegister(moduleType, module.value, mReg));
		case REL:
			return this.dataUtilities.discreteOutputCoils.get(new ModuleRegister(moduleType, module.value, mReg));
		case PWM:
			return this.dataUtilities.analogOutputHoldingRegisters
					.get(new ModuleRegister(moduleType, module.value, mReg));
		case GPIO:
			return this.dataUtilities.discreteInputContacts
					.get(new ModuleRegister(ModuleType.LEAFLET, module.value, mReg));
		default:
			throw new OpenemsException("Unsupported moduleType " + moduleType);
		}
	}

	/**
	 * Return the Address from the Source file which is usually needed for
	 * operation. This method is only for the AIO module.
	 *
	 * @param moduleType   Type of the module (TMP,RELAY,etc.)
	 * @param moduleNumber Module number specified on the Device
	 * @param position     Pin position of the device on the module
	 * @param input        true if the Register needed is an input Register | false
	 *                     if an output
	 * @return Modbus Offset as integer
	 */
	public int getFunctionAddress(ModuleType moduleType, ModuleNumber moduleNumber, int position, boolean input)
			throws OpenemsException {
		if (moduleType != ModuleType.AIO) {
			return this.registerModule(moduleType, moduleNumber, position);
		} else {
			if (input) {
				return this.dataUtilities.analogInputRegisters
						.get(new ModuleRegister(moduleType, moduleNumber.value, position));
			} else {
				return this.dataUtilities.analogOutputHoldingRegisters
						.get(new ModuleRegister(moduleType, moduleNumber.value, position));
			}
		}

	}

	/**
	 * Removes Module from internal Position map.
	 *
	 * @param moduleType   Type of the module (TMP,RELAY,etc.)
	 * @param moduleNumber Module number specified on the Device
	 * @param position     Pin position of the device on the module
	 */
	@Override
	public void unregisterModule(ModuleType moduleType, ModuleNumber moduleNumber, int position) {
		this.dataUtilities.positionMap.get(moduleType).getPositionMap().get(moduleNumber.value)
				.remove((Integer) position);
	}

	/**
	 * Invert the Relay Functionality.
	 *
	 * @param moduleNumber Module Number specified on the Relay module
	 * @param position     Position of the Relay on the Module
	 */
	@Override
	public void invertRelay(int moduleNumber, int position) {
		try {
			switch (moduleNumber) {
			case 1:
				this.relayOneInvertStatus = this.relayOneInvertStatus | position;
				getWriteInvertRelayOneStatus().setNextWriteValue(this.relayOneInvertStatus);
				break;
			case 2:
				this.relayTwoInvertStatus = this.relayTwoInvertStatus | position;
				getWriteInvertRelayTwoStatus().setNextWriteValue(this.relayTwoInvertStatus);
				break;
			case 3:
				this.relayThreeInvertStatus = this.relayThreeInvertStatus | position;
				getWriteInvertRelayThreeStatus().setNextWriteValue(this.relayThreeInvertStatus);
				break;
			case 4:
				this.relayFourInvertStatus = this.relayFourInvertStatus | position;
				getWriteInvertRelayFourStatus().setNextWriteValue(this.relayFourInvertStatus);
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + moduleNumber);
			}
		} catch (OpenemsError.OpenemsNamedException ignored) {
			this.log.error("Error in invertRelay");
		}
	}

	/**
	 * Sets the Frequency of a Pwm Module.
	 *
	 * @param moduleNumber Module Number of the Pwm Module that is getting
	 *                     configured
	 * @param frequency    Frequency value (between 24 and 1500hz)
	 */
	@Override
	public void setPwmConfiguration(int moduleNumber, int frequency) {
		this.pwm.setPwmConfiguration(moduleNumber, frequency);
	}

	/**
	 * Returns the Register Address for the Discrete Output needed for the inversion
	 * of a Pwm Module.
	 *
	 * @param pwmModule Module number specified on the Pwm Module
	 * @param mReg      Pin Position of the Pwm Device
	 * @return Invert Register for the Pwm Device
	 */
	@Override
	public int getPwmDiscreteOutputAddress(int pwmModule, int mReg) {
		return this.dataUtilities.discreteOutputCoils.get(new ModuleRegister(ModuleType.PWM, pwmModule, mReg));
	}

	/**
	 * Configures the AIO Modules.
	 *
	 * @param moduleNumber Module number specified on the Aio Module
	 * @param position     Pin Position of the Aio Device
	 * @param config       The configuration, of the specific AIO Output (e.g.
	 *                     0-20mA_in)
	 */
	@Override
	public void setAioConfig(int moduleNumber, int position, String config) {
		this.aio.setAioConfig(moduleNumber, position, config);
	}

	/**
	 * Return the Register Address where the AIO module outputs a percentage value
	 * of whatever is configured.
	 *
	 * @param moduleType   Type of the module (TMP,RELAY,etc.)
	 * @param moduleNumber Module number specified on the Device
	 * @param mReg         Position of the AIO device
	 * @param input        true if Input | false if output
	 * @return Address of the Percent conversion Register
	 */
	@Override
	public int getAioPercentAddress(ModuleType moduleType, int moduleNumber, int mReg, boolean input)
			throws OpenemsException {
		if (moduleType != ModuleType.AIO) {
			throw new OpenemsException("Invalid module type " + moduleType);
		} else {
			if (input) {
				return this.dataUtilities.analogInputRegisters.get(new ModuleRegister(moduleType, moduleNumber, mReg));
			} else {
				return this.dataUtilities.analogOutputHoldingRegisters
						.get(new ModuleRegister(moduleType, moduleNumber, mReg));
			}
		}

	}

	/**
	 * Puts a relay back in regular mode. Is Called when a inverted Relay
	 * deactivates.
	 *
	 * @param moduleNumber Module number specified on the Device
	 * @param position     Position of the Relay on the module
	 */
	@Override
	public void revertInversion(int moduleNumber, int position) {
		try {
			switch (moduleNumber) {
			case 1:
				this.relayOneInvertStatus = this.relayOneInvertStatus ^ position;
				getWriteInvertRelayOneStatus().setNextWriteValue(this.relayOneInvertStatus);
				break;
			case 2:
				this.relayTwoInvertStatus = this.relayTwoInvertStatus ^ position;
				getWriteInvertRelayTwoStatus().setNextWriteValue(this.relayTwoInvertStatus);
				break;
			case 3:
				this.relayThreeInvertStatus = this.relayThreeInvertStatus ^ position;
				getWriteInvertRelayThreeStatus().setNextWriteValue(this.relayThreeInvertStatus);
				break;
			case 4:
				this.relayFourInvertStatus = this.relayFourInvertStatus ^ position;
				getWriteInvertRelayFourStatus().setNextWriteValue(this.relayFourInvertStatus);
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + moduleNumber);
			}
		} catch (OpenemsError.OpenemsNamedException ignored) {
			this.log.error("Error in revertInversion");
		}
	}

	/**
	 * Changes the content of the Leaflet Config Register to enter configuration
	 * mode. !IMPORTANT NOTE! Config Mode HAS to be exited when the configuration is
	 * done to avoid hardware damages.
	 */
	public void enterConfigMode() {
		try {
			this.getWriteLeafletConfigChannel().setNextWriteValue(1337);
			this.configFlag = true;
		} catch (OpenemsError.OpenemsNamedException ignored) {
			this.log.error("Error in enterConfigMode");
		}

	}

	/**
	 * Checks if we are still in Config mode and the Configuration has already taken
	 * place.
	 *
	 * @param event BEFORE_PROCESS_IMAGE
	 */
	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			if (this.configFlag && (getReadAioConfig() == this.aio.getConfigFlags())) {
				this.exitConfigMode();
			}
			this.updateUptime(this.systemStartTime);
			this.updateStatusLedEdge();
			this.updateStatusLedBackend();

			break;
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			break;
		}

	}

	/**
	 * Exits Configuration mode by changing the Config register back.
	 */
	private void exitConfigMode() {
		try {
			this.getWriteLeafletConfigChannel().setNextWriteValue(7331);
			this.configFlag = false;
		} catch (OpenemsError.OpenemsNamedException ignored) {
			this.log.error("Error in exitConfigMode");
		}

	}

	private RedLed updateStatusLedEdge() {

		if (this.sum.getState().isAtLeast(Level.FAULT)) {
			this.setStatusLedEdgeValue(LedState.RED);
			return RedLed.ON;

		} else if (this.sum.getState().isAtLeast(Level.WARNING)) {
			this.setStatusLedEdgeValue(LedState.OFF);
			return RedLed.OFF;

		} else {
			this.setStatusLedEdgeValue(LedState.RED_BLINK);
			return RedLed.BLINK;
		}
	}

	private OrangeLed updateStatusLedBackend() {
		boolean cloudConnected = this.getCloudConnectionState();
		if (cloudConnected) {
			this.setStatusLedBackendValue(LedState.ORANGE_BLINK);
			return OrangeLed.BLINK;
		} else {
			this.setStatusLedBackendValue(LedState.ORANGE);
			return OrangeLed.ON;
		}
	}

	private boolean getCloudConnectionState() {
		try {
			var cmp = this.componentManager.getComponent(this.config.backendComponentId());
			return !cmp.getState().isAtLeast(Level.INFO);
		} catch (OpenemsNamedException e) {
			return false;
		}
	}

	/**
	 * Return WriteChannel for the Leaflet Config.
	 *
	 * @return Integer WriteChannel WriteLeafletConfig
	 */
	private WriteChannel<Integer> getWriteLeafletConfigChannel() {
		return this.channel(LeafletCore.ChannelId.WRITE_LEAFLET_CONFIG);
	}

	private WriteChannel<Boolean> getPeripheralsChannel() {
		return this.channel(LeafletCore.ChannelId.PERIPHERALS);
	}

	private WriteChannel<Short> getLedsChannel() {
		return this.channel(LeafletCore.ChannelId.LED);
	}

	@Override
	public CoreRelais getCoreRelais() {
		return this;
	}

	@Override
	public CorePwm getCorePwm() {
		return this;
	}

	@Override
	public CoreAio getCoreAio() {
		return this;
	}

	@Override
	public CoreTemperature getCoreTemperature() {
		return this;
	}

	@Override
	public String debugLog() {
		return "TMP " + Integer.toBinaryString(this.getTemperatureModules()) + " REL "
				+ Integer.toBinaryString(this.getRelayModules()) + " AIO "
				+ Integer.toBinaryString(this.getAioModules()) + " PWM " + Integer.toBinaryString(this.getPwmModules())
				+ " Freq " + this.getReadPwmFrequency();
	}

	/**
	 * Internal Led States are set.
	 * 
	 * @param orangeLed 0 = Off | 0x01 = Blink | 0x02 = Constant Light
	 * @param redLed    0 = Off | 0x10 = Blink | 0x20 = Constant Light
	 * @throws OpenemsError.OpenemsNamedException This should not happen
	 */
	public void setLeds(OrangeLed orangeLed, RedLed redLed) throws OpenemsError.OpenemsNamedException {
		this.getLedsChannel().setNextWriteValue((short) (orangeLed.value | redLed.value));
	}
}
