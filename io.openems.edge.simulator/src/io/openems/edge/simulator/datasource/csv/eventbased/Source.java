package io.openems.edge.simulator.datasource.csv.eventbased;

public enum Source {
    EVENT_BASED_1("evcs-event-based-charging-profile-1.csv"), //
    EVENT_BASED_2("evcs-event-based-charging-profile-2.csv"), //
    EVENT_BASED_3("evcs-event-based-charging-profile-3.csv"), //
    EVENT_BASED_4("evcs-event-based-charging-profile-4.csv"), //
    NOT_CONNECTED("not-connected.csv"), //
    ALWAYS_CONNECTED_ONE_PHASE("always-connected-1-phase.csv"), //
    ALWAYS_CONNECTED_TWO_PHASE("always-connected-2-phase.csv"), //
    ALWAYS_CONNECTED_THREE_PHASE("always-connected-3-phase.csv");

    protected final String filename;

    private Source(String filename) {
	this.filename = filename;
    }
}
