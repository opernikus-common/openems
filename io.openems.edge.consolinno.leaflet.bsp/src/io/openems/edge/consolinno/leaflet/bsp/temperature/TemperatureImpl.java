package io.openems.edge.consolinno.leaflet.bsp.temperature;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.consolinno.leaflet.bsp.core.LeafletCore;
import io.openems.edge.consolinno.leaflet.bsp.core.enums.ModuleType;
import io.openems.edge.thermometer.api.Thermometer;

/**
 * Provides a Consolinno Temperature sensor. It communicates via Modbus with the
 * Temperature Module, gets it's addresses via the LeafletCore and sets it's
 * Temperature into the Thermometer Nature.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Consolinno.Leaflet.Bsp.Temperature", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TemperatureImpl extends AbstractOpenemsModbusComponent
		implements Thermometer, ModbusComponent, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(TemperatureImpl.class);
	private int tempAnalogInAddress;
	private Config config;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private LeafletCore leafletCore;

	public TemperatureImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Thermometer.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (this.leafletCore != null) {
			try {
				this.leafletCore.checkModulePresent(ModuleType.TMP, config.module(), config.position().value,
						config.id());
				this.tempAnalogInAddress = this.leafletCore.registerModule(ModuleType.TMP, config.module(),
						config.position().value);
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
		this.leafletCore.unregisterModule(ModuleType.TMP, this.config.module(), this.config.position().value);
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		if (this.leafletCore == null) {
			return null;
		}
		return new ModbusProtocol(this, new FC4ReadInputRegistersTask(this.tempAnalogInAddress, //
				Priority.LOW, //
				m(Thermometer.ChannelId.TEMPERATURE, new SignedWordElement(this.tempAnalogInAddress))));
	}

	@Override
	public String debugLog() {
		return getTemperature().isDefined() ? getTemperature().asString() : "-";
	}

}
