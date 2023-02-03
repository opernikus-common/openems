package io.openems.edge.heater.chp.dachs;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class MyDeviceTest {

	private static final String COMPONENT_ID = "chp0";
	private static final String URL = "127.0.0.1:8080";
	private static final String USERNAME = "glt";
	private static final String PASSWORD = "";

	@Test
	public void test() throws Exception {
		new ComponentTest(new DachsGltImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setUrl(URL) //
						.setUsername(USERNAME) //
						.setPassword(PASSWORD) //
						.setInterval(10) //
						.setReadOnly(false)
						.setVerbose(false)
						.build())
				.next(new TestCase());
	}

}
