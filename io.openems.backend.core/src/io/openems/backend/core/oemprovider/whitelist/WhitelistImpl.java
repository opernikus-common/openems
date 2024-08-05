package io.openems.backend.core.oemprovider.whitelist;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.oemprovider.WhiteList;

/**
 * The Whitelist class, handles the whitelist/blacklist channels for the
 * Timedata writes.
 */
public class WhitelistImpl implements WhiteList {

	private final Logger log = LoggerFactory.getLogger(WhitelistImpl.class);
	private final PatternList whitelist = new PatternList();
	private final Set<String> blacklistedChannels = new HashSet<>();
	private final Set<String> whitelistedChannels = new HashSet<>();
	private final String componentId;
	private final boolean debug;

	public WhitelistImpl(String componentId, String filenameWhitelist, boolean debug) {
		this.debug = debug;
		this.componentId = componentId;
		this.updateWhitelist(filenameWhitelist);
	}

	protected void updateWhitelist(String filename) {
		try {
			this.whitelist.load(filename);
			this.log.info(this.componentId + ": Whitelist " + filename + " successfully loaded.");
		} catch (IOException e) {
			this.log.error("ERROR: Unable to parse whitelist file. Got exception " + e.getMessage());
		}
	}

	private List<Pattern> getTimedataWhitelistPatterns() {
		return this.whitelist.getAllWhitelistPatterns();
	}

	@Override
	public boolean isChannelBlacklisted(String channelAddress) {

		if (this.whitelistedChannels.contains(channelAddress)) {
			return false;
		}
		if (this.blacklistedChannels.contains(channelAddress)) {
			return true;
		}

		// working with blacklisted/whitelistedChannels before, reduces CPU stress due
		// to massive regexp pattern matchings later.

		var match = this.getTimedataWhitelistPatterns().stream() //
				.anyMatch(pattern -> pattern.matcher(channelAddress).matches());
		if (match) {
			this.whitelistedChannels.add(channelAddress);
			return false;
		}
		if (this.blacklistedChannels.add(channelAddress)) {
			if (this.debug) {
				this.log.info(this.componentId + ": Blacklisted Channel: " + channelAddress);
			} else {
				if (this.blacklistedChannels.size() % 100 == 0) {
					this.log.info(this.componentId + ": # of Channels whitelisted: " + this.whitelistedChannels.size()
							+ ", blacklisted: " + this.blacklistedChannels.size());
				}
			}
		}

		return true;
	}

}
