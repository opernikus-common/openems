package io.openems.edge.consolinno.leaflet.bsp.core.enums;

public enum OrangeLed {
    OFF((short) 0), //
    BLINK((short) 1), // Orange Blink
    ON((short) 2); // Orange constant light

    public final short value;

    private OrangeLed(short value) {
	this.value = value;
    }

}
