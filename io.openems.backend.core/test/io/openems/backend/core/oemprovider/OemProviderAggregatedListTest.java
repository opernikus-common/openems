package io.openems.backend.core.oemprovider;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;

import org.junit.Test;

import io.openems.backend.common.oemprovider.AggregatedList;
import io.openems.backend.common.oemprovider.ChannelType;
import io.openems.backend.common.oemprovider.DataType;

public class OemProviderAggregatedListTest {

	private static final String DUMMY_COMPONENT = "timedata0";
	private static final String AGGREGATED_FILE = "data/aggregatedInflux.config";
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

	public OemProviderAggregatedListTest() {
		this.o = new OemProviderImpl();
		this.o.activate(this.c);
	}

	@Test
	public void test() {
		AggregatedList al = this.o.getAggregatedList(DUMMY_COMPONENT, AGGREGATED_FILE);
		assertEquals(al.timedataGetChannelType("meter113/ActivePowerL1"), ChannelType.UNDEFINED);
		assertEquals(al.timedataGetChannelType("meterA/ActivePowerL1"), ChannelType.UNDEFINED);
		assertEquals(al.timedataGetChannelType("_sum/EssSoc"), ChannelType.AVG);
		assertEquals(al.timedataGetChannelType("_sum/GridBuyActiveEnergy"), ChannelType.MAX);
		assertEquals(al.timedataGetChannelType("ctrlGridOptimizedCharge0/DelayChargeMaximumChargeLimit"),
				ChannelType.AVG);
		assertEquals(al.timedataGetChannelType("io9/Relay1"), ChannelType.AVG);
		assertEquals(al.timedataGetChannelType("io91/Relay1"), ChannelType.UNDEFINED);
		assertEquals(al.timedataGetChannelType("io1/Relay10"), ChannelType.AVG);
		assertEquals(al.timedataGetChannelType("meter9/ActivePowerL1"), ChannelType.AVG);
		assertEquals(al.timedataGetChannelType("meter99/ActivePowerL1"), ChannelType.AVG);
		assertEquals(al.timedataGetChannelType("meter99/ActivePowerL4"), ChannelType.UNDEFINED);
		assertEquals(al.timedataGetChannelType("meter991/ActivePowerL1"), ChannelType.UNDEFINED);
		assertEquals(al.timedataFetchAggregatedChannel("io1/Relay10"), DataType.LONG);
		assertEquals(al.timedataFetchAggregatedChannel("_sum/EssSoc"), DataType.LONG);
		assertEquals(al.timedataFetchAggregatedChannel("meterA/ActivePowerL1"), DataType.UNDEFINED);
		assertEquals(al.timedataGetChannelType("evcs0/PowerPrecision"), ChannelType.AVG);
		assertEquals(al.timedataFetchAggregatedChannel("evcs0/PowerPrecision"), DataType.DOUBLE);
		assertEquals(al.timedataGetChannelType("evcs10/PowerPrecision"), ChannelType.UNDEFINED);
		assertEquals(al.timedataFetchAggregatedChannel("evcs10/PowerPrecision"), DataType.UNDEFINED);

	}

}
