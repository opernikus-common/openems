package io.openems.edge.consolinno.leaflet.bsp.core.enums;

public enum RedLed {
    OFF((short) 0), //
    BLINK((short) 16), // Red Blink
    ON((short) 32); // Red Constant Light

    public final short value;

    private RedLed(short value) {
	this.value = value;
    }
}
