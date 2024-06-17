package io.openems.edge.consolinno.meter.mbus;

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
import io.openems.edge.bridge.mbus.api.AbstractOpenemsMbusComponent;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.bridge.mbus.api.ChannelRecord.DataType;
import io.openems.edge.bridge.mbus.api.MbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.MeterMbus;
import io.openems.edge.meter.api.WaterMeterMbus;

@Designate(ocd = ConfigWaterMeter.class, factory = true)
@Component(name = "WaterMeter.Mbus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)

public class WaterMeterMbusImpl extends AbstractOpenemsMbusComponent
		implements OpenemsComponent, WaterMeterMbus, MbusComponent {

	private ConfigWaterMeter config;

	private final AtomicBoolean volumePositionFound = new AtomicBoolean(false);

	@Reference
	protected ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setMbus(BridgeMbus mBus) {
		super.setMbus(mBus);
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MANUFACTURER_ID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)), //
		DEVICE_ID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public WaterMeterMbusImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				WaterMeterMbus.ChannelId.values(), //
				MeterMbus.ChannelId.values(), //
				MbusComponent.ChannelId.values(), //
				ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, ConfigWaterMeter config) {
		this.config = config;
		super.dynamicDataAddress = this.config.model() == WaterMeterModelMbus.AUTOSEARCH;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm,
				"Mbus", config.wmbus_Id(), config.pollingIntervalSeconds() > 0 ? config.pollingIntervalSeconds() : 0)) {
			return;
		}
		this.getTotalConsumedWaterChannel().onSetNextValue(value -> this.getMeterReadingChannel().setNextValue(value));

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected void addChannelDataRecords() {

		this.channelDataRecordsList.add(0,
				new ChannelRecord(this.channel(WaterMeterMbus.ChannelId.TOTAL_CONSUMED_WATER),
						this.config.model().getVolumeCounterPosition()));

		this.channelDataRecordsList.add(new ChannelRecord(this.channel(MeterMbus.ChannelId.TIMESTAMP_SECONDS), -1));

		this.channelDataRecordsList.add(new ChannelRecord(this.channel(MeterMbus.ChannelId.TIMESTAMP_STRING), -2));
		this.channelDataRecordsList
				.add(new ChannelRecord(this.channel(ChannelId.MANUFACTURER_ID), DataType.Manufacturer));
		this.channelDataRecordsList.add(new ChannelRecord(this.channel(ChannelId.DEVICE_ID), DataType.DeviceId));
	}

	@Override
	public void findRecordPositions(VariableDataStructure data) {
		// Search for the entries starting at the top of the list.
		if (this.config.model() == WaterMeterModelMbus.AUTOSEARCH && !this.volumePositionFound.get()) {
			var dataRecords = data.getDataRecords();
			var dataRecordNumber = new AtomicInteger(0);
			dataRecords.forEach(record -> {
				if (!this.volumePositionFound.get() && record.getDescription() == DataRecord.Description.VOLUME) {
					this.channelDataRecordsList.get(0).setDataRecordPosition(dataRecordNumber.get());
					this.volumePositionFound.set(true);
				}
				dataRecordNumber.getAndIncrement();
			});
		}
	}

	@Override
	public String debugLog() {
		return "L: " + this.getMeterReading();
	}

}
