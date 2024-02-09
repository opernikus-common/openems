package io.openems.edge.simulator.datasource.csv.eventbased;

import java.io.IOException;
import java.time.LocalTime;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.simulator.CsvFormat;
import io.openems.edge.simulator.CsvUtils;
import io.openems.edge.simulator.DataContainer;
import io.openems.edge.simulator.datasource.api.AbstractCsvDatasource;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.Datasource.CSV.EventBased", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
})
public class CsvDatasourceEventBased extends AbstractCsvDatasource
		implements SimulatorDatasource, OpenemsComponent, EventHandler {

	private static final String EVENT_TIME_HOUR = "EventTimeHour";
	private static final String EVENT_TIME_MINUTE = "EventTimeMin";

	private final Logger log = LoggerFactory.getLogger(CsvDatasourceEventBased.class);

	@Reference
	private ComponentManager componentManager;

	private Config config;

	private LocalTime currentEventTime = LocalTime.of(0, 0);

	public CsvDatasourceEventBased() {
		super(//
				OpenemsComponent.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws NumberFormatException, IOException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), 0);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE -> {
			this.selectRecord();
		}
		}
	}

	private void selectRecord() {
		LocalTime now = LocalTime.now(this.componentManager.getClock());
		try {
			LocalTime nextEventTime = LocalTime.of(this.getNextValue(EVENT_TIME_HOUR).intValue(),
					this.getNextValue(EVENT_TIME_MINUTE).intValue());
			// not the end of the datasource
			if (this.currentEventTime.isBefore(nextEventTime)) {
				if (nextEventTime.isBefore(now)) {
					this.data.nextRecord();
					this.currentEventTime = nextEventTime;
				}
			} else { // end of the datasource: wait until midnight and start again
				if (now.getHour() == 0) {
					this.currentEventTime = now;
				}
			}
		} catch (Exception e) {
			this.logError(this.log, "Could not read time for next event");
			e.printStackTrace();
		}
	}

	private Float getNextValue(String name) throws NumberFormatException, IOException {
		return this.data.getNextValue(name).get();
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Override
	protected DataContainer getData() throws NumberFormatException, IOException {
		return CsvUtils.readCsvFileFromResource(CsvDatasourceEventBased.class, this.config.source().filename,
				CsvFormat.ENGLISH, 1);
	}
}
