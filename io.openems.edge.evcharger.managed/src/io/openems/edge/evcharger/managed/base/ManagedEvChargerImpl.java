package io.openems.edge.evcharger.managed.base;

import java.time.Clock;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcharger.api.EvChargerTools;
import io.openems.edge.evcharger.api.ManageableEvCharger;
import io.openems.edge.evcharger.api.ManagedEvCharger;
import io.openems.edge.evcharger.api.data.Iec62196Status;
import io.openems.edge.evcharger.api.data.Phases;
import io.openems.edge.evcharger.api.data.Status;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "EvCharger.ManagedEvCharger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class ManagedEvChargerImpl extends AbstractOpenemsComponent
		implements ManagedEvCharger, EventHandler, OpenemsComponent {

	// private final Logger log =
	// LoggerFactory.getLogger(ManagedEvChargerImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Sum sum;

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	private ManageableEvCharger evCharger;

	private Config config;

	public ManagedEvChargerImpl() {
		this(Clock.systemDefaultZone());
	}

	protected ManagedEvChargerImpl(Clock clock) {
		// TODO was passiert mit der Clock?
		super(//
				OpenemsComponent.ChannelId.values(), //
				ManagedEvCharger.ConfigChannelId.values(), //
				ManagedEvCharger.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.modify(config);

		this.addListener();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Modified
	private void modified(ComponentContext compContext, Config config) {
		this.config = config;
		super.modified(compContext, config.id(), config.alias(), config.enabled());
		this.modify(config);

	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> this.channelUpdate();
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> this.executeWrite();
		}
	}

	private void modify(Config config) {
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "evcharger", config.evcharger_id())) {
			return;
		}
		if (!config.enabled()) {
			return;
		}
		this._setOperationMode(this.config.operationMode());
		this._setPriority(this.config.priority());
		try {
			this.setSessionEnergyLimit(this.config.energySessionLimit());
			this._setWarnConfigEnergyLimit(false);
		} catch (Exception e) {
			this._setWarnConfigEnergyLimit(true);
		}
		this._setPowerPrecision(1);
		this._setIsSessionSuspendable(false);
		this._setSessionPhases(Phases.THREE_PHASE);

		this.evCharger._setStatus(Status.NO_VEHICLE);

	}

	private void addListener() {
		this.addStatusListener();
		this.getSessionEnergyLimitChannel().onSetNextWrite(val -> {
			this._setSessionEnergyLimit(val);
		});

	}

	private void addStatusListener() {
		this.evCharger.getIec62196StatusChannel().onSetNextValue(val -> {
			if (!val.isDefined()) {
				this.evCharger._setStatus(null);
				return;
			}
			Status s = switch ((Iec62196Status) val.asEnum()) {
			case CHARGING -> Status.CHARGING;
			case CHARGING_VENTILATION -> Status.CHARGING;
			case DEACTIVATED -> Status.DEACTIVATED;
			case ERROR -> Status.ERROR;
			case NO_VEHICLE -> {
				yield Status.NO_VEHICLE;
			}
			case UNDEFINED -> Status.ERROR;
			case VEHICLE_DETECTED -> {
				yield Status.VEHICLE_DETECTED;
				// TODO set Status.CHARGING_FINISHED
			}
			default -> Status.NO_VEHICLE;
			};
			this.evCharger._setStatus(s);
		});
	}

	private void channelUpdate() {

		/*
		 * TODO calculate this._setPowerPrecision(1);
		 * 
		 * TODO abpruefen this._setIsSessionSuspendable(false);
		 * 
		 * TODO aktualisieren this._setSessionPhases(Phases.THREE_PHASE);
		 * 
		 * -fortlaufend context aktualisieren
		 * 
		 */

		// OemsTools.logComponentChannels(this, this.log);
		// logDebug(this.log, "Next charge power: " + this.config.alias() + " W");
		// TODO reagieren auf
		// this.evCharger.setOperationMode(this.config.operationMode());

	}

	private void executeWrite() {
		if (this.getActivePowerPerPhaseChannel().getNextWriteValue().isEmpty()) {
			return;
		}
		var phasePowerToApply = this.getActivePowerPerPhaseChannel().getNextWriteValueAndReset();
		var current = EvChargerTools.convertWattToMilliampere(phasePowerToApply.get(), this.maxUsablePhases());

		// TODO nach SessionEnergyLimit aufhoeren

		try {
			this.evCharger.applyCurrent(current, this.maxUsablePhases());
		} catch (Exception e) {
			e.printStackTrace();
		}
		this._setActivePowerPerPhase(phasePowerToApply.get());
	}

	private int maxUsablePhases() {
		if (!this.evCharger.isNumberOfPhasesLimitable()) {
			return 3;
		}
		return TypeUtils.fitWithin(1, 3, this.getLimitPhases().orElse(3));
	}

}
