package io.openems.edge.io.revpi.bsp.digitalio;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class IoRevolutionPiDigitalIoImplTest {

	private static final String COMPONENT_ID = "io0";
	private static final String[] IN = { "I_1", "I_2", "I_3", "I_4", //
			"I_5", "I_6", "I_7", "I_8", //
			"I_9", "I_10", "I_11", "I_12", //
			"I_13", "I_14" }; //

	private static final String[] OUT = { //
			"O_1", "O_2", "O_3", "O_4", //
			"O_5", "O_6", "O_7", "O_8", //
			"O_9", "O_10", "O_11", "O_12", //
			"O_13", "O_14" };

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoRevPiDigitalIoDeviceImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setRevpiType(ExpansionModule.REVPI_DIO) //
						.setIn(IN) //
						.setOut(OUT) //
						.setUpdateOutputFromHardware(true) //
						.setSimulationMode(true) //
						.setSimulationDataIn("") //
						.build()) //
		;
	}

}
