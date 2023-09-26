package io.openems.edge.bridge.wmbus.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jmbus.wireless.WMBusConnection;

import io.openems.edge.bridge.mbus.api.ChannelDataRecordMapper;
import io.openems.edge.bridge.mbus.api.ChannelRecord;

public class WMbusProtocol {

	private final AbstractOpenemsWMbusComponent parent; // creator of this task instance
	private final byte[] key;

	private final List<ChannelRecord> channelDataRecordsList = new ArrayList<>();

	// This is the data link layer (=dll) secondary address. The radio address is
	// part of this.
	private SecondaryAddress dllSecondaryAddress = null;

	// The meter number is part of the transport layer (=tpl) secondary address.
	// Most meters can be identified by the
	// radio address alone, but for some cases (like distinguishing between channel
	// 1 and 2 for the Padpuls Relay)
	// the meter number is also needed.
	private boolean identifyByMeterNumber = false;
	private String meterNumber = "";

	public WMbusProtocol(AbstractOpenemsWMbusComponent parent, String keyAsHexString, ChannelRecord... channelRecords) {
		this.parent = parent;
		if (keyAsHexString.equals("")) {
			this.key = null;
		} else {
			this.key = this.hexStringToByteArray(keyAsHexString);
		}
		this.channelDataRecordsList.addAll(Arrays.asList(channelRecords));
	}

	private byte[] hexStringToByteArray(String value) {
		return HexFormat.of().parseHex(value);
	}

	/**
	 * Respond to a received DataStructure.
	 * 
	 * @param data received by the {@link BridgeWMbus}.
	 */
	public void setResponse(VariableDataStructure data) {
		new ChannelDataRecordMapper(data, this.channelDataRecordsList);
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
			this.parent.findRecordPositions(data, this.channelDataRecordsList);
		}
	}

	public String getRadioAddress() {
		return this.parent.getRadioAddress();
	}

	public String getComponentId() {
		return this.parent.id();
	}

	public void setDllSecondaryAddress(SecondaryAddress dllSecondaryAddress) {
		this.dllSecondaryAddress = dllSecondaryAddress;
	}

	public SecondaryAddress getDllSecondaryAddress() {
		return this.dllSecondaryAddress;
	}

	/**
	 * Register the Protocol/Meter Key.
	 * 
	 * @param connection the WMBusConnection established by the {@link BridgeWMbus}.
	 */
	public void registerKey(WMBusConnection connection) {
		connection.addKey(this.dllSecondaryAddress, this.key);
	}

	public void setLogSignalStrength(int signalStrength) {
		this.parent.setLogSignalStrength(signalStrength);
	}

	public boolean isIdentifyByMeterNumber() {
		return this.identifyByMeterNumber;
	}

	public void setMeterNumber(String meterNumber) {
		this.meterNumber = meterNumber;
		this.identifyByMeterNumber = true;
	}

	public String getMeterNumber() {
		return this.meterNumber;
	}

	public void setWrongDecryptionKey(boolean fail) {
		this.parent._setWmbusDecryptionFailed(fail);
	}

	public List<ChannelRecord> getChannelDataRecordsList() {
		return this.channelDataRecordsList;
	}
}
