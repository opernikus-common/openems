package io.openems.edge.evcharger.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.TimedataProvider;

/**
 * ManageableEvCharger describes a manageable EvCharger.
 */
@ProviderType
public interface ManageableEvCharger extends EvCharger, ElectricityMeter, TimedataProvider, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// TODO switch to EVCharger power object
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Indicates if EvCharger is able of reducing the number of used phases.
	 * 
	 * @return true if phaseLimitation is possible, false else.
	 */
	public default boolean isNumberOfPhasesLimitable() {
		return false;
	}

	/**
	 * Apply the charge current in mA.
	 * 
	 * <p>
	 * Note that most chargepoints can only set one current. This current is applied
	 * to the chargepoint and depending on the connected car, the car is charged
	 * with applied current on L1 and/or on L2 and/or on L3. That means that a car
	 * with a one phase charge will charge with 1 x current. A car with a three
	 * phase charger will charge with 3 x current.
	 * 
	 * @param current           in mA.
	 * @param maxNumberOfPhases some chargepoints can limit their number of used
	 *                          active phases (can be 1,2 or 3).
	 * @throws Exception on write error.
	 */
	public void applyCurrent(int current, int maxNumberOfPhases) throws Exception;

	// TODO
	// /**
	// * Used for Modbus/TCP Api Controller. Provides a Modbus table for the
	// Channels
	// * of this Component.
	// *
	// * @param accessMode filters the Modbus-Records that should be shown
	// * @return the {@link ModbusSlaveNatureTable}
	// */
	// public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode
	// accessMode) {
	// return ModbusSlaveNatureTable.of(ManageableEvCharger.class, accessMode, 100)
	// //
	// .channel(0, RawChannelId.SET_CHARGE_POWER_LIMIT, ModbusType.UINT16) //
	// .channel(1, RawChannelId.SET_DISPLAY_TEXT, ModbusType.STRING16) //
	// .channel(17, RawChannelId.SET_ENERGY_LIMIT, ModbusType.UINT16) //
	// .channel(18, RawChannelId.PRIORITY, ModbusType.ENUM16) //
	// .build();
	// }

}
