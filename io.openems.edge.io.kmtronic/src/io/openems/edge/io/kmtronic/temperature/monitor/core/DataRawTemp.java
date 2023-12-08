package io.openems.edge.io.kmtronic.temperature.monitor.core;

import java.util.ArrayList;
import java.util.List;

public class DataRawTemp {

	private final List<String> ids;
	private final List<String> names;
	private final List<Float> temps;

	public DataRawTemp() {
		this.ids = new ArrayList<>();
		this.names = new ArrayList<>();
		this.temps = new ArrayList<>();
	}

	/**
	 * Get the id by index.
	 * 
	 * @param index the index.
	 * @return the id
	 */
	public String getId(int index) {
		return this.ids.get(index);
	}

	public void setId(String id) {
		this.ids.add(id);
	}

	/**
	 * Get the name by index.
	 * 
	 * @param index the index.
	 * @return the name
	 */
	public String getName(int index) {
		return this.names.get(index);
	}

	public void setName(String name) {
		this.names.add(name);
	}

	/**
	 * Get the temperature by index.
	 * 
	 * @param index the index.
	 * @return the temperature
	 */
	public Float getTemp(int index) {
		return this.temps.get(index);
	}

	public void setTemp(Float temp) {
		this.temps.add(temp);
	}

}
