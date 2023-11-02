package io.openems.edge.core.appmanager;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import io.openems.edge.app.TestADependencyToC;
import io.openems.edge.app.TestBDependencyToC;
import io.openems.edge.app.TestC;
import io.openems.edge.app.TestMultipleIds;
import io.openems.edge.app.api.ModbusTcpApiReadOnly;
import io.openems.edge.app.api.ModbusTcpApiReadWrite;
import io.openems.edge.app.api.RestJsonApiReadOnly;
import io.openems.edge.app.ess.FixActivePower;
import io.openems.edge.app.ess.PrepareBatteryExtension;
import io.openems.edge.app.evcs.EvcsCluster;
import io.openems.edge.app.evcs.HardyBarthEvcs;
import io.openems.edge.app.evcs.IesKeywattEvcs;
import io.openems.edge.app.evcs.KebaEvcs;
import io.openems.edge.app.evcs.WebastoNextEvcs;
import io.openems.edge.app.evcs.WebastoUniteEvcs;
import io.openems.edge.app.heat.HeatPump;
import io.openems.edge.app.integratedsystem.FeneconHome;
import io.openems.edge.app.integratedsystem.FeneconHome20;
import io.openems.edge.app.integratedsystem.FeneconHome30;
import io.openems.edge.app.meter.MicrocareSdm630Meter;
import io.openems.edge.app.meter.SocomecMeter;
import io.openems.edge.app.pvselfconsumption.GridOptimizedCharge;
import io.openems.edge.app.pvselfconsumption.SelfConsumptionOptimization;
import io.openems.edge.app.timeofusetariff.AwattarHourly;
import io.openems.edge.app.timeofusetariff.StromdaoCorrently;
import io.openems.edge.app.timeofusetariff.Tibber;
import io.openems.edge.common.component.ComponentManager;

public class Apps {

	private Apps() {
		super();
	}

	/**
	 * Helper method for easier creation of a list of the used {@link OpenemsApp
	 * OpenemsApps}.
	 * 
	 * @param t            the {@link AppManagerTestBundle}
	 * @param appFunctions the methods to create the {@link OpenemsApp OpenemsApps}
	 * @return the list of the {@link OpenemsApp OpenemsApps}
	 */
	@SafeVarargs
	public static final List<OpenemsApp> of(AppManagerTestBundle t,
			Function<AppManagerTestBundle, OpenemsApp>... appFunctions) {
		return Arrays.stream(appFunctions) //
				.map(f -> f.apply(t)) //
				.collect(Collectors.toUnmodifiableList());
	}

	// Integrated Systems

