package io.openems.backend.core.oemprovider;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.oemprovider.AggregatedList;
import io.openems.backend.common.oemprovider.OemProvider;
import io.openems.backend.common.oemprovider.WhiteList;
import io.openems.backend.core.oemprovider.whitelist.AggregatedListImpl;
import io.openems.backend.core.oemprovider.whitelist.WhitelistImpl;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = OemProvider.SINGLETON_SERVICE_PID, //
		immediate = true //
)
public class OemProviderImpl extends AbstractOpenemsBackendComponent implements OemProvider {
	private Config config;

	private final Logger log = LoggerFactory.getLogger(OemProvider.class);

	public OemProviderImpl() {
		super("OemProviderImpl");
	}

	/**
	 * Activates the component.
	 * 
	 * @param config the {@link Config Configuration}
	 */
	@Activate
	public void activate(Config config) {
		this.config = config;
	}

	@Deactivate
	private void deactivate() {
	}

	@Override
	public WhiteList getWhitelist(String componentId, String channelWhitelist) {
		this.logInfo(this.log, componentId + ": Creating Whitelist: " + channelWhitelist);
		var whitelist = new WhitelistImpl(componentId, channelWhitelist, this.config.verbose());
		return whitelist;
	}

	@Override
	public AggregatedList getAggregatedList(String componentId, String aggregatedChannelList) {
		this.logInfo(this.log, componentId + ": Creating aggregated list: " + aggregatedChannelList);
		var aggrList = new AggregatedListImpl(componentId, aggregatedChannelList, this.config.verbose());
		return aggrList;
	}

}
