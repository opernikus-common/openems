package io.openems.edge.evcs.alfen;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Priority;

public class EvcsAlfenImplTest {

	private static final String EVCS_ID = "evcs0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsAlfenImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(EVCS_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setMinHwPower(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER) //
						.setMaxHwPower(Evcs.DEFAULT_MAXIMUM_HARDWARE_POWER) //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.setPriority(Priority.LOW) //
						.setDebugMode(false) //
						.build()) //
				.next(new TestCase());
	}

}
