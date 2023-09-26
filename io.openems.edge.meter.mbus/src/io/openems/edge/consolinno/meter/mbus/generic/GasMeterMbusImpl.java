package io.openems.edge.consolinno.meter.mbus.generic;

import io.openems.edge.bridge.mbus.api.MbusComponent;
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
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.GasMeterMbus;
import io.openems.edge.meter.api.MeterMbus;

@Designate(ocd = ConfigGasMeter.class, //
		factory = true //
)
@Component(name = "Gasmeter.Mbus.Generic", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		immediate = true //
)
public class GasMeterMbusImpl extends AbstractOpenemsMbusComponent implements OpenemsComponent, GasMeterMbus, MbusComponent {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setMbus(BridgeMbus mbus) {
		super.setMbus(mbus);
	}

	private ConfigGasMeter config;

	public GasMeterMbusImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				GasMeterMbus.ChannelId.values(), //
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

		public Doc doc() {
			return this.doc;
		}
	}


	@Activate
	void activate(ComponentContext context, ConfigGasMeter config) {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm,
				"Mbus", config.Mbus_id(), config.pollingIntervalSeconds() > 0 ? config.pollingIntervalSeconds() : 0)) {
			return;
		}
		this.getTotalConsumedEnergyCubicMeterChannel().onSetNextValue(value -> this.getMeterReadingChannel().setNextValue(value));
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected void addChannelDataRecords() {
		this.addToChannelDataRecordListIfDefined(channel(GasMeterMbus.ChannelId.READING_POWER), this.config.meterReading());
		this.addToChannelDataRecordListIfDefined(channel(GasMeterMbus.ChannelId.FLOW_RATE), this.config.flowRateAddress());
		this.addToChannelDataRecordListIfDefined(channel(GasMeterMbus.ChannelId.TOTAL_CONSUMED_ENERGY_CUBIC_METER),
				this.config.totalConsumedEnergyAddress());
		this.addToChannelDataRecordListIfDefined(channel(GasMeterMbus.ChannelId.FLOW_TEMP), this.config.flowTempAddress());
		this.addToChannelDataRecordListIfDefined(channel(GasMeterMbus.ChannelId.RETURN_TEMP),
				this.config.returnTempAddress());
		this.channelDataRecordsList.add(new ChannelRecord(channel(MeterMbus.ChannelId.TIMESTAMP_SECONDS), -1));
		this.channelDataRecordsList.add(new ChannelRecord(channel(MeterMbus.ChannelId.TIMESTAMP_STRING), -2));
		this.channelDataRecordsList
				.add(new ChannelRecord(channel(ChannelId.DEVICE_ID), ChannelRecord.DataType.DeviceId));
		this.channelDataRecordsList
				.add(new ChannelRecord(channel(ChannelId.MANUFACTURER_ID), ChannelRecord.DataType.Manufacturer));

	}

	@Override
	public void findRecordPositions(VariableDataStructure data) {
		// Not needed so far.
	}

	@Override
	public String debugLog() {
		return "L: " + this.getMeterReading();
	}

}