	/**
	 * Test method for creating a {@link FeneconHome}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconHome feneconHome(AppManagerTestBundle t) {
		return app(t, FeneconHome::new, "App.FENECON.Home");
	}

	/**
	 * Test method for creating a {@link FeneconHome20}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconHome20 feneconHome20(AppManagerTestBundle t) {
		return app(t, FeneconHome20::new, "App.FENECON.Home.20");
	}

	/**
	 * Test method for creating a {@link FeneconHome30}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconHome30 feneconHome30(AppManagerTestBundle t) {
		return app(t, FeneconHome30::new, "App.FENECON.Home.30");
	}

	// TimeOfUseTariff

	/**
	 * Test method for creating a {@link AwattarHourly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final AwattarHourly awattarHourly(AppManagerTestBundle t) {
		return app(t, AwattarHourly::new, "App.TimeOfUseTariff.Awattar");
	}

	/**
	 * Test method for creating a {@link StromdaoCorrently}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final StromdaoCorrently stromdaoCorrently(AppManagerTestBundle t) {
		return app(t, StromdaoCorrently::new, "App.TimeOfUseTariff.Stromdao");
	}

	/**
	 * Test method for creating a {@link Tibber}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final Tibber tibber(AppManagerTestBundle t) {
		return app(t, Tibber::new, "App.TimeOfUseTariff.Tibber");
	}

	// Test

	/**
	 * Test method for creating a {@link TestADependencyToC}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TestADependencyToC testADependencyToC(AppManagerTestBundle t) {
		return app(t, TestADependencyToC::new, "App.Test.TestADependencyToC");
	}

	/**
	 * Test method for creating a {@link TestBDependencyToC}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TestBDependencyToC testBDependencyToC(AppManagerTestBundle t) {
		return app(t, TestBDependencyToC::new, "App.Test.TestBDependencyToC");
	}

	/**
	 * Test method for creating a {@link TestC}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TestC testC(AppManagerTestBundle t) {
		return app(t, TestC::new, "App.Test.TestC");
	}

	/**
	 * Test method for creating a {@link TestMultipleIds}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TestMultipleIds testMultipleIds(AppManagerTestBundle t) {
		return app(t, TestMultipleIds::new, "App.Test.TestMultipleIds");
	}

	// Api

	/**
	 * Test method for creating a {@link ModbusTcpApiReadOnly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final ModbusTcpApiReadOnly modbusTcpApiReadOnly(AppManagerTestBundle t) {
		return app(t, ModbusTcpApiReadOnly::new, "App.Api.ModbusTcp.ReadOnly");
	}

	/**
	 * Test method for creating a {@link ModbusTcpApiReadWrite}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final ModbusTcpApiReadWrite modbusTcpApiReadWrite(AppManagerTestBundle t) {
		return app(t, ModbusTcpApiReadWrite::new, "App.Api.ModbusTcp.ReadWrite");
	}

	/**
	 * Test method for creating a {@link RestJsonApiReadOnly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final RestJsonApiReadOnly restJsonApiReadOnly(AppManagerTestBundle t) {
		return app(t, RestJsonApiReadOnly::new, "App.Api.RestJson.ReadOnly");
	}

	// Evcs

	/**
	 * Test method for creating a {@link RestJsonApiReadOnly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final HardyBarthEvcs hardyBarthEvcs(AppManagerTestBundle t) {
		return app(t, HardyBarthEvcs::new, "App.Evcs.HardyBarth");
	}

	/**
	 * Test method for creating a {@link KebaEvcs}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final KebaEvcs kebaEvcs(AppManagerTestBundle t) {
		return app(t, KebaEvcs::new, "App.Evcs.Keba");
	}

	/**
	 * Test method for creating a {@link IesKeywattEvcs}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final IesKeywattEvcs iesKeywattEvcs(AppManagerTestBundle t) {
		return app(t, IesKeywattEvcs::new, "App.Evcs.IesKeywatt");
	}

	/**
	 * Test method for creating a {@link WebastoNextEvcs}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final WebastoNextEvcs webastoNext(AppManagerTestBundle t) {
		return app(t, WebastoNextEvcs::new, "App.Evcs.Webasto.Next");
	}

	/**
	 * Test method for creating a {@link WebastoUniteEvcs}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final WebastoUniteEvcs webastoUnite(AppManagerTestBundle t) {
		return app(t, WebastoUniteEvcs::new, "App.Evcs.Webasto.Unite");
	}

	/**
	 * Test method for creating a {@link EvcsCluster}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final EvcsCluster evcsCluster(AppManagerTestBundle t) {
		return app(t, EvcsCluster::new, "App.Evcs.Cluster");
	}

	// Heat

	/**
	 * Test method for creating a {@link HeatPump}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final HeatPump heatPump(AppManagerTestBundle t) {
		return app(t, HeatPump::new, "App.Heat.HeatPump");
	}

	// PvSelfConsumption

	/**
	 * Test method for creating a {@link GridOptimizedCharge}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final GridOptimizedCharge gridOptimizedCharge(AppManagerTestBundle t) {
		return app(t, GridOptimizedCharge::new, "App.PvSelfConsumption.GridOptimizedCharge");
	}

	/**
	 * Test method for creating a {@link SelfConsumptionOptimization}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final SelfConsumptionOptimization selfConsumptionOptimization(AppManagerTestBundle t) {
		return app(t, SelfConsumptionOptimization::new, "App.PvSelfConsumption.SelfConsumptionOptimization");
	}

	// Meter

	/**
	 * Test method for creating a {@link SocomecMeter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final SocomecMeter socomecMeter(AppManagerTestBundle t) {
		return app(t, (componentManager, componentContext, cm, componentUtil) -> new SocomecMeter(componentManager,
				componentContext, cm, componentUtil, t.appManagerUtil), "App.Meter.Socomec");
	}

	/**
	 * Test method for creating a {@link MicrocareSdm630Meter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final MicrocareSdm630Meter microcareSdm630Meter(AppManagerTestBundle t) {
		return app(t,
				(componentManager, componentContext, cm, componentUtil) -> new MicrocareSdm630Meter(componentManager,
						componentContext, cm, componentUtil, t.appManagerUtil),
				"App.Meter.Microcare.Sdm630");
	}

	// ess-controller

	/**
	 * Test method for creating a {@link FixActivePower}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FixActivePower fixActivePower(AppManagerTestBundle t) {
		return app(t, FixActivePower::new, "App.Ess.FixActivePower");
	}

	/**
	 * Test method for creating a {@link PrepareBatteryExtension}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final PrepareBatteryExtension prepareBatteryExtension(AppManagerTestBundle t) {
		return app(t, PrepareBatteryExtension::new, "App.Ess.PrepareBatteryExtension");
	}

	private static final <T> T app(AppManagerTestBundle t, DefaultAppConstructor<T> constructor, String appId) {
		return constructor.create(t.componentManger, AppManagerTestBundle.getComponentContext(appId), t.cm,
				t.componentUtil);
	}

	private static interface DefaultAppConstructor<A> {

		public A create(ComponentManager componentManager, ComponentContext componentContext, ConfigurationAdmin cm,
				ComponentUtil componentUtil);

	}

}
