package io.openems.edge.bridge.mbus.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.openmuc.jmbus.VariableDataStructure;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractOpenemsMbusComponent extends AbstractOpenemsComponent implements MbusComponent {

	protected final List<ChannelRecord> channelDataRecordsList = new ArrayList<>();
	private final AtomicReference<BridgeMbus> mBus = new AtomicReference<>(null);

	private static final int NOT_DEFINED = -404;
	private Integer primaryAddress = null;
	protected boolean dynamicDataAddress = false; // For M-Bus devices that have dynamic addresses of their data.

	protected AbstractOpenemsMbusComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	public List<ChannelRecord> getChannelDataRecordsList() {
		return this.channelDataRecordsList;
	}

	public Integer getPrimaryAddress() {
		return this.primaryAddress;
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
	 * @param primaryAddress Primary address of the M-Bus device. Typically,
	 *                       'config.primaryAddress'
	 * @param cm             An instance of ConfigurationAdmin. Receive it
	 *                       using @Reference
	 * @param mbusReference  The name of the @Reference setter method for the M-Bus
	 *                       bridge
	 * @param mbusId         The ID of the M-Bus bridge. Typically
	 *                       'config.mbus_id()'
	 * @return true if the target filter was updated. You may use it to abort the
	 *         'activate()' method.
	 */
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int primaryAddress,
			ConfigurationAdmin cm, String mbusReference, String mbusId) {
		return this.activate(context, id, alias, enabled, primaryAddress, cm, mbusReference, mbusId, 0);
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
	 * @param primaryAddress Primary address of the M-Bus device. Typically,
	 *                       'config.primaryAddress'
	 * @param cm             An instance of ConfigurationAdmin. Receive it
	 *                       using @Reference
	 * @param mbusReference  The name of the @Reference setter method for the M-Bus
	 *                       bridge
	 * @param mbusId         The ID of the M-Bus bridge. Typically
	 *                       'config.mbus_id()'
	 * @param pollInterval   the pollingInterval in seconds-> interval to request the mbus meter
	 *                       data values. 0 for every cycle
	 * @return true if the target filter was updated. You may use it to abort the
	 *         'activate()' method.
	 */
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int primaryAddress,
			ConfigurationAdmin cm, String mbusReference, String mbusId, int pollInterval) {
		super.activate(context, id, alias, enabled);
		this.primaryAddress = primaryAddress;

		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "mbus", mbusId)) {
			return true;
		}
		BridgeMbus mbus = this.mBus.get();
		if (this.isEnabled() && mbus != null) {
			this.addChannelDataRecords();
			mbus.addTask(id, new MbusTask(mbus, this, pollInterval));
		}
		return false;
	}

	protected void activate(String id) {
		throw new IllegalArgumentException("Use the other activate() method.");
	}

	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() for M-Bus components!");
	}

	@Override
	protected void modified(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("Use the other activate() for Modbus components!");
	}

	@Override
	protected void deactivate() {
		super.deactivate();
		var mBus = this.mBus.getAndSet(null);
		if (mBus != null) {
			mBus.removeTask(this.id());
		}
	}

	/**
	 * Define channels of the mbus device and the record position of its
	 * corresponding values or the datatype if the channel displays secondary
	 * address values.
	 */
	protected abstract void addChannelDataRecords();

	/**
	 * Set the Mbus bridge. Should be called by @Reference
	 *
	 * @param mBus the BridgeMbus Reference
	 */
	protected void setMbus(BridgeMbus mBus) {
		this.mBus.set(mBus);
	}

	/**
	 * Unset the Mbus bridge. Should be called by @Reference
	 *
	 * @param mbus the BridgeMbus Reference
	 */
	protected void unsetMbus(BridgeMbus mbus) {
		this.mBus.compareAndSet(mbus, null);
		if (mbus != null) {
			mbus.removeTask(this.id());
		}
	}

	protected BridgeMbus getMBus() {
		return this.mBus.get();
	}

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
	 * @param data                   The data received from the M-Bus device.
	 */
	public abstract void findRecordPositions(VariableDataStructure data);

	/**
	 * Internal Method for Generic MBus Meter. This determines if a configured
	 * channel has a correct address or is "undefined" e.g. shouldn't be handled.
	 *
	 * @param channel the channel of the meter.
	 * @param address the address where the channel receives its data.
	 */
	protected void addToChannelDataRecordListIfDefined(Channel<?> channel, int address) {
		if (address == NOT_DEFINED) {
			return;
		}
		this.channelDataRecordsList.add(new ChannelRecord(channel, address));
	}
}
