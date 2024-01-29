package io.openems.edge.evcs.stoehr.designtower;

import io.openems.edge.evcs.api.Priority;
import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evcs.api.PhaseRotation;

public class DesigntowerTest {

    private static final String EVCS_ID = "evcs0";
    private static final String MODBUS_ID = "modbus0";

    @Test
    public void test() throws Exception {
	new ComponentTest(new EvcsDesigntowerImpl()) //
		.addReference("cm", new DummyConfigurationAdmin()) //
		.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
		.activate(MyConfig.create() //
			.setId(EVCS_ID) //
			.setModbusId(MODBUS_ID) //
			.setModbusUnitId(1) //
			.setMinCurrent(6) //
			.setMaxHwCurrent(32) //
			.setPhaseRotation(PhaseRotation.L1_L2_L3) //
			.setPriority(Priority.LOW) //
			.setDebugMode(false) //
			.build()) //
		.next(new TestCase());
    }

}
