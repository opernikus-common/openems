package io.openems.edge.heater.chp.dachs;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.heater.api.Chp;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.ManagedChp;
import io.openems.edge.heater.api.ManagedHeaterByOperationMode;

/**
 * Chp Dachs GLT interface. This controller communicates with a Senertec Dachs
 * Chp via the GLT web interface and maps the return message to OpenEMS
 * channels. Read and write is supported. Not all GLT commands have been coded
 * in yet, only those for basic CHP operation.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Chp.SenertecDachs", immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE })

public class DachsGltImpl extends AbstractOpenemsComponent
		implements Heater, Chp, ManagedHeaterByOperationMode, ManagedChp, DachsGlt, OpenemsComponent, EventHandler {

	protected final Logger log = LoggerFactory.getLogger(DachsGltImpl.class);

	private Config config;

	private ReadWorker readWorker;
	private WriteWorker writeWorker;

	public DachsGltImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DachsGlt.ChannelId.values(), //
				Chp.ChannelId.values(), //
				Heater.ChannelId.values(), //
				ManagedHeaterByOperationMode.ChannelId.values(), //
				ManagedChp.ChannelId.values()
		);

	}

	protected Config getConfig() {
		return this.config;
	}

	@Activate
	void activate(ComponentContext context, Config config)
			throws OpenemsError.OpenemsNamedException, ConfigurationException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

        DachsDevice dachs = new DachsDevice(this);
        this.readWorker = new ReadWorker(this, dachs);
		this.readWorker.activate(config.id() + ".rw");
		if (!this.config.readOnly()) {
            this.writeWorker = new WriteWorker(this, dachs);
			this.writeWorker.activate(config.id() + ".ww");
		}
	}

	@Deactivate
	protected void deactivate() {
		if (this.readWorker != null) {
			this.readWorker.deactivate();
        }
        if (this.writeWorker != null) {
			this.writeWorker.deactivate();
		}
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.readWorker.triggerNextRun();
			break;
            //TODO check later -> if instead of after process image ->
            // use topic cycle after controllers since we await the controller input
            // and then write into the chp dachs with incoming information
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			if (!this.config.readOnly()) {
				this.writeWorker.triggerNextRun();
			}
			break;
		}

	}

	protected void logInfo(String txt) {
		this.logInfo(this.log, txt);
	}

    //unused atm ? TODO either remove or add somewhere
	protected void logWarn(String txt) {
		this.logWarn(this.log, txt);
	}

	protected void logError(String txt) {
		this.logError(this.log, txt);
	}

	@Override
	public String debugLog() {
		return this.getStateError().getName() + " " + this.getStateWarning().getName();
	}

}
