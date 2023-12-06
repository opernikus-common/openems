package io.openems.edge.meter.gmc.em2389;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;

public class MeterGmcEm2389ImplTest {

	private static final String METER_ID = "meter0";
	private static final String MODBUS_ID = "modbus0";
	private static final int UNIT_ID = 1;

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterGmcEm2389Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(UNIT_ID) //
						.setType(MeterType.GRID) //
						.setalternativePowerCalculation(false) //
						.build()) //
		;
	}
}
