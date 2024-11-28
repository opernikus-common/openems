package io.openems.edge.evcharger.managed.base.test;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.evcharger.api.ManagedEvCharger;

public class DummyManagedEvCharger extends AbstractDummyOpenemsComponent<DummyManagedEvCharger>
		implements ManagedEvCharger, EventHandler, OpenemsComponent {

	public DummyManagedEvCharger(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ManagedEvCharger.ConfigChannelId.values(), //
				ManagedEvCharger.ChannelId.values() //
		);
	}

	@Override
	protected DummyManagedEvCharger self() {
		return this;
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
	}

}
