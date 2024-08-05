package io.openems.backend.core.oemprovider.whitelist;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.oemprovider.AggregatedList;
import io.openems.backend.common.oemprovider.ChannelType;
import io.openems.backend.common.oemprovider.DataType;

/**
 * The Whitelist class, handles the aggregated list channels for the Timedata
 * writes.
 */
public class AggregatedListImpl implements AggregatedList {

	private final Logger log = LoggerFactory.getLogger(AggregatedListImpl.class);
	private final PatternList aggregatedList = new PatternList();
	private final Map<String, DataType> averageChannel = new HashMap<>();
	private final Map<String, DataType> maxChannel = new HashMap<>();
	private final Map<String, DataType> undefinedChannel = new HashMap<>();
	private final AtomicReference<ChannelType> foundDataType = new AtomicReference<>(ChannelType.UNDEFINED);
	private final String componentId;
	protected boolean debug;

	public AggregatedListImpl(String componentId, String aggregatedWhitelist, boolean debug) {
		this.debug = debug;
		this.componentId = componentId;

		this.updateAggregatedWhitelist(aggregatedWhitelist);
	}

	protected void updateAggregatedWhitelist(String filename) {
		try {
			this.aggregatedList.load(filename);
			this.log.info(this.componentId + ": Aggregated list " + filename + " successfully loaded.");
		} catch (IOException e) {
			this.log.error("ERROR: Unable to parse aggregated list file. Got exception " + e.getMessage());
		}
	}

	private void timedataAggregatedInfluxGetAllowedAverageChannels(String channel) {
		if (this.averageChannel.containsKey(channel)) {
			this.foundDataType.set(ChannelType.AVG);
		}
		if (this.foundDataType.get() == ChannelType.UNDEFINED) {
			this.searchPatternAndPutToMap(this.averageChannel, "AVG", channel);
		}
	}

	private void timedataAggregatedInfluxGetAllowedCumulatedChannels(String channel) {
		if (this.maxChannel.containsKey(channel)) {
			this.foundDataType.set(ChannelType.MAX);
		}
		this.searchPatternAndPutToMap(this.maxChannel, "MAX", channel);
	}

	private void searchPatternAndPutToMap(Map<String, DataType> channelMap, String channelType, String channel) {
		var patterns = this.aggregatedList.getAllWhitelistPatterns();
		patterns.stream().filter(pattern -> {
			var str = pattern.toString().split(", ");
			return str.length == 3 && str[0].equalsIgnoreCase(channelType)
					&& this.matchesChannel(str[2].trim(), channel);
		}).findFirst().ifPresent(foundPattern -> {
			var str = foundPattern.toString().split(", ");
			channelMap.put(channel, DataType.fromString(str[1]));
			this.foundDataType.set(ChannelType.fromString(channelType));
		});
	}

	private boolean matchesChannel(String expr, String channel) {
		return Pattern.compile(expr).matcher(channel).find();
	}

	@Override
	public ChannelType timedataGetChannelType(String channel) {
		this.foundDataType.set(ChannelType.UNDEFINED);
		if (!this.undefinedChannel.containsKey(channel)) {
			this.timedataAggregatedInfluxGetAllowedAverageChannels(channel);
			if (this.foundDataType.get() == ChannelType.UNDEFINED) {
				this.timedataAggregatedInfluxGetAllowedCumulatedChannels(channel);
				if (this.foundDataType.get() == ChannelType.UNDEFINED) {
					this.undefinedChannel.put(channel, DataType.UNDEFINED);
				}
			}
		}
		return this.foundDataType.get();
	}

	@Override
	public DataType timedataFetchAggregatedChannel(String channel) {
		if (this.undefinedChannel.containsKey(channel)) {
			return DataType.UNDEFINED;
		}
		var type = this.averageChannel.getOrDefault(channel, DataType.UNDEFINED);
		if (type == DataType.UNDEFINED) {
			type = this.maxChannel.getOrDefault(channel, DataType.UNDEFINED);
		}
		return type;
	}

	@Override
	public boolean isAverageChannel(String channel) {
		// TODO this is low performance, please fix before using it in productive
		// environment with 1000+ systems
		var type = this.averageChannel.getOrDefault(channel, DataType.UNDEFINED);
		return type != DataType.UNDEFINED;
	}

	@Override
	public boolean isMaxChannel(String channel) {
		// TODO this is low performance, please fix before using it in productive
		// environment with 1000+ systems
		var type = this.maxChannel.getOrDefault(channel, DataType.UNDEFINED);
		return type != DataType.UNDEFINED;
	}

}
