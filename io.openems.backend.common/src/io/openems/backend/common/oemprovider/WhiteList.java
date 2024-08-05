package io.openems.backend.common.oemprovider;

/**
 * The WhiteList class handles access / writes of timedata databases.
 */
public interface WhiteList {

	/**
	 * Checks if a given channel is on the black list. Only channels on the
	 * whitelist are allowed to be written to the timedata database.
	 *
	 * @param channel the channel to check, typically in form
	 *                "componentID/channelID".
	 *
	 * @return true if the channel must not be written to the database. False if the
	 *         channel is allowed to be written to database.
	 */
	public boolean isChannelBlacklisted(String channel);

}
