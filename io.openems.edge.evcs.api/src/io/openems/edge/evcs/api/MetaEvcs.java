package io.openems.edge.evcs.api;

/**
 * A MetaEvcs is a wrapper for physical EVCS. It is not a
 * physical EVCS itself. This is used to distinguish e.g. an EvcsCluster from an
 * actual EVCS.
 */
public interface MetaEvcs extends Evcs {

}
