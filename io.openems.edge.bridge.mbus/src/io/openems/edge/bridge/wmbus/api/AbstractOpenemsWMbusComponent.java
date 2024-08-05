package io.openems.edge.bridge.wmbus.api;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.openmuc.jmbus.VariableDataStructure;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractOpenemsWMbusComponent extends AbstractOpenemsComponent implements WmbusComponent {

	protected final Logger log = LoggerFactory.getLogger(AbstractOpenemsWMbusComponent.class);

	private String radioAddress;

	protected static final int VALID_RADIO_ADDRESS_LENGTH = 8;
	protected boolean dynamicDataAddress = false;
	protected WMbusProtocol protocol = null;

	protected AbstractOpenemsWMbusComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Call this method from Component implementations activate().
	 * 
	 * @param context        ComponentContext of this component. Receive it from
	 *                       parameter for @Activate
	 * @param id             ID of this component. Typically 'config.id()'
	 * @param alias          Human-readable name of this Component. Typically
	 *                       'config.alias()'. Defaults to 'id' if empty
	 * @param enabled        Whether the component should be enabled. Typically
	 *                       'config.enabled()'
	 * @param radioAddress   Device Id of the M-Bus device, usually printed on the
	 *                       casing. Typically 'config.radioAddress'
	 * @param cm             An instance of ConfigurationAdmin. Receive it
	 *                       using @Reference
	 * @param wmbusReference The name of the @Reference setter method for the M-Bus
	 *                       bridge
	 * @param wmbusId        The ID of the M-Bus bridge. Typically
	 *                       'config.wmbusBridgeId()'
	 * @param key            The decryption key for the encrypted data sent by the
	 *                       device.
	 * @return true if the target filter was updated. You may use it to abort the
	 *         activate() method.
	 */
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, String radioAddress,
			ConfigurationAdmin cm, String wmbusReference, String wmbusId, String key) {
		super.activate(context, id, alias, enabled);
		// update filter for 'WirelessMbus'
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "Wmbus", wmbusId)) {
			return true;
		}
		this.radioAddress = radioAddress;
		if (radioAddress.length() != VALID_RADIO_ADDRESS_LENGTH) {
			this.logError(this.log, "The radio address needs to be 8 characters long. The entered radio address "
					+ this.radioAddress + " is " + this.radioAddress.length() + " characters long. Cannot activate.");
			this._setWmbusRadioAddressLengthWrong(true);
			return true;
		}
		this._setWmbusRadioAddressLengthWrong(false);
		BridgeWMbus wmbus = this.wmbusId.get();
		if (this.isEnabled() && wmbus != null && !this.getWmbusRadioAddressLengthWrong().orElse(false)) {
			wmbus.addProtocol(this.id(), this.getWMbusProtocol(key));
		}
		return false;
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		BridgeWMbus wmbus = this.wmbusId.getAndSet(null);
		if (wmbus != null) {
			wmbus.removeProtocol(this.id());
		}
	}

	public String getRadioAddress() {
		return this.radioAddress;
	}

	private final AtomicReference<BridgeWMbus> wmbusId = new AtomicReference<BridgeWMbus>(null);

	/**
	 * Set the WMbus bridge. Should be called by @Reference
	 *
	 * @param wmbus the BridgeWMbus Reference
	 */
	protected void setWmbus(BridgeWMbus wmbus) {
		this.wmbusId.set(wmbus);
	}

	/**
	 * Unset the WMbus bridge. Should be called by @Reference
	 *
	 * @param wmbus the BridgeWMbus Reference
	 */
	protected void unsetWMbus(BridgeWMbus wmbus) {
		this.wmbusId.compareAndSet(wmbus, null);
		if (wmbus != null) {
			wmbus.removeProtocol(this.id());
		}
	}

	private WMbusProtocol getWMbusProtocol(String key) {
		WMbusProtocol protocol = this.protocol;
		if (protocol != null) {
			return protocol;
		}
		this.protocol = this.defineWMbusProtocol(key);
		return this.protocol;
	}

	/**
	 * Defines the WMbus protocol. Here you define channels of the wmbus device and
	 * the record position of its corresponding values or the datatype if the
	 * channel displays secondary address values.
	 *
	 * @param key The decryption key for the encrypted data sent by the device.
	 *
	 * @return the WMbusProtocol
	 */
	protected abstract WMbusProtocol defineWMbusProtocol(String key);

	/**
	 * Some meters change the record positions in their data during runtime. This
	 * option accounts for that. When enabled, it checks for the correctness of the
	 * record position by comparing the unit of the channel with the unit of the
	 * data on that record position. If it is a mismatch, the records are searched
	 * to find a data item with matching unit.
	 *
	 * @return use dynamicDataAddress true/false
	 */
	public boolean hasDynamicDataAddress() {
		return this.dynamicDataAddress;
	}

	/**
	 * If "dynamicDataAddress" is true, this method is called. It checks for the
	 * correctness of the record position by comparing the unit of the channel with
	 * the unit of the data on that record position. If it is a mismatch, the
	 * records are searched to find a data item with matching unit.
	 *
	 * @param data                   The data received from the WM-Bus device.
	 *
	 * @param channelDataRecordsList The list of channelDataRecords, where the
	 *                               addresses are stored. The method should modify
	 *                               the addresses in this list.
	 */
	public abstract void findRecordPositions(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList);

	public abstract void setLogSignalStrength(int signalStrength);

}
