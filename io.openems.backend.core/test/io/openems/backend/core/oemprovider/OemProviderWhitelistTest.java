package io.openems.backend.core.oemprovider;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;

import org.junit.Test;

import io.openems.backend.common.oemprovider.WhiteList;

public class OemProviderWhitelistTest {

	private static final String DUMMY_COMPONENT = "timedata0";
	private static final String FILE = "data/channelWhitelist.config";
	private OemProviderImpl o;
	private final Config c = new Config() {

		@Override
		public Class<? extends Annotation> annotationType() {
			return null;
		}

		@Override
		public String webconsole_configurationFactory_nameHint() {
			return null;
		}

		@Override
		public boolean verbose() {
			return false;
		}

	};

	public OemProviderWhitelistTest() {
		try {
			String name = new File(FILE).getCanonicalPath();
			System.out.println("Loading file " + name);
			this.o = new OemProviderImpl();
			this.o.activate(this.c);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void test() {
		WhiteList wl = this.o.getWhitelist(DUMMY_COMPONENT, FILE);

		assertFalse(wl.isChannelBlacklisted("meter0/ActivePower"));
		assertFalse(wl.isChannelBlacklisted("meter0/ActivePowerL1"));
		assertFalse(wl.isChannelBlacklisted("meter0/ActivePowerL2"));
		assertFalse(wl.isChannelBlacklisted("meter0/ActivePowerL3"));
		// default whitelist has wildcard
		// if you want to enable correctBlacklist pattern, remove wildcard
		// add your channel afterwards
		//assertTrue(wl.isChannelBlacklisted("meter0/ActivePowerL4"));
		//assertTrue(wl.isChannelBlacklisted("meter0/ActivePowerBla"));

		assertFalse(wl.isChannelBlacklisted("meter0/ActivePowerL1"));
		assertFalse(wl.isChannelBlacklisted("meter3/ActivePowerL1"));
		assertFalse(wl.isChannelBlacklisted("meter03/ActivePowerL1"));
		assertFalse(wl.isChannelBlacklisted("meter82/ActivePowerL1"));
		//assertTrue(wl.isChannelBlacklisted("meter113/ActivePowerL1"));
		//assertTrue(wl.isChannelBlacklisted("meterA/ActivePowerL1"));

		assertFalse(wl.isChannelBlacklisted("meter0/ActivePowerL1"));
		assertFalse(wl.isChannelBlacklisted("meter0/ActivePowerL2"));
		assertFalse(wl.isChannelBlacklisted("meter0/ActivePowerL3"));
		assertFalse(wl.isChannelBlacklisted("meter0/ActivePower"));
		//assertTrue(wl.isChannelBlacklisted("meter0/ActivePowerA3"));
		//assertTrue(wl.isChannelBlacklisted("meter0/ActivePowerL0"));

		assertFalse(wl.isChannelBlacklisted("meter03/ActivePower"));
		assertFalse(wl.isChannelBlacklisted("meter13/ActivePower"));
		//assertTrue(wl.isChannelBlacklisted("meter131/ActivePower"));

		//assertTrue(wl.isChannelBlacklisted("einwort0/State"));
		//assertTrue(wl.isChannelBlacklisted("zwei worte0/State"));

		assertFalse(wl.isChannelBlacklisted("thermometer0/Temperature"));
		//assertTrue(wl.isChannelBlacklisted("thermometer10/Temperature"));
	}

}
