package io.openems.edge.bridge.mbus.test;

import java.util.HashMap;
import java.util.Map;

import org.openmuc.jmbus.MBusConnection;

import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.MbusTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyMbusBridgeImpl extends AbstractOpenemsComponent implements BridgeMbus {

	public DummyMbusBridgeImpl(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeMbus.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	private final Map<String, MbusTask> tasks = new HashMap<>();

	@Override
	public void addTask(String sourceId, MbusTask task) {
		this.tasks.put(sourceId, task);
	}

	@Override
	public MBusConnection getmBusConnection() {
		return null;
	}

	@Override
	public void removeTask(String sourceId) {
		this.tasks.remove(sourceId);
	}
}
