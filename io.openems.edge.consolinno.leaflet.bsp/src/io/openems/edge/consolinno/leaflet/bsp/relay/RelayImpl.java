package io.openems.edge.consolinno.leaflet.bsp.relay;

import java.util.Arrays;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.leaflet.bsp.core.LeafletCore;
import io.openems.edge.consolinno.leaflet.bsp.core.enums.ModuleType;
import io.openems.edge.io.api.DigitalOutput;

/**
 * Provides a Relay from the Consolinno Relay Module.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
	name = "Consolinno.Leaflet.Bsp.Relay", //
	immediate = true, //
	configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class RelayImpl extends AbstractOpenemsModbusComponent
	implements DigitalOutput, ModbusComponent, OpenemsComponent {

    private final Logger log = LoggerFactory.getLogger(RelayImpl.class);

    private Config config;
    private int relayOutputAddress;

    private final BooleanWriteChannel[] channelOut;
    private final StringBuilder debugBuilder = new StringBuilder();

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
	super.setModbus(modbus);
    }

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    private LeafletCore leafletCore;

    public RelayImpl() {
	super(//
		OpenemsComponent.ChannelId.values(), //
		ModbusComponent.ChannelId.values(), //
		Relay.values() //
	);
	this.channelOut = new BooleanWriteChannel[] { //
		this.channel(Relay.OUT) };
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsException {
	this.config = config;
	if (this.leafletCore != null) {
	    try {
		this.leafletCore.checkModulePresent(ModuleType.REL, config.module(), config.position().value,
			config.id());

		// Output coils start at 0 and not 1 like Analog Input
		this.relayOutputAddress = this.leafletCore.registerModule(ModuleType.REL, config.module(),
			config.position().value - 1);
		this.channel(Relay.NORMALLY_CLOSED).setNextValue(config.normallyClosed());
	    } catch (OpenemsException ex) {
		this.logError(this.log, ex.getMessage());
		throw ex;
	    }
	}
	if (super.activate(context, config.id(), config.alias(), config.enabled(), this.config.modbusUnitId(), this.cm,
		"Modbus", this.config.modbus_id())) {
	    return;
	}
	if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "leafletCore", config.leaflet_id())) {
	    return;
	}
    }

    @Deactivate
    protected void deactivate() {
	this.setAllRelais(false);
	this.leafletCore.unregisterModule(ModuleType.REL, this.config.module(), this.config.position().value);
	super.deactivate();
    }

    private void setAllRelais(boolean isOn) {
	Arrays.stream(this.channelOut).forEach(channel -> {
	    try {
		channel.setNextWriteValueFromObject(isOn);
	    } catch (OpenemsError.OpenemsNamedException e) {
		this.logWarn(this.log, "Couldn't set relais: " + channel.channelId() + " to " + isOn);
	    }
	});
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
	if (this.leafletCore == null) {
	    return null;
	}
	return new ModbusProtocol(this,
		new FC5WriteCoilTask(this.relayOutputAddress,
			(CoilElement) m(Relay.OUT, new CoilElement(this.relayOutputAddress),
				ElementToChannelConverter.INVERT_IF_TRUE(this.config.normallyClosed()))),
		new FC1ReadCoilsTask(this.relayOutputAddress, Priority.HIGH,
			m(Relay.OUT, new CoilElement(this.relayOutputAddress),
				ElementToChannelConverter.INVERT_IF_TRUE(this.config.normallyClosed()))));
    }

    @Override
    public BooleanWriteChannel[] digitalOutputChannels() {
	return this.channelOut;
    }

    @Override
    public String debugLog() {
	this.debugBuilder.setLength(0);
	var wc = ((BooleanReadChannel) this.channel(Relay.DEBUG_OUT)).value();
	this.appendToBuilder("WR", wc, this.debugBuilder);
	var rc = ((BooleanReadChannel) this.channel(Relay.OUT)).value();
	this.appendToBuilder("RD", rc, this.debugBuilder);
	var nc = ((BooleanReadChannel) this.channel(Relay.NORMALLY_CLOSED)).value();
	this.appendToBuilder("NC", nc, this.debugBuilder);
	return this.debugBuilder.toString();
    }

    private void appendToBuilder(String descriptor, Value<Boolean> val, StringBuilder builder) {
	builder.append(descriptor).append(" ");
	val.asOptional().ifPresentOrElse(value -> builder.append(value ? "1" : "0"), () -> {
	    builder.append("-");
	});
	builder.append(" ");
    }
}
