package io.openems.backend.common.oemprovider;

import org.osgi.annotation.versioning.ProviderType;

/**
 * OemProvider provides information used by a given Provider.
 */
@ProviderType
public interface OemProvider {

	public static final String SINGLETON_SERVICE_PID = "Core.OemProvider";

	/**
	 * Gets the {@link WhiteList}.
	 * 
	 * @param componentId      the componentId of the requesting timedata object.
	 * @param channelWhitelist the filename of the channel whitelist
	 * @return a {@link WhiteList}
	 */
	WhiteList getWhitelist(String componentId, String channelWhitelist);

	/**
	 * Gets the {@link AggregatedList}.
	 * 
	 * @param componentId           the componentId of the requesting timedata
	 *                              object.
	 * @param aggregatedChannelList the filename of the aggregated channel list
	 * @return a {@link AggregatedList}
	 */
	AggregatedList getAggregatedList(String componentId, String aggregatedChannelList);

}
