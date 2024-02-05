package io.openems.edge.common.test;

import io.openems.edge.common.timer.Timer;

/**
 * A DummyTimer, provides BaseFunctionality for Unittests, that are using a
 * Timer.
 */
public class DummyTimer implements Timer {

	private final int maxCount;
	private int count;

	public DummyTimer(int maxCheckCalls) {
		this.maxCount = maxCheckCalls;
		this.count = this.maxCount;
	}

	@Override
	public boolean check() {
		if (this.count-- <= 0) {
			return true;
		}
		return false;
	}

	@Override
	public void reset() {
		this.count = this.maxCount;
	}
}
