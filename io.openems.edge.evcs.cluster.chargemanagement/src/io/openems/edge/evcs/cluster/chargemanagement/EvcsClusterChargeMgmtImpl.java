package io.openems.edge.evcs.cluster.chargemanagement;

import java.time.Clock;
import java.util.List;

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.timer.TimerManager;
import io.openems.edge.controller.evcs.cluster.chargemanagement.EvcsClusterLimiter;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.MetaEvcs;
import io.openems.edge.evcs.cluster.chargemanagement.statemachine.Context;
import io.openems.edge.evcs.cluster.chargemanagement.statemachine.StateMachine;
import io.openems.edge.evcs.cluster.chargemanagement.utils.Diagnostics;
import io.openems.edge.evcs.cluster.chargemanagement.utils.EvcsTools;
import io.openems.edge.evcs.cluster.chargemanagement.utils.ModifyConfigTool;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Cluster.ChargeManagement", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE//
})
public class EvcsClusterChargeMgmtImpl extends AbstractManagedEvcs
		implements EvcsClusterChargeMgmt, MetaEvcs, Evcs, ManagedEvcs, DigitalOutput, EventHandler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EvcsClusterChargeMgmtImpl.class);

	private final StateMachine stateMachine = new StateMachine();
	private final BooleanWriteChannel[] digitalOutChannels;
	private final SupplyCableConstraints constraints;
	private final Diagnostics diagnostics;
	private final Context context;
	private boolean runOnce = true;
	private boolean active = false;
	@Reference
	private EvcsPower power;

	@Reference
	private TimerManager timerManager;

	@Reference
	protected ConfigurationAdmin configAdmin;

	@Reference
	protected ComponentManager componentManager;

	public EvcsClusterChargeMgmtImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EvcsClusterChargeMgmt.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values() //
		);
		this.constraints = new SupplyCableConstraints(this);
		this.context = new Context(this, new Cluster(this), this.constraints);
		this.digitalOutChannels = new BooleanWriteChannel[] { this.getAllowChargingChannel() };
		this.diagnostics = new Diagnostics(this);
	}

	@Reference(policy = ReferencePolicy.STATIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			// note: when changing target filter, default config.evcs_target needs to be
			// changed also.
			target = "(&(enabled=true)(!(service.factoryPid=Evcs.Cluster.ChargeManagement)))" //
	)
	protected void addEvcs(ManagedEvcs evcs) {
		this.logWarn(this.log, "add EVCS " + evcs.id());
		if (evcs == this || !evcs.isEnabled()) {
			return;
		}
		evcs.applyChargePowerPerPhase(true);
		this.context.getCluster().add(evcs);
	}

	protected void removeEvcs(ManagedEvcs evcs) {
		evcs.applyChargePowerPerPhase(false);
		this.context.getCluster().remove(evcs);
	}

	@Reference(policy = ReferencePolicy.STATIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE //
	)
	protected void addEvcsClusterLimiter(EvcsClusterLimiter evcsClusterLimiter) {
		this.logWarn(this.log, "add evcsClusterLimiter " + evcsClusterLimiter.id());
		this.context.getCableConstraints().add(evcsClusterLimiter);
	}

	protected void removeEvcsClusterLimiter(EvcsClusterLimiter evcsClusterLimiter) {
		this.logWarn(this.log, "remove evcsClusterLimiter " + evcsClusterLimiter.id());
		this.context.getCableConstraints().remove(evcsClusterLimiter);
	}

	@Override
	@Activate
	protected void activate(ComponentContext compContext, Config config) {
		super.activate(compContext, config);
		if (!config.enabled()) {
			return;
		}
		if (this.applyConfig(config)) {
			return;
		}
		this.active = true;
		this.logWarn(this.log, "activated (" + this.context.getCluster() + "):");
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.active = false;

		this.context.getCluster().clean();
		this.selfEvcsResetChannels();
		this.logWarn(this.log, "deactivated");
	}

	/**
	 * Checks if component is activated or deactivated.
	 *
	 * @return true if component is activated.
	 */
	@Override
	public boolean isActivated() {
		return this.active;
	}

	private boolean applyConfig(Config config) {
		this.config = config;
		var sfpid = this.serviceFactoryPid();
		if (sfpid != null //
				&& (sfpid.length() == 0 || EvcsTools.contains(config.evcs_ids(), "evcsClusterCharge"))) {
			sfpid = null;
		}
		if (OpenemsComponent.updateReferenceFilterIgnoreFactoryId(this.configAdmin, this.servicePid(), sfpid, "Evcs",
				config.evcs_ids())) {
			return true;
		}
		if (OpenemsComponent.updateReferenceFilter(this.configAdmin, this.servicePid(), "EvcsClusterLimiter",
				config.evcsClusterLimiter_ids())) {
			return true;
		}
		this.context.setTimerManager(this.timerManager);
		this.context.updateConfig(config);
		this.reinit();
		return false;
	}

	private void reinit() {
		this.cleanUp();
		try {
			this._setAllowCharging(this.config.allowCharging());
			this.setAllowCharging(this.config.allowCharging());
		} catch (Exception e) {
			// STATE wrong config setzen
			e.printStackTrace();
		}
		this.selfEvcsInit(this.runOnce);
		this.runOnce = false;
	}

	/**
	 * Sets some channel values to 0. Called in the constructor.
	 */
	private void cleanUp() {
		this._setEvcsPowerLimit(0);
		this._setEvcsPowerLimitPrio(0);
		this.selfEvcsResetChannels();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.updateStatemachine();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.updateChannelValues();
			this.selfEvcsUpdateChannels();
			break;
		}
	}

	private void updateStatemachine() {
		if (!this.active) {
			return;
		}
		try {
			this._setClusterState(this.stateMachine.getCurrentState());
			this.stateMachine.run(this.context);

		} catch (OpenemsNamedException e) {
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	private void updateChannelValues() {

		// check allow charging
		var value = this.getAllowChargingValueAndReset();
		if (value.isPresent()) {
			var allowCharging = value.get().booleanValue();
			if (this.config.allowCharging() != allowCharging) {
				this.logInfo(this.log, "Configuration change. Switching Allow Charging " + allowCharging);
				ModifyConfigTool.updateAllowCharging(this.configAdmin, this.componentManager, this.config.id(),
						allowCharging);
			}
			this._setAllowCharging(allowCharging);
		}

		this.context.getCluster().updateChannelValues();
		this.constraints.updateChannelValues();
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutChannels;
	}

	@Override
	protected List<ClusterEvcs> getClusteredEvcss() {
		return this.context.getCluster().getAllEvcss();
	}

	@Override
	protected SupplyCableConstraints getCableConstraints() {
		return this.constraints;
	}

	protected Diagnostics getDiagnostics() {
		return this.diagnostics;
	}

	public Context getContext() {
		return this.context;
	}

	public TimerManager getTimerManager() {
		return this.timerManager;
	}

	public Clock getClock() {
		return this.componentManager.getClock();
	}

	/**
	 * Log in context of cluster.
	 *
	 * @param text to log
	 */
	public void logInfo(String text) {
		if (this.config.verboseDebug()) {
			this.logInfo(this.log, text);
		}
	}

	/**
	 * Log in context of cluster.
	 *
	 * @param text to log
	 */
	public void logWarn(String text) {
		this.logWarn(this.log, text);
	}

	/**
	 * Log in context of cluster.
	 *
	 * @param text to log
	 */
	public void logError(String text) {
		this.logError(this.log, text);
	}

	@Override
	public void applyChargePowerPerPhase(boolean value) {
	}
	
	@Override
	public String debugLog() {
		return this.stateMachine.getCurrentState().asCamelCase() + ", " //
				+ this.getNumberOfEvcs() + ", " //
				+ this.getNumberOfEvcsPrio() + " prio evcs Limit: " //
				+ this.getEvcsPowerLimit() + " " //
				+ this.getEvcsPowerLimitPrio() + ", cluster: " //
				+ this.getChargePower();
	}


}
