package io.openems.edge.consolinno.leaflet.bsp.core.modules;

import io.openems.edge.consolinno.leaflet.bsp.core.LeafletCore;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface CorePwm extends OpenemsComponent {

	/**
	 * Sets the Frequency of a Pwm Module.
	 *
	 * @param moduleNumber Module Number of the Pwm Module that is getting
	 *                     configured
	 * @param frequency    Frequency value (between 24 and 1500hz)
	 */
	void setPwmConfiguration(int moduleNumber, int frequency);

	/**
	 * Returns the Register Address for the Discrete Output needed for the inversion
	 * of a Pwm Module.
	 *
	 * @param pwmModule Module number specified on the Pwm Module
	 * @param mReg      Pin Position of the Pwm Device
	 * @return Invert Register for the Pwm Device
	 */
	int getPwmDiscreteOutputAddress(int pwmModule, int mReg);

	/**
	 * Return channel for the detected Pwm Modules.
	 *
	 * @return Integer PwmModules Channel
	 */
	default Channel<Integer> getPwmModulesChannel() {
		return this.channel(LeafletCore.ChannelId.PWM_MODULES);
	}

	/**
	 * Return detected Pwm Modules.
	 *
	 * @return Integer of detected Pwm Modules
	 */
	default int getPwmModules() {
		if (this.getPwmModulesChannel().value().isDefined()) {
			return this.getPwmModulesChannel().value().get();
		} else if (this.getPwmModulesChannel().getNextValue().isDefined()) {
			return this.getPwmModulesChannel().getNextValue().get();
		} else {
			return -1;
		}
	}

	/**
	 * Return the Configuration Channel for the Pwm Frequency of the first Module.
	 *
	 * @return Integer WritePwmFrequencyOne WriteChannel
	 */
	default WriteChannel<Integer> getWritePwmFrequencyOne() {
		return this.channel(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_ONE);
	}

	/**
	 * Return the Configuration Channel for the Pwm Frequency of the second Module.
	 *
	 * @return Integer WritePwmFrequencyTwo WriteChannel
	 */
	default WriteChannel<Integer> getWritePwmFrequencyTwo() {
		return this.channel(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_TWO);
	}

	/**
	 * Return the Configuration Channel for the Pwm Frequency of the third Module.
	 *
	 * @return Integer WritePwmFrequencyThree WriteChannel
	 */
	default WriteChannel<Integer> getWritePwmFrequencyThree() {
		return this.channel(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_THREE);
	}

	/**
	 * Return the Configuration Channel for the Pwm Frequency of the forth Module.
	 *
	 * @return Integer WritePwmFrequencyFour WriteChannel
	 */
	default WriteChannel<Integer> getWritePwmFrequencyFour() {
		return this.channel(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_FOUR);
	}

	/**
	 * Return the Configuration Channel for the Pwm Frequency of the fifth Module.
	 *
	 * @return Integer WritePwmFrequencyFive WriteChannel
	 */
	default WriteChannel<Integer> getWritePwmFrequencyFive() {
		return this.channel(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_FIVE);
	}

	/**
	 * Return the Configuration Channel for the Pwm Frequency of the sixth Module.
	 *
	 * @return Integer WritePwmFrequencySix WriteChannel
	 */
	default WriteChannel<Integer> getWritePwmFrequencySix() {

		return this.channel(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_SIX);
	}

	/**
	 * Return the Configuration Channel for the Pwm Frequency of the seventh Module.
	 *
	 * @return Integer WritePwmFrequencySeven WriteChannel
	 */
	default WriteChannel<Integer> getWritePwmFrequencySeven() {
		return this.channel(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_SEVEN);
	}

	/**
	 * Return the Configuration Channel for the Pwm Frequency of the eighth Module.
	 *
	 * @return Integer WritePwmFrequencyEight WriteChannel
	 */
	default WriteChannel<Integer> getWritePwmFrequencyEight() {
		return this.channel(LeafletCore.ChannelId.WRITE_PWM_FREQUENCY_EIGHT);
	}

	/**
	 * Return the Read-Only Configuration Channel for the Pwm Frequency of the first
	 * Module.
	 *
	 * @return Integer ReadPwmFrequencyOne Channel
	 */
	default Channel<Integer> getReadPwmFrequencyOne() {
		return this.channel(LeafletCore.ChannelId.READ_PWM_FREQUENCY_ONE);
	}

	/**
	 * Return the Read-Only Configuration Channel for the Pwm Frequency of the
	 * second Module.
	 *
	 * @return Integer ReadPwmFrequencyTwo Channel
	 */
	default Channel<Integer> getReadPwmFrequencyTwo() {
		return this.channel(LeafletCore.ChannelId.READ_PWM_FREQUENCY_TWO);
	}

	/**
	 * Return the Read-Only Configuration Channel for the Pwm Frequency of the third
	 * Module.
	 *
	 * @return Integer ReadPwmFrequencyThree Channel
	 */
	default Channel<Integer> getReadPwmFrequencyThree() {
		return this.channel(LeafletCore.ChannelId.READ_PWM_FREQUENCY_THREE);
	}

	/**
	 * Return the Read-Only Configuration Channel for the Pwm Frequency of the forth
	 * Module.
	 *
	 * @return Integer ReadPwmFrequencyFour Channel
	 */
	default Channel<Integer> getReadPwmFrequencyFour() {
		return this.channel(LeafletCore.ChannelId.READ_PWM_FREQUENCY_FOUR);
	}

	/**
	 * Return the Read-Only Configuration Channel for the Pwm Frequency of the fifth
	 * Module.
	 *
	 * @return Integer ReadPwmFrequencyFive Channel
	 */
	default Channel<Integer> getReadPwmFrequencyFive() {
		return this.channel(LeafletCore.ChannelId.READ_PWM_FREQUENCY_FIVE);
	}

	/**
	 * Return the Read-Only Configuration Channel for the Pwm Frequency of the sixth
	 * Module.
	 *
	 * @return Integer ReadPwmFrequencySix Channel
	 */
	default Channel<Integer> getReadPwmFrequencySix() {
		return this.channel(LeafletCore.ChannelId.READ_PWM_FREQUENCY_SIX);
	}

	/**
	 * Return the Read-Only Configuration Channel for the Pwm Frequency of the
	 * seventh Module.
	 *
	 * @return Integer ReadPwmFrequencySeven Channel
	 */
	default Channel<Integer> getReadPwmFrequencySeven() {
		return this.channel(LeafletCore.ChannelId.READ_PWM_FREQUENCY_SEVEN);
	}

	/**
	 * Return the Read-Only Configuration Channel for the Pwm Frequency of the
	 * eighth Module.
	 *
	 * @return Integer ReadPwmFrequencyEight Channel
	 */
	default Channel<Integer> getReadPwmFrequencyEight() {
		return this.channel(LeafletCore.ChannelId.READ_PWM_FREQUENCY_EIGHT);
	}

	/**
	 * Return the Configured Pwm Frequency for all modules.
	 *
	 * @return String of Pwm Frequency
	 */
	default String getReadPwmFrequency() {

		String returnString = "";
		if (this.getReadPwmFrequencyOne().value().isDefined() //
				&& this.getReadPwmFrequencyOne().value().get() != 32768) {
			returnString += this.getReadPwmFrequencyOne().value() + " ";
		} else {
			returnString += "- ";
		}
		if (this.getReadPwmFrequencyTwo().value().isDefined() //
				&& this.getReadPwmFrequencyTwo().value().get() != 32768) {
			returnString += this.getReadPwmFrequencyTwo().value() + " ";
		} else {
			returnString += "- ";
		}
		if (this.getReadPwmFrequencyThree().value().isDefined() //
				&& this.getReadPwmFrequencyThree().value().get() != 32768) {
			returnString += this.getReadPwmFrequencyThree().value() + " ";
		} else {
			returnString += "- ";
		}
		if (this.getReadPwmFrequencyFour().value().isDefined()
				&& this.getReadPwmFrequencyFour().value().get() != 32768) {
			returnString += this.getReadPwmFrequencyFour().value() + " ";
		} else {
			returnString += "- ";
		}
		if (this.getReadPwmFrequencyFive().value().isDefined()
				&& this.getReadPwmFrequencyFive().value().get() != 32768) {
			returnString += this.getReadPwmFrequencyFive().value() + " ";
		} else {
			returnString += "- ";
		}
		if (this.getReadPwmFrequencySix().value().isDefined() && this.getReadPwmFrequencySix().value().get() != 32768) {
			returnString += this.getReadPwmFrequencySix().value() + " ";
		} else {
			returnString += "- ";
		}
		if (this.getReadPwmFrequencySeven().value().isDefined()
				&& this.getReadPwmFrequencySeven().value().get() != 32768) {
			returnString += this.getReadPwmFrequencySeven().value() + " ";
		} else {
			returnString += "- ";
		}
		if (this.getReadPwmFrequencyEight().value().isDefined()
				&& this.getReadPwmFrequencyEight().value().get() != 32768) {
			returnString += this.getReadPwmFrequencyEight().value() + " ";
		} else {
			returnString += "- ";
		}
		return returnString;
	}

}
