package io.openems.edge.io.acthor;

import io.openems.edge.meter.api.MeterType;
import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class AcThorImplTest {

	private static final String IO_ID = "heater0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new AcThorImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setModbusId(MODBUS_ID) //
						.setId(IO_ID) //
						.setModbusUnitId(1) //
						.setType(MeterType.CONSUMPTION_METERED) //
						.setPowerStep(3000) //
						.build()); //
	}

}
