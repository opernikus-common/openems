package io.openems.edge.evcs.compleo;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.timer.DummyTimerManager;
import io.openems.edge.common.timer.TimerManager;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Priority;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class EvcsCompleoImplTest {

	private static final String EVCS_ID = "evcs0";
	private static final String MODBUS_ID = "modbus0";
	private static final String METER_ID = "meter1";
	private DummyComponentManager cpm;

	// Evcs Channel Addresses
	private static ChannelAddress evcsChargePower = new ChannelAddress(EVCS_ID, "ChargePower");
	private static ChannelAddress evcsSetChargePower = new ChannelAddress(EVCS_ID, "SetChargePowerLimit");
	private static ChannelAddress evcsCurrent = new ChannelAddress(EVCS_ID, "Current");
	private static ChannelAddress evcsCurrentL1 = new ChannelAddress(EVCS_ID, "CurrentL1");
	private static ChannelAddress evcsCurrentL2 = new ChannelAddress(EVCS_ID, "CurrentL2");
	private static ChannelAddress evcsCurrentL3 = new ChannelAddress(EVCS_ID, "CurrentL3");
	private static ChannelAddress evcsEnergy = new ChannelAddress(EVCS_ID, "ActiveConsumptionEnergy");
	private static ChannelAddress evcsStatus = new ChannelAddress(EVCS_ID, "Status");

	// Meter Channel Addresses
	private static ChannelAddress meterActivePower = new ChannelAddress(METER_ID, "ActivePower");
	private static ChannelAddress meterCurrent = new ChannelAddress(METER_ID, "Current");
	private static ChannelAddress meterCurrentL1 = new ChannelAddress(METER_ID, "CurrentL1");
	private static ChannelAddress meterCurrentL2 = new ChannelAddress(METER_ID, "CurrentL2");
	private static ChannelAddress meterCurrentL3 = new ChannelAddress(METER_ID, "CurrentL3");
	private static ChannelAddress meterEnergy = new ChannelAddress(METER_ID, "ActiveProductionEnergy");

	@Test
	public void testWithIntegratedMeter() throws Exception {
		this.cpm = new DummyComponentManager(new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC));
		TimerManager tm = new DummyTimerManager(this.cpm);
		new ComponentTest(new EvcsCompleoImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("timerManager", tm) //
				.activate(MyConfig.create() //
						.setId(EVCS_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(255) //
						.setModel(Model.COMPLEO_ECO_20) //
						.setMinHwCurrent(6000) //
						.setMaxHwCurrent(16000) //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.setPriority(Priority.LOW) //
						.setStartStopDelay(120) //
						.setMeteringType(MeteringType.WITH_INTEGRATED_METER) //
						.setMeterId(METER_ID) //
						.setRestartPilotSignal(false) //
						.setDebugMode(false) //
						.build()) //
				.next(new TestCase());
	}

	@Test
	public void testWithExternalMeter() throws Exception {
		this.cpm = new DummyComponentManager(new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC));
		var tm = new DummyTimerManager(this.cpm);
		new ComponentTest(new EvcsCompleoImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("meter", new DummyElectricityMeter(METER_ID)) //
				.addReference("timerManager", tm) //
				.activate(MyConfig.create() //
						.setId(EVCS_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(255) //
						.setModel(Model.COMPLEO_ECO_20) //
						.setMinHwCurrent(6000) //
						.setMaxHwCurrent(16000) //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.setPriority(Priority.LOW) //
						.setStartStopDelay(120) //
						.setMeteringType(MeteringType.WITH_EXTERNAL_METER) //
						.setMeterId(METER_ID) //
						.setRestartPilotSignal(false) //
						.setDebugMode(false) //
						.build()) //
				.next(new TestCase() //
						.input(meterActivePower, 11040) //
						.input(meterCurrent, 48000) //
						.input(meterCurrentL1, 16000) //
						.input(meterCurrentL2, 16000) //
						.input(meterCurrentL3, 16000) //
						.input(meterEnergy, 12345L) //
						.output(evcsChargePower, 11040) //
						.output(evcsCurrent, 48000) //
						.output(evcsCurrentL1, 16000) //
						.output(evcsCurrentL2, 16000) //
						.output(evcsCurrentL3, 16000) //
						.output(evcsEnergy, 12345L) //
				);
	}

	@Test
	public void testWithoutMeter() throws Exception {
		this.cpm = new DummyComponentManager(new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC));
		TimerManager tm = new DummyTimerManager(this.cpm);
		new ComponentTest(new EvcsCompleoImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("timerManager", tm) //
				.activate(MyConfig.create() //
						.setId(EVCS_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(255) //
						.setModel(Model.COMPLEO_ECO_20) //
						.setMinHwCurrent(6000) //
						.setMaxHwCurrent(16000) //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.setPriority(Priority.LOW) //
						.setStartStopDelay(120) //
						.setMeteringType(MeteringType.WITHOUT_METER) //
						.setMeterId(METER_ID) //
						.setRestartPilotSignal(false) //
						.setDebugMode(false) //
						.build()) //
				.next(new TestCase("Not ready for charging") //
						.input(evcsSetChargePower, 11040) //
						.input(evcsStatus, Status.NOT_READY_FOR_CHARGING) //
						.output(evcsChargePower, 0))
				.next(new TestCase("Charging Three Phase") //
						.input(evcsSetChargePower, 11040) //
						.input(evcsStatus, Status.CHARGING) //
						.output(evcsChargePower, 11040) //
						.output(evcsCurrent, 48000) //
						.output(evcsCurrentL1, 16000) //
						.output(evcsCurrentL2, 16000) //
						.output(evcsCurrentL3, 16000) //
				);
	}

}
