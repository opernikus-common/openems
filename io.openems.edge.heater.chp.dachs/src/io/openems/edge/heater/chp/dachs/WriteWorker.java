package io.openems.edge.heater.chp.dachs;

import io.openems.common.worker.AbstractCycleWorker;

public class WriteWorker extends AbstractCycleWorker {

    private final DachsGltImpl parent;
    private final DachsDevice dachs;

    public WriteWorker(DachsGltImpl parent, DachsDevice dachs) {
        this.parent = parent;
        this.dachs = dachs;
    }

    @Override
    protected void forever() throws Throwable {

        this.parent._setOperationModeRequest(this.parent.getOperationModeRequest());

        if (this.dachs.available()) {
            this.dachs.handleActivationState();
        }
    }

}
