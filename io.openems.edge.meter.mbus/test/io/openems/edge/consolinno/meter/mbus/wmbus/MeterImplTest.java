package io.openems.edge.consolinno.meter.mbus.wmbus;

import org.junit.Test;

import io.openems.edge.bridge.wmbus.test.DummyWmbusBridgeImpl;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MeterImplTest {

	private static final String COMPONENT_ID = "meter0";
	private static final String BRIDGE_ID = "wmbus0";

	@Test()
	public void test() throws Exception {
		new ComponentTest(new WaterMeterWirelessMbusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) // #
				.addReference("setWmbus", new DummyWmbusBridgeImpl(BRIDGE_ID)) //
				.activate(MyConfigWaterMeterWMbus.create() //
						.setId(COMPONENT_ID) //
						.setWmbusId(BRIDGE_ID) //
						.build()) //
		;
	}

}
