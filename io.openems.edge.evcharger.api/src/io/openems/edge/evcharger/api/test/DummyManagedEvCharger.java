package io.openems.edge.evcharger.api.test;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.evcharger.api.EvCharger;
import io.openems.edge.evcharger.api.ManageableEvCharger;
import io.openems.edge.evcharger.api.data.Iec62196Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.test.DummyTimedata;

public class DummyManagedEvCharger extends AbstractDummyOpenemsComponent<DummyManagedEvCharger>
		implements ManageableEvCharger, EvCharger, TimedataProvider, EventHandler, OpenemsComponent {

	public DummyManagedEvCharger(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvCharger.ConfigChannelId.values(), //
				EvCharger.RawChannelId.values(), //
				EvCharger.ChannelId.values(), //
				ManageableEvCharger.ChannelId.values() //
		);
	}

	@Override
	protected DummyManagedEvCharger self() {
		return this;
	}

	@Override
	public void handleEvent(Event event) {

		this._setIec62196Status(Iec62196Status.CHARGING);
		this._setRawCurrentL1(6000);
		this._setRawCurrentL2(6000);
		this._setRawCurrentL3(6000);
	}

	@Override
	public void applyCurrent(int current, int limitPhases) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public MeterType getMeterType() {
		return null;
	}

	@Override
	public Timedata getTimedata() {
		return new DummyTimedata("td");
	}
}
