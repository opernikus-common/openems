package io.openems.edge.io.test;

import java.util.ArrayList;
import java.util.List;

import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.io.api.bsp.BoardSupportPackage;

/**
 * Provides a simple, simulated {@link BoardSupportPackage} Component that can
 * be used together with the OpenEMS Component test framework.
 */
public class DummyBoardSupportPackage extends AbstractDummyOpenemsComponent<DummyBoardSupportPackage>
		implements BoardSupportPackage {

	private final List<BooleanReadChannel> inputs = new ArrayList<>();
	private final List<BooleanWriteChannel> outputs = new ArrayList<>();

	public DummyBoardSupportPackage(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				BoardSupportPackage.ChannelId.values() //
		);
		this.inputs.add(this.getDigitalIn1Channel());
		this.inputs.add(this.getDigitalIn2Channel());
		this.inputs.add(this.getDigitalIn3Channel());
		this.inputs.add(this.getDigitalIn4Channel());
		this.outputs.add(this.getDigitalOut1WriteChannel());
	}

	@Override
	protected DummyBoardSupportPackage self() {
		return this;
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {

		return this.inputs.toArray(new BooleanReadChannel[0]);
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.outputs.toArray(new BooleanWriteChannel[0]);
	}
}
