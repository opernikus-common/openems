package io.openems.edge.evcs.cluster.virtualmeter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Meter Virtual Cluster Chargemanagement", //
		description = "This is a virtual meter which is used to sum up all Evcss within one Evcs Cluster Chargemanagement")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Evcs Cluster Chargemgmt ID", description = "Id of the EvcsCluster Chargemangement to make a meter from.")
	String evcsClusterChargeMgmtId() default "evcsClusterCharge0";

	String webconsole_configurationFactory_nameHint() default "Meter Virtual Cluster Chargemanagement [{id}]";
}
