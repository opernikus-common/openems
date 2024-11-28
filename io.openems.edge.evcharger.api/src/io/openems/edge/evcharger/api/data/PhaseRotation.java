package io.openems.edge.evcharger.api.data;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.evcharger.api.EvCharger;
import io.openems.edge.meter.api.ElectricityMeter;

public enum PhaseRotation implements OptionsEnum {
	/**
	 * EVCS which use standard hardware connection configuration.
	 * 
	 * <p>
	 * L1 is connect to L1,...
	 */
	L1_L2_L3(1, "L1_L2_L3", //
			EvCharger.RawChannelId.RAW_CURRENT_L1, //
			EvCharger.RawChannelId.RAW_CURRENT_L2, //
			EvCharger.RawChannelId.RAW_CURRENT_L3, //
			ElectricityMeter.ChannelId.CURRENT_L1, //
			ElectricityMeter.ChannelId.CURRENT_L2, //
			ElectricityMeter.ChannelId.CURRENT_L3 //
	), //
	/**
	 * EVCS which use a rotated hardware connection configuration.
	 * 
	 * <p>
	 * L1 is connect to L2,...
	 */
	L2_L3_L1(2, "L2_L3_L1", //
			EvCharger.RawChannelId.RAW_CURRENT_L2, //
			EvCharger.RawChannelId.RAW_CURRENT_L3, //
			EvCharger.RawChannelId.RAW_CURRENT_L1, //
			ElectricityMeter.ChannelId.CURRENT_L1, //
			ElectricityMeter.ChannelId.CURRENT_L2, //
			ElectricityMeter.ChannelId.CURRENT_L3 //
	), //
	/**
	 * EVCS which use a rotated hardware connection configuration.
	 * 
	 * <p>
	 * L1 is connect to L3,...
	 */
	L3_L1_L2(3, "L3_L1_L2", //
			EvCharger.RawChannelId.RAW_CURRENT_L3, //
			EvCharger.RawChannelId.RAW_CURRENT_L1, //
			EvCharger.RawChannelId.RAW_CURRENT_L2, //
			ElectricityMeter.ChannelId.CURRENT_L1, //
			ElectricityMeter.ChannelId.CURRENT_L2, //
			ElectricityMeter.ChannelId.CURRENT_L3 //
	); //

	private final int value;
	private final String name;

	private final ChannelId inPhase1;
	private final ChannelId inPhase2;
	private final ChannelId inPhase3;
	private final ChannelId outPhase1;
	private final ChannelId outPhase2;
	private final ChannelId outPhase3;

	PhaseRotation(int value, String name, //
			ChannelId inPhase1, ChannelId inPhase2, ChannelId inPhase3, //
			ChannelId outPhase1, ChannelId outPhase2, ChannelId outPhase3 //
	) {
		this.value = value;
		this.name = name;
		this.inPhase1 = inPhase1;
		this.inPhase2 = inPhase2;
		this.inPhase3 = inPhase3;
		this.outPhase1 = outPhase1;
		this.outPhase2 = outPhase2;
		this.outPhase3 = outPhase3;
	}

	public ChannelId getInPhase1() {
		return this.inPhase1;
	}

	public ChannelId getInPhase2() {
		return this.inPhase2;
	}

	public ChannelId getInPhase3() {
		return this.inPhase3;
	}

	public ChannelId getOutPhase1() {
		return this.outPhase1;
	}

	public ChannelId getOutPhase2() {
		return this.outPhase2;
	}

	public ChannelId getOutPhase3() {
		return this.outPhase3;
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
	 * 
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
	 * 
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
