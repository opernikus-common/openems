package io.openems.edge.evcs.api;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.evcs.api.Evcs.ChannelId;

public enum PhaseRotation implements OptionsEnum {
    /**
     * EVCS which use standard hardware connection configuration.
     * 
     * <p>
     * L1 is connect to L1,...
     */
    L1_L2_L3(1, "L1_L2_L3", //
	    Evcs.ChannelId.CURRENT_L1, //
	    Evcs.ChannelId.CURRENT_L2, //
	    Evcs.ChannelId.CURRENT_L3), //
    /**
     * EVCS which use a rotated hardware connection configuration.
     * 
     * <p>
     * L1 is connect to L2,...
     */
    L2_L3_L1(2, "L2_L3_L1", //
	    Evcs.ChannelId.CURRENT_L2, //
	    Evcs.ChannelId.CURRENT_L3, //
	    Evcs.ChannelId.CURRENT_L1), //
    /**
     * EVCS which use a rotated hardware connection configuration.
     * 
     * <p>
     * L1 is connect to L3,...
     */
    L3_L1_L2(3, "L3_L1_L2", //
	    Evcs.ChannelId.CURRENT_L3, //
	    Evcs.ChannelId.CURRENT_L1, //
	    Evcs.ChannelId.CURRENT_L2);

    private final int value;
    private final String name;

    private final ChannelId firstPhase;
    private final ChannelId secondPhase;
    private final ChannelId thirdPhase;

    PhaseRotation(int value, String name, ChannelId firstPhase, ChannelId secondPhase, ChannelId thirdPhase) {
	this.value = value;
	this.name = name;
	this.firstPhase = firstPhase;
	this.secondPhase = secondPhase;
	this.thirdPhase = thirdPhase;
    }

    public ChannelId getFirstPhase() {
	return this.firstPhase;
    }

    public ChannelId getSecondPhase() {
	return this.secondPhase;
    }

    public ChannelId getThirdPhase() {
	return this.thirdPhase;
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
	return L1_L2_L3;
    }

    /**
     * Returns the phaseRotation from name.
     * @param name the name
     * @return the phase rotation
     */
    public PhaseRotation fromName(String name) {
	for (PhaseRotation rotation : PhaseRotation.values()) {
	    if (rotation.name.equalsIgnoreCase(name)) {
		return rotation;
	    }
	}
	return PhaseRotation.L1_L2_L3;
    }

    /**
     * Returns the phaseRotation from value.
     * @param value the int value
     * @return the phase rotation
     */
    public PhaseRotation fromValue(Integer value) {
	for (PhaseRotation rotation : PhaseRotation.values()) {
	    if (rotation.value == value) {
		return rotation;
	    }
	}
	return PhaseRotation.L1_L2_L3;

    }

}
