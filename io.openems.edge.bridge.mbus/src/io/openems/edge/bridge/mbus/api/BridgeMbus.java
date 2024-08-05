package io.openems.edge.bridge.mbus.api;

import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import org.openmuc.jmbus.MBusConnection;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface BridgeMbus extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
				.debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)
				.text("Communication via MBus Failed")), //
		CYCLE_TIME_IS_TOO_SHORT(Doc.of(Level.WARNING) //
				.debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)), //
		EXECUTION_DURATION(Doc.of(OpenemsType.LONG));

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
	 * Gets the Channel for {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMbusCommunicationFailedChannel() {
		return this.channel(ChannelId.SLAVE_COMMUNICATION_FAILED);
	}

	/**
	 * Gets the Modbus Communication Failed State. See
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMbusCommunicationFailed() {
		return this.getMbusCommunicationFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMbusCommunicationFailed(boolean value) {
		this.getMbusCommunicationFailedChannel().setNextValue(value);
	}


	/**
	 * Add a Task.
	 * 
	 * @param sourceId the Source-ID
	 * @param task     the {@link MbusTask}
	 */
	public void addTask(String sourceId, MbusTask task);

	/**
	 * Get the {@link MBusConnection}.
	 * 
	 * @return the {@link MBusConnection}
	 */
	public MBusConnection getmBusConnection();

	/**
	 * Remove the task with the given Source-ID.
	 * 
	 * @param sourceId the Source-ID
	 */
	public void removeTask(String sourceId);
}
