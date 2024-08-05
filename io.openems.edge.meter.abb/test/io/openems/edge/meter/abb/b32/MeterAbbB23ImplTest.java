package io.openems.edge.meter.abb.b32;

import org.junit.Test;

import io.openems.edge.bridge.mbus.test.DummyMbusBridgeImpl;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;

public class MeterAbbB23ImplTest {

	private static final String COMPONENT_ID = "meter0";
	private static final String BRIDGE_ID = "mbus0";

	@Test()
	public void test() throws Exception {
		new ComponentTest(new MeterAbbB23Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) // #
				.addReference("setMbus", new DummyMbusBridgeImpl(BRIDGE_ID)) // oEMS
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMbusId("bridge0") //
						.setPrimaryAddress(10) //
						.setType(MeterType.PRODUCTION) //
						.build()) //
		;
	}

}
