package io.openems.edge.bridge.wmbus.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * A OpenEMS Component that uses Modbus communication.
 *
 * <p>
 * Classes implementing this interface typically inherit
 * {@link AbstractOpenemsWMbusComponent}.
 */
@ProviderType
public interface WmbusComponent extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		WMBUS_DECRYPTION_FAILED(Doc.of(Level.FAULT) //
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE) //
				.text("WMbus Decryption Failed, Check Decryption Key")), //

		RADIO_ADDRESS_LENGTH_WRONG(Doc.of(Level.FAULT)
				.text("Radio Address Length is wrong. Exactly 8 Characters needed.")
		)
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#WMBUS_DECRYPTION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getWmbusDecryptionFailedChannel() {
		return this.channel(ChannelId.WMBUS_DECRYPTION_FAILED);
	}

	/**
	 * Gets the Wmbus Communication Failed State. See
	 * {@link ChannelId#WMBUS_DECRYPTION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getWmbusDecryptionFailed() {
		return this.getWmbusDecryptionFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#WMBUS_DECRYPTION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWmbusDecryptionFailed(boolean value) {
		this.getWmbusDecryptionFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RADIO_ADDRESS_LENGTH_WRONG}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getWmbusRadioAddressLengthWrongChannel() {
		return this.channel(ChannelId.RADIO_ADDRESS_LENGTH_WRONG);
	}

	/**
	 * Gets the Wmbus Radio Address Length Wrong State. See
	 * {@link ChannelId#RADIO_ADDRESS_LENGTH_WRONG}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getWmbusRadioAddressLengthWrong() {
		return this.getWmbusRadioAddressLengthWrongChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#RADIO_ADDRESS_LENGTH_WRONG} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWmbusRadioAddressLengthWrong(boolean value) {
		this.getWmbusRadioAddressLengthWrongChannel().setNextValue(value);
	}
}
