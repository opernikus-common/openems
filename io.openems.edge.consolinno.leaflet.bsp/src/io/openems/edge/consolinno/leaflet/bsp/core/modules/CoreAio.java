package io.openems.edge.consolinno.leaflet.bsp.core.modules;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.consolinno.leaflet.bsp.core.LeafletCore;
import io.openems.edge.consolinno.leaflet.bsp.core.enums.ModuleType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides access to Core AIO functionality.
 */
public interface CoreAio extends OpenemsComponent {

	/**
	 * Configures the AIO Modules.
	 *
	 * @param moduleNumber Module number specified on the Aio Module
	 * @param position     Pin Position of the Aio Device
	 * @param config       The configuration, of the specific AIO Output (e.g.
	 *                     0-20mA_in)
	 */
	void setAioConfig(int moduleNumber, int position, String config);

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
	int getAioPercentAddress(ModuleType moduleType, int moduleNumber, int mReg, boolean input) throws OpenemsException;

	/**
	 * Return detected Aio Modules.
	 *
	 * @return Integer of detected Aio Modules
	 */
	default int getAioModules() {
		if (this.getAioModulesChannel().value().isDefined()) {
			return this.getAioModulesChannel().value().get();
		} else if (this.getAioModulesChannel().getNextValue().isDefined()) {
			return this.getAioModulesChannel().getNextValue().get();
		} else {
			return -1;
		}
	}

	/**
	 * Return channel for the detected Aio Modules.
	 *
	 * @return Integer AioModules Channel
	 */
	default Channel<Integer> getAioModulesChannel() {
		return this.channel(LeafletCore.ChannelId.AIO_MODULES);
	}

	/**
	 * Return WriteChannel for the Configuration of the first Aio Module.
	 *
	 * @return Integer AioConfigOne WriteChannel
	 */
	default WriteChannel<Integer> getAioConfigOne() {
		return this.channel(LeafletCore.ChannelId.WRITE_AIO_CONFIG_ONE);
	}

	/**
	 * Return WriteChannel for the Configuration of the second Aio Module.
	 *
	 * @return Integer AioConfigTwo WriteChannel
	 */
	default WriteChannel<Integer> getAioConfigTwo() {
		return this.channel(LeafletCore.ChannelId.WRITE_AIO_CONFIG_TWO);
	}

	/**
	 * Return WriteChannel for the Configuration of the third Aio Module.
	 *
	 * @return Integer AioConfigThree WriteChannel
	 */
	default WriteChannel<Integer> getAioConfigThree() {
		return this.channel(LeafletCore.ChannelId.WRITE_AIO_CONFIG_THREE);
	}

	/**
	 * Return WriteChannel for the Configuration of the forth Aio Module.
	 *
	 * @return Integer AioConfigFour WriteChannel
	 */
	default WriteChannel<Integer> getAioConfigFour() {
		return this.channel(LeafletCore.ChannelId.WRITE_AIO_CONFIG_FOUR);
	}

	/**
	 * Return WriteChannel for the Configuration of the fifth Aio Module.
	 *
	 * @return Integer AioConfigFive WriteChannel
	 */
	default WriteChannel<Integer> getAioConfigFive() {
		return this.channel(LeafletCore.ChannelId.WRITE_AIO_CONFIG_FIVE);
	}

	/**
	 * Return WriteChannel for the Configuration of the sixth Aio Module.
	 *
	 * @return Integer AioConfigSix WriteChannel
	 */
	default WriteChannel<Integer> getAioConfigSix() {
		return this.channel(LeafletCore.ChannelId.WRITE_AIO_CONFIG_SIX);
	}

	/**
	 * Return WriteChannel for the Configuration of the seventh Aio Module.
	 *
	 * @return Integer AioConfigSeven WriteChannel
	 */
	default WriteChannel<Integer> getAioConfigSeven() {
		return this.channel(LeafletCore.ChannelId.WRITE_AIO_CONFIG_SEVEN);
	}

