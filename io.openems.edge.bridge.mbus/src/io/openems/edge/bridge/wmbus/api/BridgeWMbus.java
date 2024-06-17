package io.openems.edge.bridge.wmbus.api;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface BridgeWMbus extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		WMBUS_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.text("WMbus Communication Failed")), //
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
	 * Gets the Channel for {@link ChannelId#WMBUS_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getWmbusCommunicationFailedChannel() {
		return this.channel(ChannelId.WMBUS_COMMUNICATION_FAILED);
	}

	/**
	 * Gets the Modbus Communication Failed State. See
	 * {@link WmbusComponent.ChannelId#WMBUS_DECRYPTION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getWmbusCommunicationFailed() {
		return this.getWmbusCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#WMBUS_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWmbusCommunicationFailed(boolean value) {
		this.getWmbusCommunicationFailedChannel().setNextValue(value);
	}

	/**
	 * Adds a {@link WMbusProtocol} to the Bridge.
	 * 
	 * @param sourceId the source Component {@link AbstractOpenemsWMbusComponent}.
	 * @param protocol the {@link WMbusProtocol}.
	 */
	public void addProtocol(String sourceId, WMbusProtocol protocol);

	/**
	 * Removes the {@link WMbusProtocol} related to the Source Component.
	 * 
	 * @param sourceId the component Id.
	 */
	public void removeProtocol(String sourceId);
}
