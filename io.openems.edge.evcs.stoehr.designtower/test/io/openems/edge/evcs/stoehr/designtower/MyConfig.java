package io.openems.edge.evcs.stoehr.designtower;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.evcs.api.PhaseRotation;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

    protected static class Builder {
	private String id;
	private String modbusId;
	private int modbusUnitId;
	private int minHwCurrent;
	private int maxHwCurrent;
	private PhaseRotation phaseRotation;
	private boolean debugMode;

	private Builder() {
	}

	public Builder setId(String id) {
	    this.id = id;
	    return this;
	}

	public Builder setModbusId(String modbusId) {
	    this.modbusId = modbusId;
	    return this;
	}

	public Builder setModbusUnitId(int modbusUnitId) {
	    this.modbusUnitId = modbusUnitId;
	    return this;
	}

	public Builder setMinCurrent(int current) {
	    this.minHwCurrent = current;
	    return this;
	}

	public Builder setMaxHwCurrent(int current) {
	    this.maxHwCurrent = current;
	    return this;
	}

	public Builder setPhaseRotation(PhaseRotation rot) {
	    this.phaseRotation = rot;
	    return this;
	}

	public Builder setDebugMode(boolean debugMode) {
	    this.debugMode = debugMode;
	    return this;
	}

	public MyConfig build() {
	    return new MyConfig(this);
	}
    }

    /**
     * Create a Config builder.
     * 
     * @return a {@link Builder}
     */
    public static Builder create() {
	return new Builder();
    }

    private final Builder builder;

    private MyConfig(Builder builder) {
	super(Config.class, builder.id);
	this.builder = builder;
    }

    @Override
    public String modbus_id() {
	return this.builder.modbusId;
    }

    @Override
    public int modbusUnitId() {
	return this.builder.modbusUnitId;
    }

    @Override
    public String Modbus_target() {
	return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
    }

    @Override
    public int minHwCurrent() {
	return this.builder.minHwCurrent;
    }

    @Override
    public int maxHwCurrent() {
	return this.builder.maxHwCurrent;
    }

    @Override
    public PhaseRotation phaseRotation() {
	return this.builder.phaseRotation;
    }

    @Override
    public boolean debugMode() {
	return this.builder.debugMode;
    }

}
