package io.openems.edge.bridge.wmbus.test;

import java.util.HashMap;
import java.util.Map;

import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.wmbus.api.BridgeWMbus;
import io.openems.edge.bridge.wmbus.api.WMbusProtocol;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyWmbusBridgeImpl extends AbstractOpenemsComponent implements BridgeWMbus {

	public DummyWmbusBridgeImpl(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeMbus.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	private final Map<String, WMbusProtocol> protocols = new HashMap<>();

	@Override
	public void addProtocol(String sourceId, WMbusProtocol protocol) {
		this.protocols.put(sourceId, protocol);
	}

	@Override
	public void removeProtocol(String sourceId) {
		this.removeProtocol(sourceId);
	}
}
