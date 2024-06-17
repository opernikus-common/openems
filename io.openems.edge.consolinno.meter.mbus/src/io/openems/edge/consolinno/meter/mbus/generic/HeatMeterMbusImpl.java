package io.openems.edge.consolinno.meter.mbus.generic;

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
import io.openems.edge.bridge.mbus.api.MbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.HeatMeterMbus;
import io.openems.edge.meter.api.MeterMbus;

@Designate(ocd = ConfigHeatMeter.class, //
		factory = true //
)
@Component(name = "HeatMeter.Mbus.Generic", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
public class HeatMeterMbusImpl extends AbstractOpenemsMbusComponent
		implements OpenemsComponent, HeatMeterMbus, MbusComponent {

	@Reference
	protected ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setMbus(BridgeMbus mBus) {
		super.setMbus(mBus);
	}

	public HeatMeterMbusImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				HeatMeterMbus.ChannelId.values(), //
				MeterMbus.ChannelId.values(), //
				MbusComponent.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	private ConfigHeatMeter config;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MANUFACTURER_ID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)), //
		DEVICE_ID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)) //
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

	@Activate
	void activate(ComponentContext context, ConfigHeatMeter config) {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm,
				"Mbus", config.Mbus_Id(), config.pollingIntervalSeconds() > 0 ? config.pollingIntervalSeconds() : 0)) {
			return;
		}
		this.getReadingEnergyChannel().onSetNextValue(value -> this.getMeterReadingChannel().setNextValue(value));
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected void addChannelDataRecords() {
		this.addToChannelDataRecordListIfDefined(channel(HeatMeterMbus.ChannelId.READING_ENERGY),
				this.config.totalConsumedEnergyAddress());
		this.addToChannelDataRecordListIfDefined(channel(HeatMeterMbus.ChannelId.FLOW_TEMP),
				this.config.flowTempAddress());
		this.addToChannelDataRecordListIfDefined(channel(HeatMeterMbus.ChannelId.RETURN_TEMP),
				this.config.returnTempAddress());
		this.addToChannelDataRecordListIfDefined(channel(HeatMeterMbus.ChannelId.READING_POWER),
				this.config.meterReadingPower());
		this.addToChannelDataRecordListIfDefined(channel(HeatMeterMbus.ChannelId.FLOW_RATE),
				this.config.flowRateAddress());
		this.channelDataRecordsList
				.add(new ChannelRecord(channel(ChannelId.MANUFACTURER_ID), ChannelRecord.DataType.Manufacturer));
		this.channelDataRecordsList
				.add(new ChannelRecord(channel(ChannelId.DEVICE_ID), ChannelRecord.DataType.DeviceId));
		this.channelDataRecordsList.add(new ChannelRecord(this.channel(MeterMbus.ChannelId.TIMESTAMP_SECONDS), -1));
		this.channelDataRecordsList.add(new ChannelRecord(this.channel(MeterMbus.ChannelId.TIMESTAMP_STRING), -2));
	}

	@Override
	public void findRecordPositions(VariableDataStructure data) {

	}

	@Override
	public String debugLog() {
		return "L: " + this.getMeterReading();
	}

}
