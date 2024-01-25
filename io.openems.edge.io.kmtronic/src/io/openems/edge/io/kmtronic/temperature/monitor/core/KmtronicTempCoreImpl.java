package io.openems.edge.io.kmtronic.temperature.monitor.core;

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
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.KMtronic.Temperature.Core", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})

public class KmtronicTempCoreImpl extends AbstractOpenemsComponent
		implements KmtronicTempCore, EventHandler, ModbusSlave, OpenemsComponent {

	@Reference
	protected ConfigurationAdmin cm;

	private final KmtronicTempReadWorker readWorker = new KmtronicTempReadWorker(this);

	private String ipAdress;

	protected int numberOfSensors;

	public KmtronicTempCoreImpl() {
		super(OpenemsComponent.ChannelId.values());
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.ipAdress = config.ip() + ":" + config.port();
		this.numberOfSensors = config.numberOfSensors();
		this.addDynamicChannels(config.numberOfSensors());
		this.readWorker.activate(this.id() + "query");
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.readWorker.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.readWorker.triggerNextRun();
		}
	}

	private void addDynamicChannels(int numberOfSensors) {
		for (var i = 0; i < numberOfSensors; i++) {
			var channelIdId = new ChannelIdImpl("ID_SENSOR_" + i, Doc.of(OpenemsType.STRING));
			this.addChannel(channelIdId);
			var channelIdName = new ChannelIdImpl("NAME_SENSOR_" + i, Doc.of(OpenemsType.STRING));
			this.addChannel(channelIdName);
			var channelIdTemp = new ChannelIdImpl("TEMP_SENSOR_" + i, Doc.of(OpenemsType.FLOAT));
			this.addChannel(channelIdTemp);
		}
	}

	public String getIpAdress() {
		return this.ipAdress;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode) //
		);
	}

}
