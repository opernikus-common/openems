package io.openems.edge.evcs.cluster.chargemanagement;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.controller.evcs.cluster.chargemanagement.PhaseImbalance;
import io.openems.edge.meter.test.DummyElectricityMeter;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MeterTest {

	private static final String METER_ID = "meter0";

	private SupplyCableConstraints m;

	public MeterTest() {
		this.m = new SupplyCableConstraints(new EvcsClusterChargeMgmtImpl());
	}

	// @Test
	protected void test01PhaseImbalancesPureConsumption() throws Exception {

		this.prepareMeterTest(10, 10, 10);
		var pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.NO_IMBALANCE.getValue());

		this.prepareMeterTest(31, 14, 10);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L1_TOO_HIGH.getValue());

		this.prepareMeterTest(10, 31, 13);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L2_TOO_HIGH.getValue());

		this.prepareMeterTest(12, 10, 31);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L3_TOO_HIGH.getValue());

		this.prepareMeterTest(0, 21, 21);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L1_TOO_LOW.getValue());

		this.prepareMeterTest(10, 21, 31);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L1_TOO_LOW.getValue());

		this.prepareMeterTest(21, 0, 21);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L2_TOO_LOW.getValue());

		this.prepareMeterTest(21, 10, 31);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L2_TOO_LOW.getValue());

		this.prepareMeterTest(21, 21, 0);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L3_TOO_LOW.getValue());

		this.prepareMeterTest(12, 22, 0);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L3_TOO_LOW.getValue());

	}

	// @Test
	protected void test10PhaseImbalancesPureProduction() throws Exception {

		this.prepareMeterTest(-10, -10, -10);
		var pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.NO_IMBALANCE.getValue());

		this.prepareMeterTest(-31, -10, -10);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L1_TOO_LOW.getValue());

		this.prepareMeterTest(-10, -31, -10);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L2_TOO_LOW.getValue());

		this.prepareMeterTest(-10, -10, -31);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L3_TOO_LOW.getValue());

		this.prepareMeterTest(-14, -10, -31);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L3_TOO_LOW.getValue());

		this.prepareMeterTest(0, -30, -30);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L1_TOO_HIGH.getValue());

		this.prepareMeterTest(-30, 0, -30);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L2_TOO_HIGH.getValue());

		this.prepareMeterTest(-30, -30, 0);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L3_TOO_HIGH.getValue());

		this.prepareMeterTest(-30, -4, 0);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L1_TOO_LOW.getValue());

		this.prepareMeterTest(-3, -28, 0);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L2_TOO_LOW.getValue());

	}

	// @Test
	protected void test20PhaseImbalancesMixedConsumptionProduction() throws Exception {

		this.prepareMeterTest(0, 0, 0);
		var pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.NO_IMBALANCE.getValue());

		this.prepareMeterTest(31, -10, -10);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L1_TOO_HIGH.getValue());

		this.prepareMeterTest(-10, 31, -10);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L2_TOO_HIGH.getValue());

		this.prepareMeterTest(-10, -10, 31);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L3_TOO_HIGH.getValue());

		this.prepareMeterTest(-15, 6, 6);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L1_TOO_LOW.getValue());

		this.prepareMeterTest(-5, 16, 17);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L1_TOO_LOW.getValue());

		this.prepareMeterTest(15, -15, 15);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L2_TOO_LOW.getValue());

		this.prepareMeterTest(5, -15, 8);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L2_TOO_LOW.getValue());

		this.prepareMeterTest(15, 17, -5);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L3_TOO_LOW.getValue());

		this.prepareMeterTest(8, 5, -15);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L3_TOO_LOW.getValue());

		this.prepareMeterTest(9, 17, -5);
		pi = this.m.getPhaseImbalance();
		assertEquals(pi.getValue(), PhaseImbalance.L3_TOO_LOW.getValue());

	}

	private void prepareMeterTest(int currentL1, int currentL2, int currentL3) {
		var d = new DummyElectricityMeter(METER_ID);
		d._setCurrentL1(currentL1 * 1_000);
		d._setCurrentL2(currentL2 * 1_000);
		d._setCurrentL3(currentL3 * 1_000);
		d._setActivePowerL1(currentL1 * 1_000 * 230);
		d._setActivePowerL2(currentL2 * 1_000 * 230);
		d._setActivePowerL3(currentL3 * 1_000 * 230);

		for (Channel<?> channel : d.channels()) {
			channel.nextProcessImage();
		}
	}

	@SuppressWarnings("unused")
	private Config testConfig = new Config() {

		@Override
		public Class<? extends Annotation> annotationType() {
			return null;
		}

		@Override
		public String id() {
			return null;
		}

		@Override
		public String alias() {
			return null;
		}

		@Override
		public boolean enabled() {
			return false;
		}

		@Override
		public boolean allowCharging() {
			return false;
		}

		@Override
		public String[] evcs_ids() {
			return null;
		}

		@Override
		public int redHoldTime() {
			return 2;
		}

		@Override
		public int roundRobinTime() {
			return 10;
		}

		@Override
		public int imbalanceHoldTime() {
			return 10;
		}

		@Override
		public int currentStep() {
			return 1;
		}

		@Override
		public int stepInterval() {
			return 1;
		}

		@Override
		public String Evcs_target() {
			return null;
		}

		@Override
		public String webconsole_configurationFactory_nameHint() {
			return null;
		}

		@Override
		public boolean allowPrioritization() {
			return true;
		}

		@Override
		public String[] evcsClusterLimiter_ids() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int limitsExceededTime() {
			return 30;
		}

		@Override
		public String EvcsClusterLimiter_target() {
			return null;
		}

		@Override
		public boolean verboseDebug() {
			return false;
		}

	};
}
