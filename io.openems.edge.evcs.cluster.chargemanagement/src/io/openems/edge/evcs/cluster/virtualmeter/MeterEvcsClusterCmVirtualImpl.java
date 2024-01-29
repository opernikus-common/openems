package io.openems.edge.evcs.cluster.virtualmeter;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.evcs.cluster.chargemanagement.EvcsClusterChargeMgmt;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.VirtualMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Virtual.ClusterChargemanagement", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class MeterEvcsClusterCmVirtualImpl extends AbstractOpenemsComponent implements MeterEvcsClusterCmVirtual,
		VirtualMeter, ElectricityMeter, EventHandler, OpenemsComponent, ModbusSlave {

	private final ClusterChannelMapping channelManager = new ClusterChannelMapping(this);
	private EvcsClusterChargeMgmt evcsClusterChargeMgmt;
	private Config config;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	public MeterEvcsClusterCmVirtualImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterEvcsClusterCmVirtual.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.channel(MeterEvcsClusterCmVirtual.ChannelId.MISSING_CLUSTER).setNextValue(true);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.channelManager.deactivate();
		this.evcsClusterChargeMgmt = null;
	}

	@Override
	public void handleEvent(Event event) {

		// must not use OSGI reference mechanism for Cluster because this leads to a
		// circular reference.

		if (this.evcsClusterChargeMgmt != null) {
			if (this.evcsClusterChargeMgmt.isActivated()) {
				return;
			}
			// evcsClusterChargeMgmt configuration has changed. remap new object.
			this.channelManager.deactivate();
		}
		try {
			this.evcsClusterChargeMgmt = this.componentManager.getComponent(this.config.evcsClusterChargeMgmtId());
			this.channel(MeterEvcsClusterCmVirtual.ChannelId.MISSING_CLUSTER).setNextValue(false);
			this.channelManager.activate(this.evcsClusterChargeMgmt);
			return;
		} catch (OpenemsNamedException e) {
			return;
		}
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.CONSUMPTION_NOT_METERED;
	}

	@Override
	public boolean addToSum() {
		return false;
	}

	@Override
	public String debugLog() {
		return this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode));
	}

}
