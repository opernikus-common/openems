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
import io.openems.edge.bridge.mbus.api.MbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.HeatMeterMbus;
import io.openems.edge.meter.api.MeterMbus;

@Designate(ocd = ConfigHeatMeter.class, //
		factory = true //
)
@Component(name = "HeatMeter.Mbus", //
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

	private ConfigHeatMeter config;
	private final AtomicBoolean energyFound = new AtomicBoolean(false);
	private final AtomicBoolean powerFound = new AtomicBoolean(false);
	private final AtomicBoolean flowTemp = new AtomicBoolean(false);
	private final AtomicBoolean returnTemp = new AtomicBoolean(false);
	private final AtomicBoolean flowRate = new AtomicBoolean(false);
	private final AtomicInteger dataRecordNumber = new AtomicInteger(0);

	public HeatMeterMbusImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				HeatMeterMbus.ChannelId.values(), //
				MeterMbus.ChannelId.values(), //
				MbusComponent.ChannelId.values(), //
				ChannelId.values());
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MANUFACTURER_ID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)), //
		DEVICE_ID(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE));

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
		super.dynamicDataAddress = config.model() == HeatMeterModel.AUTOSEARCH;
		// use data record positions as specified in HeatMeterType
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
		this.channelDataRecordsList.add(0, new ChannelRecord(channel(HeatMeterMbus.ChannelId.READING_ENERGY),
				this.config.model().getTotalConsumptionEnergyAddress()));
		this.channelDataRecordsList.add(1, new ChannelRecord(channel(HeatMeterMbus.ChannelId.FLOW_TEMP),
				this.config.model().getFlowTempAddress()));
		this.channelDataRecordsList.add(2, new ChannelRecord(channel(HeatMeterMbus.ChannelId.RETURN_TEMP),
				this.config.model().getReturnTempAddress()));
		this.channelDataRecordsList.add(3, new ChannelRecord(channel(HeatMeterMbus.ChannelId.READING_POWER),
				this.config.model().getPowerAddress()));
		this.channelDataRecordsList.add(4, new ChannelRecord(channel(HeatMeterMbus.ChannelId.FLOW_RATE),
				this.config.model().getFlowRateAddress()));
		this.channelDataRecordsList.add(5,
				new ChannelRecord(channel(ChannelId.MANUFACTURER_ID), ChannelRecord.DataType.Manufacturer));
		this.channelDataRecordsList.add(6,
				new ChannelRecord(channel(ChannelId.DEVICE_ID), ChannelRecord.DataType.DeviceId));
		this.channelDataRecordsList.add(7, new ChannelRecord(this.channel(MeterMbus.ChannelId.TIMESTAMP_SECONDS), -1));
		this.channelDataRecordsList.add(8, new ChannelRecord(this.channel(MeterMbus.ChannelId.TIMESTAMP_STRING), -2));
	}

	@Override
	public void findRecordPositions(VariableDataStructure data) {
		// Search for the entries starting at the top of the list.
		// TODO TEST
		if (this.config.model() == HeatMeterModel.AUTOSEARCH && !this.autoSearchDone()) {
			var dataRecords = data.getDataRecords();
			this.dataRecordNumber.getAndSet(0);
			dataRecords.forEach(record -> {
				var description = record.getDescription();
				if (!this.energyFound.get() && description == DataRecord.Description.ENERGY) {
					this.channelDataRecordsList.get(0).setDataRecordPosition(this.dataRecordNumber.get());
					this.energyFound.set(true);
				} else if (!this.powerFound.get() && description == DataRecord.Description.POWER) {
					this.channelDataRecordsList.get(3).setDataRecordPosition(this.dataRecordNumber.get());
					this.powerFound.set(true);
				} else if (!this.flowTemp.get() && description == DataRecord.Description.FLOW_TEMPERATURE) {
					this.channelDataRecordsList.get(1).setDataRecordPosition(this.dataRecordNumber.get());
					this.flowTemp.set(true);
				} else if (!this.returnTemp.get() && description == DataRecord.Description.RETURN_TEMPERATURE) {
					this.channelDataRecordsList.get(2).setDataRecordPosition(this.dataRecordNumber.get());
					this.returnTemp.set(true);
				} else if (!this.flowRate.get() && description == DataRecord.Description.VOLUME_FLOW) {
					this.channelDataRecordsList.get(4).setDataRecordPosition(this.dataRecordNumber.get());
					this.flowRate.set(true);
				}
				this.dataRecordNumber.getAndIncrement();
			});
		}
	}

	private boolean autoSearchDone() {
		return this.energyFound.get() //
				&& this.powerFound.get() //
				&& this.flowTemp.get() //
				&& this.returnTemp.get() //
				&& this.flowRate.get();
	}

	@Override
	public String debugLog() {
		return "L: " + this.getMeterReading();
	}
}
