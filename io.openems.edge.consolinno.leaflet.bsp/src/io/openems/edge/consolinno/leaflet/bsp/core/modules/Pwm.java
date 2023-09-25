package io.openems.edge.consolinno.leaflet.bsp.core.modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.consolinno.leaflet.bsp.core.LeafletCoreImpl;
import io.openems.edge.consolinno.leaflet.bsp.core.enums.ModuleType;
import io.openems.edge.consolinno.leaflet.bsp.core.utils.DataUtilities;
import io.openems.edge.consolinno.leaflet.bsp.core.utils.ModuleRegister;

public class Pwm {
	private final Logger log = LoggerFactory.getLogger(Pwm.class);

	private DataUtilities dataUtilities;
	private LeafletCoreImpl leafletCore;
	private int pwmConfigRegisterOne;
	private int pwmConfigRegisterTwo;
	private int pwmConfigRegisterThree;
	private int pwmConfigRegisterFour;
	private int pwmConfigRegisterFive;
	private int pwmConfigRegisterSix;
	private int pwmConfigRegisterSeven;
	private int pwmConfigRegisterEight;

	public Pwm(LeafletCoreImpl leafletCore, DataUtilities dataUtilities) {
		this.leafletCore = leafletCore;
		this.dataUtilities = dataUtilities;

	}

	/**
	 * Stores the pwmConfig Register in a local variable.
	 */
	public void setPwmConfigurationAddresses() {
		this.pwmConfigRegisterOne = this.getConfigurationAddress(ModuleType.PWM, 1);
		this.pwmConfigRegisterTwo = this.getConfigurationAddress(ModuleType.PWM, 2);
		this.pwmConfigRegisterThree = this.getConfigurationAddress(ModuleType.PWM, 3);
		this.pwmConfigRegisterFour = this.getConfigurationAddress(ModuleType.PWM, 4);
		this.pwmConfigRegisterFive = this.getConfigurationAddress(ModuleType.PWM, 5);
		this.pwmConfigRegisterSix = this.getConfigurationAddress(ModuleType.PWM, 6);
		this.pwmConfigRegisterSeven = this.getConfigurationAddress(ModuleType.PWM, 7);
		this.pwmConfigRegisterEight = this.getConfigurationAddress(ModuleType.PWM, 8);
	}

	/**
	 * Sets the Frequency of a Pwm Module.
	 *
	 * @param moduleNumber Module Number of the Pwm Module that is getting
	 *                     configured
	 * @param frequency    Frequency value (between 24 and 1500hz)
	 */
	public void setPwmConfiguration(int moduleNumber, int frequency) {
		this.leafletCore.enterConfigMode();
		try {
			switch (moduleNumber) {
			case 1:
				this.leafletCore.getWritePwmFrequencyOne().setNextWriteValue(frequency);
				break;
			case 2:
				this.leafletCore.getWritePwmFrequencyTwo().setNextWriteValue(frequency);
				break;
			case 3:
				this.leafletCore.getWritePwmFrequencyThree().setNextWriteValue(frequency);
				break;
			case 4:
				this.leafletCore.getWritePwmFrequencyFour().setNextWriteValue(frequency);
				break;
			case 5:
				this.leafletCore.getWritePwmFrequencyFive().setNextWriteValue(frequency);
				break;
			case 6:
				this.leafletCore.getWritePwmFrequencySix().setNextWriteValue(frequency);
				break;
			case 7:
				this.leafletCore.getWritePwmFrequencySeven().setNextWriteValue(frequency);
				break;
			case 8:
				this.leafletCore.getWritePwmFrequencyEight().setNextWriteValue(frequency);
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + moduleNumber);
			}

		} catch (OpenemsError.OpenemsNamedException ignored) {
			this.log.error("Error in setPwmConfiguration");

		}
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

	public Logger getLog() {
		return this.log;
	}

	public DataUtilities getDataUtilities() {
		return this.dataUtilities;
	}

	public LeafletCoreImpl getLeafletCore() {
		return this.leafletCore;
	}

	public int getPwmConfigRegisterOne() {
		return this.pwmConfigRegisterOne;
	}

	public int getPwmConfigRegisterTwo() {
		return this.pwmConfigRegisterTwo;
	}

	public int getPwmConfigRegisterThree() {
		return this.pwmConfigRegisterThree;
	}

	public int getPwmConfigRegisterFour() {
		return this.pwmConfigRegisterFour;
	}

	public int getPwmConfigRegisterFive() {
		return this.pwmConfigRegisterFive;
	}

	public int getPwmConfigRegisterSix() {
		return this.pwmConfigRegisterSix;
	}

	public int getPwmConfigRegisterSeven() {
		return this.pwmConfigRegisterSeven;
	}

	public int getPwmConfigRegisterEight() {
		return this.pwmConfigRegisterEight;
	}
}
