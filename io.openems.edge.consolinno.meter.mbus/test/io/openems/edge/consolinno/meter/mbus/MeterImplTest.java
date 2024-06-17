package io.openems.edge.consolinno.meter.mbus;

import org.junit.Test;

import io.openems.edge.bridge.mbus.test.DummyMbusBridgeImpl;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MeterImplTest {

	private static final String COMPONENT_ID = "meter0";
	private static final String BRIDGE_ID = "mbus0";

	@Test()
	public void test() throws Exception {
		new ComponentTest(new HeatMeterMbusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) // #
				.addReference("setMbus", new DummyMbusBridgeImpl(BRIDGE_ID)) //
				.activate(MyConfigHeatMeter.create() //
						.setId(COMPONENT_ID) //
						.setMbusId(BRIDGE_ID).setPrimaryAddress(10) //
						.build()) //
		;
		new ComponentTest(new GasMeterMbusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) // #
				.addReference("setMbus", new DummyMbusBridgeImpl(BRIDGE_ID)) //
				.activate(MyConfigGasMeter.create() //
						.setId(COMPONENT_ID) //
						.setMbusId(BRIDGE_ID) //
						.setPrimaryAddress(10) //
						.build()) //
		;
		new ComponentTest(new WaterMeterMbusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) // #
				.addReference("setMbus", new DummyMbusBridgeImpl(BRIDGE_ID)) //
				.activate(MyConfigWaterMeter.create() //
						.setId(COMPONENT_ID) //
						.setMbusId(BRIDGE_ID).setPrimaryAddress(10) //
						.build()) //
		;
	}

}
