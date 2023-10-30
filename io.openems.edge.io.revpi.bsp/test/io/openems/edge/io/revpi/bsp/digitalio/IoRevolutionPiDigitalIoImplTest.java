package io.openems.edge.io.revpi.bsp.digitalio;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class IoRevolutionPiDigitalIoImplTest {

	// private static final String COMPONENT_ID = "io0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoRevPiDigitalIoDeviceImpl()) //
		// .activate(MyConfig.create() //
		// .setId(COMPONENT_ID) //
		// .setInitOutputFromHardware(false) //
		// .build()) //
		;
	}

}
