package io.openems.edge.consolinno.meter.mbus.wmbus;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.openmuc.jmbus.DataRecord;
import org.openmuc.jmbus.VariableDataStructure;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.bridge.wmbus.api.AbstractOpenemsWMbusComponent;
import io.openems.edge.bridge.wmbus.api.BridgeWMbus;
import io.openems.edge.bridge.wmbus.api.WMbusProtocol;
import io.openems.edge.bridge.wmbus.api.WmbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.MeterMbus;
import io.openems.edge.meter.api.WaterMeterMbus;

@Designate(ocd = ConfigWaterMeterWirelessMbus.class, factory = true)
@Component(name = "WaterMeter.WirelessMbus", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
public class WaterMeterWirelessMbusImpl extends AbstractOpenemsWMbusComponent
		implements OpenemsComponent, WaterMeterMbus, WmbusComponent {

	private ConfigWaterMeterWirelessMbus config;

	private final AtomicBoolean volumePositionFound = new AtomicBoolean(false);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setWmbus(BridgeWMbus wmbus) {
		super.setWmbus(wmbus);
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SIGNAL_STRENGTH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DECIBEL_MILLIWATT)),
		MANUFACTURER_ID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)), //
		DEVICE_ID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public WaterMeterWirelessMbusImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				WaterMeterMbus.ChannelId.values(), //
				MeterMbus.ChannelId.values(), //
				WmbusComponent.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, ConfigWaterMeterWirelessMbus config) {
		this.config = config;
		super.dynamicDataAddress = this.isAutoSearch();
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.radioAddress(), this.cm, "Wmbus",
				config.Wmbus_Id(), config.key())) {
			return;
		}
		this.getTotalConsumedWaterChannel().onSetNextValue(value -> this.getMeterReadingChannel().setNextValue(value));
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	protected WMbusProtocol defineWMbusProtocol(String key) {
		WMbusProtocol protocol = new WMbusProtocol(this, key,
				new ChannelRecord(this.channel(WaterMeterMbus.ChannelId.TOTAL_CONSUMED_WATER),
						this.config.model().getVolumeCounterPosition()), //
				new ChannelRecord(this.channel(MeterMbus.ChannelId.TIMESTAMP_SECONDS), -1), //
				new ChannelRecord(this.channel(MeterMbus.ChannelId.TIMESTAMP_STRING), -2), //
				new ChannelRecord(this.channel(ChannelId.MANUFACTURER_ID), ChannelRecord.DataType.Manufacturer), //
				new ChannelRecord(this.channel(ChannelId.DEVICE_ID), ChannelRecord.DataType.DeviceId)); //
		switch (this.config.model()) {
		case RELAY_PADPULS_M2W_CHANNEL1 -> {
			String meterNumber1 = super.getRadioAddress().substring(2, 8) + "01";
			protocol.setMeterNumber(meterNumber1);
		}
		case RELAY_PADPULS_M2W_CHANNEL2 -> {
			String meterNumber2 = super.getRadioAddress().substring(2, 8) + "02";
			protocol.setMeterNumber(meterNumber2);
		}
		}
		return protocol;
	}

	@Override
	public void setLogSignalStrength(int signalStrength) {
		this.channel(ChannelId.SIGNAL_STRENGTH).setNextValue(signalStrength);
	}

	@Override
	public void findRecordPositions(VariableDataStructure data, List<ChannelRecord> channelDataRecordsList) {

		// Search for the entries starting at the top of the list.
		if (this.isAutoSearch() && !this.volumePositionFound.get()) {
			List<DataRecord> dataRecords = data.getDataRecords();
			var dataRecordNumber = new AtomicInteger(0);
			var add = 1;
			if (this.config.model() == WaterMeterModelWirelessMbus.ENGELMANN_WATERSTAR_M) {
				Collections.reverse(dataRecords);
				dataRecordNumber.set(dataRecords.size() - 1);
				add = -1;
			}
			int finalAdd = add;
			dataRecords.forEach(record -> {
				if (record.getDescription() == DataRecord.Description.VOLUME && !this.volumePositionFound.get()) {
					this.protocol.getChannelDataRecordsList().get(0).setDataRecordPosition(dataRecordNumber.get());
					this.volumePositionFound.set(true);
				}
				dataRecordNumber.getAndAdd(finalAdd);
			});
		}
	}

	private boolean isAutoSearch() {
		return this.config.model() == WaterMeterModelWirelessMbus.AUTOSEARCH
				|| this.config.model() == WaterMeterModelWirelessMbus.ENGELMANN_WATERSTAR_M;
	}

	@Override
	public String debugLog() {
		return "L: " + this.getMeterReading();
	}
}
