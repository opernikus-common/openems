package io.openems.edge.bridge.wmbus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

import org.openmuc.jmbus.wireless.WMBusConnection;
import org.openmuc.jmbus.wireless.WMBusListener;
import org.openmuc.jmbus.wireless.WMBusMessage;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.wmbus.api.BridgeWMbus;
import io.openems.edge.bridge.wmbus.api.WMbusProtocol;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = ConfigWmBus.class, factory = true)
@Component(name = "Bridge.WirelessMbus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE })
public class BridgeWMbusImpl extends AbstractOpenemsComponent implements BridgeWMbus, EventHandler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(BridgeWMbusImpl.class);

	public BridgeWMbusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeWMbus.ChannelId.values() //
		);
	}

	private final Map<String, WMbusProtocol> devices = new HashMap<>();
	private final WMbusWorker worker = new WMbusWorker(this);
	private WMBusConnection wMBusConnection;
	private String portName;
	private boolean scan;
	private boolean debug;

	@Activate
	protected void activate(ComponentContext context, ConfigWmBus configWmBus) {
		super.activate(context, configWmBus.id(), configWmBus.alias(), configWmBus.enabled());
		this.portName = configWmBus.portName();
		this.scan = configWmBus.scan();
		this.debug = configWmBus.debug();
		WMBusListener listener = new WmBusReceiver(this);
		this.worker.activate(configWmBus.id());
		WMBusConnection.WMBusSerialBuilder builder = new WMBusConnection.WMBusSerialBuilder(configWmBus.manufacturer(),
				listener, this.portName);
		builder.setMode(configWmBus.mode());

		try {
			this.wMBusConnection = builder.build();
			this._setWmbusCommunicationFailed(false);
		} catch (IOException e) {
			this._setWmbusCommunicationFailed(true);
			this.logError(this.log, "Connection via [" + this.portName + "] failed: " + e.getMessage());
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		try {
			this.wMBusConnection.close();
		} catch (IOException e) {
			this.logError(this.log, "Closing connection via [" + this.portName + "] failed. Reason: " + e.getMessage());
		}
		this.worker.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)) {
			this.worker.triggerNextRun();
		}
	}

	@Override
	public void addProtocol(String sourceId, WMbusProtocol protocol) {
		this.devices.put(sourceId, protocol);
	}

	@Override
	public void removeProtocol(String sourceId) {
		this.devices.remove(sourceId);
	}

	public static class WmBusReceiver implements WMBusListener {
		private final BridgeWMbusImpl parent;

		public WmBusReceiver(BridgeWMbusImpl parent) {
			this.parent = parent;
		}

		@Override
		public void newMessage(WMBusMessage message) {
			this.parent.messageQueue.add(message);
		}

		@Override
		public void discardedBytes(byte[] bytes) {
		}

		@Override
		public void stoppedListening(IOException cause) {
			this.parent.logError(this.parent.log,
					"Connection via [" + this.parent.portName + "] failed: " + cause.getMessage());
		}

	}

	private final LinkedBlockingDeque<WMBusMessage> messageQueue = new LinkedBlockingDeque<>();

	public LinkedBlockingDeque<WMBusMessage> getMessageQueue() {
		return this.messageQueue;
	}

	public Map<String, WMbusProtocol> getDevices() {
		return this.devices;
	}

	public boolean isScan() {
		return this.scan;
	}

	public boolean isDebug() {
		return this.debug;
	}

	public WMBusConnection getWmBusConnection() {
		return this.wMBusConnection;
	}

}