	/**
	 * Return the Configuration of the Aio Modules.
	 *
	 * @return Integer of Aio Module Configuration
	 */
	default Integer getReadAioConfig() {
		int result = 0;
		if (this.getReadAioConfigOne().value().isDefined()) {
			result = result | this.getReadAioConfigOne().value().get();
		} else if (this.getReadAioConfigOne().getNextValue().isDefined()) {
			result = result | this.getReadAioConfigOne().getNextValue().get();
		}
		if (this.getReadAioConfigTwo().value().isDefined()) {
			result = result | this.getReadAioConfigTwo().value().get();
		} else if (this.getReadAioConfigTwo().getNextValue().isDefined()) {
			result = result | this.getReadAioConfigTwo().getNextValue().get();
		}
		if (this.getReadAioConfigThree().value().isDefined()) {
			result = result | this.getReadAioConfigThree().value().get();
		} else if (this.getReadAioConfigThree().getNextValue().isDefined()) {
			result = result | this.getReadAioConfigThree().getNextValue().get();
		}
		if (this.getReadAioConfigFour().value().isDefined()) {
			result = result | this.getReadAioConfigFour().value().get();
		} else if (this.getReadAioConfigFour().getNextValue().isDefined()) {
			result = result | this.getReadAioConfigFour().getNextValue().get();
		}
		if (this.getReadAioConfigFive().value().isDefined()) {
			result = result | this.getReadAioConfigFive().value().get();
		} else if (this.getReadAioConfigFive().getNextValue().isDefined()) {
			result = result | this.getReadAioConfigFive().getNextValue().get();
		}
		if (this.getReadAioConfigSix().value().isDefined()) {
			result = result | this.getReadAioConfigSix().value().get();
		} else if (this.getReadAioConfigSix().getNextValue().isDefined()) {
			result = result | this.getReadAioConfigSix().getNextValue().get();
		}
		if (this.getReadAioConfigSeven().value().isDefined()) {
			result = result | this.getReadAioConfigSeven().value().get();
		} else if (this.getReadAioConfigSeven().getNextValue().isDefined()) {
			result = result | this.getReadAioConfigSeven().getNextValue().get();
		}

		return result;
	}

	/**
	 * Returns the Config Channel for the first Aio Module.
	 *
	 * @return Integer ReadAioConfigOne Channel
	 */
	default Channel<Integer> getReadAioConfigOne() {
		return this.channel(LeafletCore.ChannelId.READ_AIO_CONFIG_ONE);
	}

	/**
	 * Returns the Config Channel for the second Aio Module.
	 *
	 * @return Integer ReadAioConfigTwo Channel
	 */
	default Channel<Integer> getReadAioConfigTwo() {
		return this.channel(LeafletCore.ChannelId.READ_AIO_CONFIG_TWO);
	}

	/**
	 * Returns the Config Channel for the third Aio Module.
	 *
	 * @return Integer ReadAioConfigThree Channel
	 */
	default Channel<Integer> getReadAioConfigThree() {
		return this.channel(LeafletCore.ChannelId.READ_AIO_CONFIG_THREE);
	}

	/**
	 * Returns the Config Channel for the forth Aio Module.
	 *
	 * @return Integer ReadAioConfigFour Channel
	 */
	default Channel<Integer> getReadAioConfigFour() {
		return this.channel(LeafletCore.ChannelId.READ_AIO_CONFIG_FOUR);
	}

	/**
	 * Returns the Config Channel for the fifth Aio Module.
	 *
	 * @return Integer ReadAioConfigFive Channel
	 */
	default Channel<Integer> getReadAioConfigFive() {
		return this.channel(LeafletCore.ChannelId.READ_AIO_CONFIG_FIVE);
	}

	/**
	 * Returns the Config Channel for the sixth Aio Module.
	 *
	 * @return Integer ReadAioConfigSix Channel
	 */
	default Channel<Integer> getReadAioConfigSix() {
		return this.channel(LeafletCore.ChannelId.READ_AIO_CONFIG_SIX);
	}

	/**
	 * Returns the Config Channel for the seventh Aio Module.
	 *
	 * @return Integer ReadAioConfigSeven Channel
	 */
	default Channel<Integer> getReadAioConfigSeven() {
		return this.channel(LeafletCore.ChannelId.READ_AIO_CONFIG_SEVEN);
	}

}
