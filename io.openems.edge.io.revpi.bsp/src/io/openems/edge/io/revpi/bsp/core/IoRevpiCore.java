package io.openems.edge.io.revpi.bsp.core;

import java.io.IOException;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface IoRevpiCore extends OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		JRE_TOTAL_MEMORY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("JRE Total Memory in Bytes")), //
		JRE_FREE_MEMORY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("JRE Free Memory in Bytes")), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}

	/**
	 * Toggles the Hardware watchdog.
	 *
	 * @throws IOException on any error
	 */
	public void toggleWatchdog() throws IOException;
}
