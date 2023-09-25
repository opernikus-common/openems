package io.openems.edge.consolinno.leaflet.bsp.core.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.consolinno.leaflet.bsp.core.LeafletCoreImpl;
import io.openems.edge.consolinno.leaflet.bsp.core.enums.ModuleType;
import io.openems.edge.consolinno.leaflet.bsp.core.utils.DataUtilities;
import io.openems.edge.consolinno.leaflet.bsp.core.utils.ModuleRegister;

public class Aio {

	private final Logger log = LoggerFactory.getLogger(Aio.class);

	private DataUtilities dataUtilities;
	private LeafletCoreImpl leafletCore;

	public Aio(LeafletCoreImpl leafletCore, DataUtilities dataUtilities) {
		this.leafletCore = leafletCore;
		this.dataUtilities = dataUtilities;

	}

	// AIO is still in development and doesn't exists for now
	private int aioConfigOne = 0;
	private int aioConfigTwo = 0;
	private int aioConfigThree = 0;
	private int aioConfigFour = 0;
	private int aioConfigFive = 0;
	private int aioConfigSix = 0;
	private int aioConfigSeven = 0;
	private int aioConfigRegisterOne;
	private int aioConfigRegisterTwo;
	private int aioConfigRegisterThree;
	private int aioConfigRegisterFour;
	private int aioConfigRegisterFive;
	private int aioConfigRegisterSix;
	private int aioConfigRegisterSeven;

	/**
	 * Configures the AIO Modules.
	 *
	 * @param moduleNumber Module number specified on the Aio Module
	 * @param position     Pin Position of the Aio Device
	 * @param config       The configuration, of the specific AIO Output (e.g.
	 *                     0-20mA_in)
	 */
	public void setAioConfig(int moduleNumber, int position, String config) {
		int configInt = this.convertAioConfigToInt(config);
		this.leafletCore.enterConfigMode();
		try {
			switch (moduleNumber) {
			case 1:
				this.aioConfigOne = this.aioConfigOne | (configInt << 4 * (position - 1));
				this.leafletCore.getAioConfigOne().setNextWriteValue(this.aioConfigOne);
				break;
			case 2:
				this.aioConfigTwo = this.aioConfigTwo | (configInt << 4 * (position - 1));
				this.leafletCore.getAioConfigTwo().setNextWriteValue(this.aioConfigTwo);
				break;
			case 3:
				this.aioConfigThree = this.aioConfigThree | (configInt << 4 * (position - 1));
				this.leafletCore.getAioConfigThree().setNextWriteValue(this.aioConfigThree);
				break;
			case 4:
				this.aioConfigFour = this.aioConfigFour | (configInt << 4 * (position - 1));
				this.leafletCore.getAioConfigFour().setNextWriteValue(this.aioConfigFour);
				break;
			case 5:
				this.aioConfigFive = this.aioConfigFive | (configInt << 4 * (position - 1));
				this.leafletCore.getAioConfigFive().setNextWriteValue(this.aioConfigFive);
				break;
			case 6:
				this.aioConfigSix = this.aioConfigSix | (configInt << 4 * (position - 1));
				this.leafletCore.getAioConfigSix().setNextWriteValue(this.aioConfigSix);
				break;
			case 7:
				this.aioConfigSeven = this.aioConfigSeven | (configInt << 4 * (position - 1));
				this.leafletCore.getAioConfigSeven().setNextWriteValue(this.aioConfigSeven);
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + moduleNumber);
			}

		} catch (OpenemsError.OpenemsNamedException ignored) {
			this.log.error("Error in setAioConfig");
		}
	}

	/**
	 * Stores the aioConfig Register in a local variable.
	 */
	public void setAioConfigurationAddresses() {
		this.aioConfigRegisterOne = this.getConfigurationAddress(ModuleType.AIO, 1);
		this.aioConfigRegisterTwo = this.getConfigurationAddress(ModuleType.AIO, 2);
		this.aioConfigRegisterThree = this.getConfigurationAddress(ModuleType.AIO, 3);
		this.aioConfigRegisterFour = this.getConfigurationAddress(ModuleType.AIO, 4);
		this.aioConfigRegisterFive = this.getConfigurationAddress(ModuleType.AIO, 5);
		this.aioConfigRegisterSix = this.getConfigurationAddress(ModuleType.AIO, 6);
		this.aioConfigRegisterSeven = this.getConfigurationAddress(ModuleType.AIO, 7);
	}

	/**
	 * Returns the Address from the Source file of the configuration register.
	 *
	 * @param moduleType   Type of the module (TMP,RELAY,etc.)
	 * @param moduleNumber Module number specified on the Device
	 * @return Configuration Register address for the module
	 */
	public int getConfigurationAddress(ModuleType moduleType, int moduleNumber) {
		return this.dataUtilities.analogOutputHoldingRegisters.get(new ModuleRegister(moduleType, moduleNumber, 0));
	}

	public int getConfigFlags() {
		return (this.aioConfigOne | this.aioConfigTwo | this.aioConfigThree | this.aioConfigFour | this.aioConfigFive
				| this.aioConfigSix | this.aioConfigSeven);
	}

	/**
	 * Converts the Readable Type String of the AIO module into the internal Int
	 * value. Should only be used for the Aio configuration.
	 *
	 * @param config String of the configuration set for that AIO (e.g 0-20mA_in)
	 * @return Internal int value that has to be written into the Register
	 */
	private int convertAioConfigToInt(String config) {
		switch (config) {
		case ("10V_out"):
			return 1;
		case ("10V_in"):
			return 2;
		case ("0-20mA_out"):
			return 3;
		case ("0-20mA_in"):
			return 4;
		case ("4-20mA_out"):
			return 5;
		case ("4-20mA_in"):
			return 6;
		case ("Temp_in"):
			return 7;
		case ("Digital_in"):
			return 8;

		}
		return 0;
	}

	public Logger getLog() {
		return this.log;
	}

	public DataUtilities getDataUtilities() {
		return this.dataUtilities;
	}

	public LeafletCoreImpl getLeafletCore() {
		return this.leafletCore;
	}

	public int getAioConfigOne() {
		return this.aioConfigOne;
	}

	public int getAioConfigTwo() {
		return this.aioConfigTwo;
	}

	public int getAioConfigThree() {
		return this.aioConfigThree;
	}

	public int getAioConfigFour() {
		return this.aioConfigFour;
	}

	public int getAioConfigFive() {
		return this.aioConfigFive;
	}

	public int getAioConfigSix() {
		return this.aioConfigSix;
	}

	public int getAioConfigSeven() {
		return this.aioConfigSeven;
	}

	public int getAioConfigRegisterOne() {
		return this.aioConfigRegisterOne;
	}

	public int getAioConfigRegisterTwo() {
		return this.aioConfigRegisterTwo;
	}

	public int getAioConfigRegisterThree() {
		return this.aioConfigRegisterThree;
	}

	public int getAioConfigRegisterFour() {
		return this.aioConfigRegisterFour;
	}

	public int getAioConfigRegisterFive() {
		return this.aioConfigRegisterFive;
	}

	public int getAioConfigRegisterSix() {
		return this.aioConfigRegisterSix;
	}

	public int getAioConfigRegisterSeven() {
		return this.aioConfigRegisterSeven;
	}

}
