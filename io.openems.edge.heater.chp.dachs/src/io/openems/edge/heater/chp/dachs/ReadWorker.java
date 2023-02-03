package io.openems.edge.heater.chp.dachs;

import io.openems.common.worker.AbstractCycleWorker;

public class ReadWorker extends AbstractCycleWorker {

	private final DachsGltImpl parent;
	private final DachsDevice dachs;
	private int coreCycleIntervalCounter;

	public ReadWorker(DachsGltImpl parent, DachsDevice dachs) {
		this.parent = parent;
		this.dachs = dachs;
		this.coreCycleIntervalCounter = this.parent.getConfig().interval();
	}

	@Override
	protected void forever() throws Throwable {

		if (this.coreCycleIntervalCounter++ < this.parent.getConfig().interval()) {
			return;
		}
		this.coreCycleIntervalCounter = 0;

		try {
			this.dachs.readValuesAndMapToChannel();
			this.parent._setReadError(false);
		} catch (Exception e) {
			this.parent._setReadError(true);
			this.parent.logError("Error reading channels. EX: " + e.getMessage());
		}
	}

}
