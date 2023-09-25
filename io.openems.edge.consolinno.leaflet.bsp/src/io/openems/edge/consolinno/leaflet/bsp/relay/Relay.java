package io.openems.edge.consolinno.leaflet.bsp.relay;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;

public enum Relay implements ChannelId {

	/**
	 * Holds writes to the relay for debugging purposes.
	 *
	 * <ul>
	 * <li>Interface: Relay
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	DEBUG_OUT(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.ON_OFF)), //
	/**
	 * Relay control channel.
	 *
	 * <ul>
	 * <li>Interface: Relay
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	OUT(new BooleanDoc() //
			.unit(Unit.ON_OFF) //
			.accessMode(AccessMode.READ_WRITE) //
			.onChannelSetNextWriteMirrorToDebugChannel(Relay.DEBUG_OUT)), //
	/**
	 * indicator if this is a normally closed or a normally opened relay.
	 *
	 * <ul>
	 * <li>Interface: Relay state
	 * <li>Type: Boolean
	 * <li>Range: On/Offa
	 * </ul>
	 */
	NORMALLY_CLOSED(Doc.of(OpenemsType.BOOLEAN) //
			.unit(Unit.ON_OFF) //
			.accessMode(AccessMode.READ_ONLY)) //

	;

	private final Doc doc;

	private Relay(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}
