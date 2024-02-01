package io.openems.edge.consolinno.leaflet.bsp.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PositionMap {

	private final Map<Integer, List<Integer>> positionMap = new HashMap<>();

	/**
	 * This Map Saves the Positions in the Module Type (TMP,RELAY,PWM) that are
	 * Occupied by a configured Modbus Device.
	 *
	 * @param moduleNumber Number of the Module specified on the Device
	 * @param position     Pin position of the device on the module
	 */
	public PositionMap(int moduleNumber, int position) {
		List<Integer> initList = new ArrayList<>(position);
		this.positionMap.put(moduleNumber, initList);
	}

	public Map<Integer, List<Integer>> getPositionMap() {
		return this.positionMap;
	}
}
