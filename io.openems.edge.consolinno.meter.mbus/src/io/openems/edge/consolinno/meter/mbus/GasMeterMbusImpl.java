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
import io.openems.edge.meter.api.GasMeterMbus;
import io.openems.edge.meter.api.MeterMbus;

@Designate(ocd = ConfigGasMeter.class, //
		factory = true//
)
@Component(name = "GasMeter.Mbus", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
public class GasMeterMbusImpl extends AbstractOpenemsMbusComponent
		implements OpenemsComponent, GasMeterMbus, MbusComponent {

	@Reference
	protected ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setMbus(BridgeMbus mbus) {
		super.setMbus(mbus);
	}

	private ConfigGasMeter config;

	private final AtomicBoolean energyFound = new AtomicBoolean(false);
	private final AtomicInteger dataRecordNumber = new AtomicInteger(0);

	public GasMeterMbusImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				GasMeterMbus.ChannelId.values(), //
				MeterMbus.ChannelId.values(), //
				MbusComponent.ChannelId.values(), //
				ChannelId.values() //
		);
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
	void activate(ComponentContext context, ConfigGasMeter config) {
		this.config = config;
		super.dynamicDataAddress = config.model() == GasMeterModel.AUTOSEARCH;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm,
				"Mbus", config.Mbus_Id(), config.pollingIntervalSeconds() > 0 ? config.pollingIntervalSeconds() : 0)) {
			return;
		}
		this.getTotalConsumedEnergyCubicMeterChannel()
				.onSetNextValue(value -> this.getMeterReadingChannel().setNextValue(value));
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected void addChannelDataRecords() {
		// TODO POWER
		this.channelDataRecordsList.add(0,
				new ChannelRecord(channel(GasMeterMbus.ChannelId.TOTAL_CONSUMED_ENERGY_CUBIC_METER),
						this.config.model().getTotalConsumptionEnergyAddress()));
		this.channelDataRecordsList.add(new ChannelRecord(this.channel(MeterMbus.ChannelId.TIMESTAMP_SECONDS), -1));
		this.channelDataRecordsList.add(new ChannelRecord(this.channel(MeterMbus.ChannelId.TIMESTAMP_STRING), -2));
		this.channelDataRecordsList
				.add(new ChannelRecord(channel(ChannelId.MANUFACTURER_ID), ChannelRecord.DataType.Manufacturer));
		this.channelDataRecordsList
				.add(new ChannelRecord(channel(ChannelId.DEVICE_ID), ChannelRecord.DataType.DeviceId));

	}

	@Override
	public void findRecordPositions(VariableDataStructure data) {
		// Search for the entries starting at the top of the list.
		if (this.config.model() == GasMeterModel.AUTOSEARCH && !this.energyFound.get()) {
			var dataRecords = data.getDataRecords();
			this.dataRecordNumber.getAndSet(0);
			dataRecords.forEach(record -> {
				var description = record.getDescription();
				if (!this.energyFound.get() && description == DataRecord.Description.ENERGY) {
					this.channelDataRecordsList.get(0).setDataRecordPosition(this.dataRecordNumber.get());
					this.energyFound.set(true);
				}
				this.dataRecordNumber.getAndIncrement();
			});
		}
	}

	@Override
	public String debugLog() {
		return "L: " + this.getMeterReading();
	}
}
