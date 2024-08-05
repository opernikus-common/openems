package io.openems.backend.common.oemprovider;

public interface AggregatedList {
	/**
	 * Gets the {@link ChannelType} of the given channel. This is used by e.g. the
	 * Aggregated Influx to determine where to write the channel values into. Only
	 * Channels, that are in the aggregatedInfluxChannel.config will be written to
	 * the db.
	 *
	 * @param channel the channelAddress
	 * @return the corresponding {@link ChannelType}
	 */
	public ChannelType timedataGetChannelType(String channel);

	/**
	 * When the db wants to add a db entry, it needs to know what {@link DataType}
	 * the channel has. Usually the influx does nothing on
	 * {@link DataType#UNDEFINED}.
	 *
	 * @param channel the channelAddress
	 * @return the correlating {@link DataType}
	 */
	public DataType timedataFetchAggregatedChannel(String channel);

	/**
	 * Check if the given channel is an average channel.
	 * 
	 * @param channel the channel to check
	 * @return true if channel is an average channel
	 */
	public boolean isAverageChannel(String channel);

	/**
	 * Check if the given channel is a max channel.
	 * 
	 * @param channel the channel to check
	 * @return true if channel is a max channel
	 */
	public boolean isMaxChannel(String channel);

}
