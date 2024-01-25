package io.openems.edge.consolinno.leaflet.bsp.core.utils;

/**
 * the minimum required LeafletBaseSoftware Version.
 */
public enum MinimumBaseSoftwareVersion {
    VERSION(78);

    private final int value;

    private MinimumBaseSoftwareVersion(int value) {
	this.value = value;

    }

    /**
     * Get Minimum Version number.
     *
     * @return Value
     */
    public int getValue() {
	return this.value;
    }
}
