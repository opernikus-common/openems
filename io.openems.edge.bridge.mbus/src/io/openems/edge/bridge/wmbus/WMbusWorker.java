package io.openems.edge.bridge.wmbus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.wmbus.api.WMbusProtocol;
import io.openems.edge.common.component.OpenemsComponent;
import org.openmuc.jmbus.DecodingException;
import org.openmuc.jmbus.SecondaryAddress;
import org.openmuc.jmbus.VariableDataStructure;
import org.openmuc.jmbus.wireless.WMBusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A message is taken from the message queue. Then the list of WMBus devices
 * registered to the bridge is traversed to see if the message is from any of
 * those devices. A comparison is done first by radio address, and if necessary
 * also by meter number. Currently, the comparison by meter number is only used
 * to tell channel 1 and 2 apart for the Padpuls Relay. I'm not entirely sure if
 * it is possible for two different devices to have the same radio address. If
 * it is, they could be told apart by their meter number. However, the
 * "detection by meter number" of this bridge can not be used by meter types
 * other than the Padpuls Relay. This is a limitation of the jmbus library. The
 * reason is, the meter number is only readable after decoding the message, and
 * the keys for decoding are stored in a list with the radio address (more
 * specifically, the dllSecondaryAddress) as the identifier. This list is part
 * of the jmbus library. So there can be only one key per radio address. The
 * Padpuls Relay circumvents this problem by using the same decryption key for
 * both channels. If you need to use two meters that happen to have the same
 * radio address, "detection by meter number" can be used to tell them apart if
 * you set them to the same decryption key. "detection by meter number" can be
 * enabled by adding a few lines of code to the meter module. If there are two
 * devices using the same radio address but you want to read data from only one
 * of them, this is no problem. As long as they don't have the same decryption
 * key, only messages from one devices can be decoded. The messages from the
 * other device can not be decoded and are not processed. You will get constant
 * decode-error messages though because of the other device.
 * </p>
 */

public class WMbusWorker extends AbstractCycleWorker {

	private final Logger log = LoggerFactory.getLogger(WMbusWorker.class);

	private final BridgeWMbusImpl parent;

	public WMbusWorker(BridgeWMbusImpl parent) {
		this.parent = parent;
	}

	@Override
	protected void forever() throws OpenemsException {

		while (!this.parent.getMessageQueue().isEmpty()) {
			WMBusMessage message;
			try {
				message = this.parent.getMessageQueue().takeLast();
				// This secondary address is the data link layer (=dll) one. It contains the
				// radio address.
				SecondaryAddress dllSecondaryAddress = message.getSecondaryAddress();
				if (this.parent.isScan()) {
					OpenemsComponent.logInfo(this.parent, this.log, "Device found:\n Radio address: " //
							+ dllSecondaryAddress.getDeviceId() //
							+ ", Manufacturer ID: " + dllSecondaryAddress.getManufacturerId() //
							+ ", device version: " + dllSecondaryAddress.getVersion() + ", device type: " //
							+ dllSecondaryAddress.getDeviceType() //
					);
				}

				for (WMbusProtocol device : this.parent.getDevices().values()) {
					SecondaryAddress deviceDllAddress = device.getDllSecondaryAddress();
					if (this.parent.isDebug()) {
						OpenemsComponent.logInfo(this.parent, this.log, "Checking Device " + device.getComponentId()
								+ " with radio address " + (device.getRadioAddress()) + ".");
					}

					/*
					 * This executes if no message from this device has been received yet. The
					 * reason why it is done this way: jmbus library stores the decryption key with
					 * the dllSecondaryAddress as identifier. The dllSecondaryAddress contains the
					 * radio address, but is not identical to it. But the number printed on a meter
					 * to identify it is the radio address, so this is what is entered in the meter
					 * config. The easiest way to get the dllSecondaryAddress is to simply take it
					 * from the first received message of that device and store it in
					 * deviceDllAddress. If that field is still null (no message yet from this
					 * device), extract the radio address from dllSecondaryAddress and identify by
					 * radio address. Then get and store the dllSecondaryAddress for this device and
					 * use it to register the decryption key. Further messages can then be
					 * identified directly by the dllSecondaryAddress.
					 */
					if (deviceDllAddress == null) {
						String radioAddress = device.getRadioAddress();
						String detectedAddress = String.valueOf(dllSecondaryAddress.getDeviceId());
						if (this.parent.isDebug()) {
							OpenemsComponent.logInfo(this.parent, this.log,
									"Not yet detected. Comparing " + radioAddress + " with " + detectedAddress + ".");
						}
						if (detectedAddress.equals(radioAddress)) {
							deviceDllAddress = dllSecondaryAddress; // Needed for if() branch in line 235.
							device.setDllSecondaryAddress(dllSecondaryAddress); // This needs to happen before
							// registerKey()
							device.registerKey(this.parent.getWmBusConnection()); // This won't work if
							// dllSecondaryAddress is
							// not set.
							OpenemsComponent.logInfo(this.parent, this.log, "Device " + device.getComponentId()
									+ " with radio address " + radioAddress + " has been detected.");
						} else {
							// Detected device is not this device from the list. Abort here, otherwise null
							// pointer
							// exception in next "if" because this device does not have the secondary
							// address set yet.
							if (this.parent.isDebug()) {
								OpenemsComponent.logInfo(this.parent, this.log,
										"The right device was not detected yet. "
												+ "Moving on to next device in list (if there are more).");
							}
							continue;
						}
					}

					// This executes if the message is from this device.
					if (deviceDllAddress.hashCode() == dllSecondaryAddress.hashCode()) {
						VariableDataStructure data = message.getVariableDataResponse();
						try {
							data.decode();
							device.updateAddress(data);
							if (device.isIdentifyByMeterNumber()) {
								/*
								 * This is needed to distinguishing between channel 1 and 2 for the Padpuls
								 * Relay. Both channels have identical radio addresses, only the meter number is
								 * different. Data has another secondary address, which is the
								 * "transport layer secondary address". It contains the meter number.
								 */

								// jmbus calls the meter number deviceId.
								String detectedMeterNumber = String.valueOf(data.getSecondaryAddress().getDeviceId());
								if (!detectedMeterNumber.equals(device.getMeterNumber())) {
									// This is not the right device. Abort and try next device in list.
									continue;
								}
							}
							device.setLogSignalStrength(message.getRssi());
							device.setResponse(data);
							if (this.parent.isScan()) {
								OpenemsComponent.logInfo(this.parent, this.log, String.valueOf(message));
							}
							device.setWrongDecryptionKey(false);
						} catch (DecodingException e) {
							device.setWrongDecryptionKey(true);
							OpenemsComponent.logError(this.parent, this.log,
									"Unable to fully decode received message for device " + device.getComponentId()
											+ " with ID " + device.getRadioAddress() + ". Check the decryption key!");
						}
						break;
					}
				}

			} catch (InterruptedException e) {
				OpenemsComponent.logWarn(this.parent, this.log, "Task was interrupted");
			}
		}

	}
}
