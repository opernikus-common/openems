package io.openems.edge.bridge.mbus.api;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.VariableDataStructure;

public class MbusTask {

	private final AbstractOpenemsMbusComponent parent; // creator of this task instance
	private final BridgeMbus bridgeMbus;
	private Instant lastPollTime;
	private final int pollingIntervalSeconds;

	public MbusTask(BridgeMbus bridgeMbus, AbstractOpenemsMbusComponent parent) {
		this(bridgeMbus, parent, 0);

	}

	public MbusTask(BridgeMbus bridgeMbus, AbstractOpenemsMbusComponent parent, int pollingIntervalSeconds) {
		this.bridgeMbus = bridgeMbus;
		this.parent = parent;
		this.pollingIntervalSeconds = pollingIntervalSeconds;
	}

	/**
	 * Get the Request.
	 * 
	 * @return a {@link VariableDataStructure}
	 * @throws InterruptedIOException on error
	 * @throws IOException            on error
	 */
	public VariableDataStructure getRequest() throws InterruptedIOException, IOException {
		this.lastPollTime = Instant.now();
		return this.bridgeMbus.getmBusConnection().read(this.parent.getPrimaryAddress());
	}

	public void setResponse(VariableDataStructure data) {
		new ChannelDataRecordMapper(data, this.parent.getChannelDataRecordsList());
	}

	public int getPrimaryAddress() {
		return this.parent.getPrimaryAddress();
	}

	/**
	 * Should the task be polled.
	 * 
	 * @return a boolean.
	 */
	public boolean shouldPoll() {
		return this.lastPollTime == null || this.pollingIntervalSeconds == 0
				|| ChronoUnit.SECONDS.between(this.lastPollTime, Instant.now()) >= this.pollingIntervalSeconds;
	}

	/**
	 * If the parent is configured as AUTOSEARCH meter. The DataStructure will be
	 * analyzed. Depending on the Descriptions, the RecordPositions are updated.
	 * e.g. When looking for a FlowTemp entry -> {@link DataRecord#getDescription()}
	 * equals {@link org.openmuc.jmbus.DataRecord.Description#FLOW_TEMPERATURE}, the
	 * corresponding datarecords address will be updated.
	 * 
	 * @param data the processed dataStructure.
	 */
	public void updateAddress(VariableDataStructure data) {
		if (this.parent.hasDynamicDataAddress()) {
			this.parent.findRecordPositions(data);
		}
	}

	public void setCommunicationFailed(boolean failed) {
		this.parent._setMbusCommunicationFailed(failed);
	}
}
