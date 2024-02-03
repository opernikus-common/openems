package io.openems.edge.evcs.cluster.chargemanagement;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "EVCS Cluster Charge Management", //
		description = "Distributes power between multiple charging stations.")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evcsClusterCharge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Allow Charging?", description = "enables or disables charging for all EVCS.")
	boolean allowCharging() default true;

	// TODO needs to be implemented
	@AttributeDefinition(name = "Allow Prioritization?", description = "enables or disables priorization handling.")
	boolean allowPrioritization() default true;

	@AttributeDefinition(name = "Evcs-IDs", description = "IDs of (Managed) EVCS devices.")
	String[] evcs_ids() default { "evcs0", "evcs1" };

	@AttributeDefinition(name = "EvcsClusterLimiter-IDs", description = "IDs of the evcsClusterLimiters.")
	String[] evcsClusterLimiter_ids() default { "ctrlEvcsClusterLimiter0" };

	@AttributeDefinition(name = "Red Hold Time", description = "State RED hold time [s] - minimum duration time state RED is hold, even if conditions "
			+ "for state RED are not valid anymore.")
	int redHoldTime() default 120; // s

	@AttributeDefinition(name = "Round Robin Time", description = "round robin cycle time [s] for round robin activity.")
	int roundRobinTime() default 900; // s

	@AttributeDefinition(name = "Max Imbalance Time", description = "max unbalance time [s] before switching "
			+ "a) from leveling to round robin or b) to cycle round robin, in case phase imbalance can not be resolved.")
	int imbalanceHoldTime() default 30; // s

	@AttributeDefinition(name = "Limits exceeded Holdtime", description = "max time [s] cluster is allowed to exceed residual power before switching "
			+ "a) from green to yellow or b) from yellow to red.")
	int limitsExceededTime() default 30; // s

	@AttributeDefinition(name = "Current Step Size", description = "current step size [A] - size of a current decrement/increment step in A (per phase).")
	int currentStep() default 1; // A

	@AttributeDefinition(name = "Step interval", description = "delay (in s) until next increment/decrement step.")
	int stepInterval() default 1; // s

	@AttributeDefinition(name = "Debug Log", description = "log more debug output.")
	boolean verboseDebug() default false;

	// note: when changing default target filter,
	// EvcsClusterChargeMgmtImpl.addEvcs() annotation needs to be changed too.
	@AttributeDefinition(name = "Evcs target filter", description = "This is auto-generated by 'Evcs-IDs'.")
	String Evcs_target() default "(&(enabled=true)(!(service.factoryPid=Evcs.Cluster.ChargeManagement)))";

	@AttributeDefinition(name = "EvcsClusterLimiter target filter", description = "This is auto-generated by 'evcsClusterLimiter-IDs'.")
	String EvcsClusterLimiter_target() default "(enabled=true)";

	String webconsole_configurationFactory_nameHint() default "EVCS Cluster Charge Management [{id}]";

}
