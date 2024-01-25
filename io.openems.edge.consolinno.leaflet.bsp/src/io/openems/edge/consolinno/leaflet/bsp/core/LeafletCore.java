package io.openems.edge.consolinno.leaflet.bsp.core;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.consolinno.leaflet.bsp.core.enums.ModuleNumber;
import io.openems.edge.consolinno.leaflet.bsp.core.enums.ModuleType;
import io.openems.edge.consolinno.leaflet.bsp.core.modules.CoreAio;
import io.openems.edge.consolinno.leaflet.bsp.core.modules.CorePwm;
import io.openems.edge.consolinno.leaflet.bsp.core.modules.CoreRelais;
import io.openems.edge.consolinno.leaflet.bsp.core.modules.CoreTemperature;

/**
 * This provides the Main Leaflet Module for the Consolinno Leaflet Stack.
 * Configures various Modules and Provides a central hub for common methods.
 */
@ProviderType
public interface LeafletCore extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
	/**
	 * Temperature modules. C:Connected | D:Disconnected T1:TMP Module 1 | T2:TMP
	 * Module 2 | T3:TMP Module 3 0b111 (7) : T1:C | T2:C | T3:C 0b110 (6) : T1:D |
	 * T2:C | T3:C 0b101 (5) : T1:C | T2:D | T3:C 0b100 (4) : T1:D | T2:D | T3:C
	 * 0b011 (3) : T1:C | T2:C | T3:D 0b010 (2) : T1:D | T2:C | T3:D 0b001 (1) :
	 * T1:C | T2:D | T3:D 0b000 (0) : T1:D | T2:D | T3:D
	 * -------------------------------- Relay modules. C:Connected | D:Disconnected
	 * R1:Relay Module 1 | R2:Relay Module 2 | R3:Relay Module 3 | R4:Relay Module 4
	 * 0b1111 (15): R1:C | R2:C | R3:C | R4:C 0b1110 (14): R1:D | R2:C | R3:C | R4:C
	 * 0b1101 (13): R1:C | R2:D | R3:C | R4:C 0b1100 (12): R1:D | R2:D | R3:C | R4:C
	 * 0b1011 (11): R1:C | R2:C | R3:D | R4:C 0b1010 (10): R1:D | R2:C | R3:D | R4:C
	 * 0b1001 (9) : R1:C | R2:D | R3:D | R4:C 0b1000 (8) : R1:D | R2:D | R3:D | R4:C
	 * 0b0111 (7) : R1:C | R2:C | R3:C | R4:D 0b0110 (6) : R1:D | R2:C | R3:C | R4:D
	 * 0b0101 (5) : R1:C | R2:D | R3:C | R4:D 0b0100 (4) : R1:D | R2:D | R3:C | R4:D
	 * 0b0011 (3) : R1:C | R2:C | R3:D | R4:D 0b0010 (2) : R1:D | R2:C | R3:D | R4:D
	 * 0b0001 (1) : R1:C | R2:D | R3:D | R4:D 0b0000 (0) : R1:D | R2:D | R3:D | R4:D
	 * -------------------------------- PWM modules. C:Connected | D:Disconnected
	 * Pn: Pwm Module n 0b11011011 = P8:C|P7:C|P6:D|P5:C|P4:C|P3:D|P2:C|P1:C | | | |
	 * | | | | V V V V V V V V 1 1 0 1 1 0 1 1 --------------------------------
	 *
	 * <ul>
	 * <li>Interface: LeafletConfigurator
	 * <li>Type: Integer
	 * </ul>
	 */
	AIO_MODULES(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	TEMPERATURE_MODULES(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	RELAY_MODULES(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	PWM_MODULES(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	PERIPHERALS(Doc.of(OpenemsType.BOOLEAN) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	/*
	 * Use the OR concatenation of the ENUMs ORANGE_LED and RED_LED.
	 */
	LED(Doc.of(OpenemsType.SHORT) //
		.accessMode(AccessMode.WRITE_ONLY) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //

	/**
	 * Write Configuration Channels for the Inversion of the Relays.
	 * <ul>
	 * <li>Interface: LeafletConfigurator
	 * <li>Type: Integer
	 * </ul>
	 */
	WRITE_RELAY_ONE_INVERT_STATUS(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_RELAY_TWO_INVERT_STATUS(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_RELAY_THREE_INVERT_STATUS(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_RELAY_FOUR_INVERT_STATUS(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	/**
	 * Write Configuration Channels for the Frequency of the Pwm Modules.
	 * <ul>
	 * <li>Interface: LeafletConfigurator
	 * <li>Type: Integer
	 * </ul>
	 */
	WRITE_PWM_FREQUENCY_ONE(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.initialValue(TypeUtils.getAsType(OpenemsType.INTEGER, 200)) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_PWM_FREQUENCY_TWO(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.initialValue(TypeUtils.getAsType(OpenemsType.INTEGER, 200)) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_PWM_FREQUENCY_THREE(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.initialValue(TypeUtils.getAsType(OpenemsType.INTEGER, 200)) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_PWM_FREQUENCY_FOUR(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.initialValue(TypeUtils.getAsType(OpenemsType.INTEGER, 200)) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_PWM_FREQUENCY_FIVE(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.initialValue(TypeUtils.getAsType(OpenemsType.INTEGER, 200)) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_PWM_FREQUENCY_SIX(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.initialValue(TypeUtils.getAsType(OpenemsType.INTEGER, 200)) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_PWM_FREQUENCY_SEVEN(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.initialValue(TypeUtils.getAsType(OpenemsType.INTEGER, 200)) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_PWM_FREQUENCY_EIGHT(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.initialValue(TypeUtils.getAsType(OpenemsType.INTEGER, 200)) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	/**
	 * Write Configuration Channels for the Operating modes of the Aio Modules.
	 * <ul>
	 * <li>Interface: LeafletConfigurator
	 * <li>Type: Integer
	 * </ul>
	 */
	WRITE_AIO_CONFIG_ONE(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_AIO_CONFIG_TWO(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_AIO_CONFIG_THREE(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_AIO_CONFIG_FOUR(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_AIO_CONFIG_FIVE(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_AIO_CONFIG_SIX(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	WRITE_AIO_CONFIG_SEVEN(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	/**
	 * Read-Only Channels for the Configured Pwm Frequency.
	 * <ul>
	 * <li>Interface: LeafletConfigurator
	 * <li>Type: Integer
	 * </ul>
	 */
	READ_PWM_FREQUENCY_ONE(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	READ_PWM_FREQUENCY_TWO(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	READ_PWM_FREQUENCY_THREE(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	READ_PWM_FREQUENCY_FOUR(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	READ_PWM_FREQUENCY_FIVE(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	READ_PWM_FREQUENCY_SIX(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), READ_PWM_FREQUENCY_SEVEN(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	READ_PWM_FREQUENCY_EIGHT(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	/**
	 * Read-Only Channels for the Configured Aio Operation Modes.
	 * <ul>
	 * <li>Interface: LeafletConfigurator
	 * <li>Type: Integer
	 * </ul>
	 */
	READ_AIO_CONFIG_ONE(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	READ_AIO_CONFIG_TWO(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	READ_AIO_CONFIG_THREE(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	READ_AIO_CONFIG_FOUR(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	READ_AIO_CONFIG_FIVE(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	READ_AIO_CONFIG_SIX(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	READ_AIO_CONFIG_SEVEN(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), //
	/**
	 * Read-Only Channels for the Configured Relay Inversion.
	 * <ul>
	 * <li>Interface: LeafletConfigurator
	 * <li>Type: Integer
	 * </ul>
	 */
	READ_RELAY_ONE_INVERT_STATUS(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), READ_RELAY_TWO_INVERT_STATUS(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), READ_RELAY_THREE_INVERT_STATUS(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	), READ_RELAY_FOUR_INVERT_STATUS(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	),
	/**
	 * Write Channel for the Leaflet Configuration.
	 * <ul>
	 * <li>Interface: LeafletConfigurator
	 * <li>Type: Integer
	 * </ul>
	 */
	WRITE_LEAFLET_CONFIG(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.VERY_LOW) //
	),
	/**
	 * Read-Only Channels for the Leaflet Configuration.
	 * <ul>
	 * <li>Interface: LeafletConfigurator
	 * <li>Type: Integer
	 * </ul>
	 */
	READ_LEAFLET_CONFIG(Doc.of(OpenemsType.INTEGER) //
		.persistencePriority(PersistencePriority.VERY_LOW) //

	);

	private final Doc doc;

	ChannelId(Doc doc) {
	    this.doc = doc;
	}

	@Override
	public Doc doc() {
	    return this.doc;
	}
    }

    /**
     * Access to the core relais functionality.
     * 
     * @return coreRelais functionality
     */
    public CoreRelais getCoreRelais();

    /**
     * Access to the core PWM functionality.
     * 
     * @return CorePwm functionality
     */
    public CorePwm getCorePwm();

    /**
     * Access to the core AIO functionality.
     * 
     * @return CoreAio functionality
     */
    public CoreAio getCoreAio();

    /**
     * Access to the core temperature functionality.
     * 
     * @return CoreTemperature functionality
     */
    public CoreTemperature getCoreTemperature();

    /**
     * The modbus id.
     * 
     * @return the modbus id of the Modbus Bridge
     */
    public String getModbusId();

    /**
     * The modbus unit id.
     * 
     * @return the modbus unit id of the Modbus Bridge
     */
    public int getModbusUnitId();

    /**
     * Check if the Module is physically present.
     *
     * @param moduleType TMP,RELAY,PWM
     * @param module     Internal Number of the module
     * @param mReg       Pin position of the Module
     * @param compId     Unique Id of the devices component
     */
    public void checkModulePresent(ModuleType moduleType, ModuleNumber module, int mReg, String compId)
	    throws OpenemsException;
    // boolean modbusModuleCheckout(ModuleType moduleType, int moduleNumber, int
    // mReg, String id);

    /**
     * Registers the device. Returns the Address from the Source file which is
     * usually needed for operation.
     *
     * @param moduleType Type of the module (TMP,RELAY,etc.)
     * @param module     Module number specified on the Device
     * @param mReg       Usually the Position of the device but sometimes
     *                   position-1. Check Register map
     * @return Modbus Offset as integer
     * @throws OpenemsException on unsupported module type
     */
    public int registerModule(ModuleType moduleType, ModuleNumber module, int mReg) throws OpenemsException;
    // int getFunctionAddress(ModuleType moduleType, int moduleNumber, int mReg)
    // throws OpenemsException;

    /**
     * Removes Module from internal Position map.
     *
     * @param moduleType Type of the module (TMP,RELAY,etc.)
     * @param module     Module number specified on the Device
     * @param position   Pin position of the device on the module
     */
    public void unregisterModule(ModuleType moduleType, ModuleNumber module, int position);

}