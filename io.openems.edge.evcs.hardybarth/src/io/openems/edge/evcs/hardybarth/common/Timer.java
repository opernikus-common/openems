package io.openems.edge.evcs.hardybarth.common;

import java.time.LocalDateTime;

public class Timer {

	private int timeInSeconds;
	private LocalDateTime timer;
	
	public Timer( int timeInSeconds ) {
		this.timeInSeconds = timeInSeconds;
		this.timer = LocalDateTime.now().plusSeconds(this.timeInSeconds);
	}

	public boolean hasExpired() {
		if(this.timer.isAfter(LocalDateTime.now())) {
			return false;
		}
		this.timer = LocalDateTime.now().plusSeconds(this.timeInSeconds);
		return true;
	}

}
