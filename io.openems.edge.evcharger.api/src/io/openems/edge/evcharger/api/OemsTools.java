package io.openems.edge.evcharger.api;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class OemsTools {

	private static final int WIDTH_FIRST = 30;

	/**
	 * Dumps all channels from the given component by using the given logger.
	 * 
	 * @param component the component to dump.
	 * @param log the logger to use.
	 */
	public static void logComponentChannels(OpenemsComponent component, Logger log) {

		var printedHeader = false;

		final Map<ChannelAddress, String> shouldPrint = new HashMap<>();
		component.channels().stream() //
				.sorted((c1, c2) -> c1.channelId().name().compareTo(c2.channelId().name())) //
				.forEach(channel -> {
					var unit = channel.channelDoc().getUnit().symbol;
					/*
					 * create descriptive text
					 */
					var channelText = "";
					switch (channel.channelDoc().getAccessMode()) {
					case READ_ONLY:
					case READ_WRITE:
						var description = "";
						if (channel instanceof EnumReadChannel) {
							try {
								description += channel.value().asOptionString();
							} catch (IllegalArgumentException e) {
								description += "UNKNOWN OPTION VALUE [" + channel.value().asString() + "]";
								description += "ERROR: " + e.getMessage();
							}
						}
						if (channel instanceof StateChannel && ((StateChannel) channel).value().orElse(false) == true) {
							if (!description.isEmpty()) {
								description += "; ";
							}
							description += ((StateChannel) channel).channelDoc().getText();
						}
						if (channel instanceof StateCollectorChannel
								&& ((StateCollectorChannel) channel).value().orElse(0) != 0) {
							if (!description.isEmpty()) {
								description += "; ";
							}
							description += ((StateCollectorChannel) channel).listStates();
						}
						channelText = String.format("%15s %-3s %s", //
								channel.value().asStringWithoutUnit(), //
								unit, //
								description.isEmpty() ? "" : "(" + description + ")");
						break;

					case WRITE_ONLY:
						channelText += "WRITE_ONLY";
					}
					// Build complete line
					var line = String.format("%-" + WIDTH_FIRST + "s : %s", channel.channelId().id(), channelText);
					// Print the line only if is not equal to the last printed line
					shouldPrint.put(channel.address(), line);
					// Add line to last printed lines
				});

		if (!shouldPrint.isEmpty()) {
			if (!printedHeader) {
				/*
				 * Print header (this is not the first run)
				 */
				OpenemsComponent.logInfo(component, log, "=======================================");
				OpenemsComponent.logInfo(component, log, "ID: " + component.id());
			}

			OpenemsComponent.logInfo(component, log, "---------------------------------------");
			shouldPrint.values().stream().sorted().forEach(line -> {
				OpenemsComponent.logInfo(component, log, line);
			});
			OpenemsComponent.logInfo(component, log, "---------------------------------------");
		}
	}
}
