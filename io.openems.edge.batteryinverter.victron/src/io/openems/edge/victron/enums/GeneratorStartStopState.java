package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum GeneratorStartStopState implements OptionsEnum {

    UNDEFINED(-1, "undefined"), //
    STOPPED(0, "Stopped"), //
    RUNNING(1, "Running"), //
    ERROR(10, "Error") //
    ;

    private final int value;
    private final String name;

    private GeneratorStartStopState(int value, String name) {
	this.value = value;
	this.name = name;
    }

    @Override
    public int getValue() {
	return this.value;
    }

    @Override
    public String getName() {
	return this.name;
    }

    @Override
    public OptionsEnum getUndefined() {
	return UNDEFINED;
    }

}
