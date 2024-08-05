package io.openems.backend.common.timedata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Class to help filter EdgeIds. Timedata can create this to apply custom
 * edgeId filter.
 */
public class TimedataEdgeFilter {

	private final Logger log = LoggerFactory.getLogger(TimedataEdgeFilter.class);
	private final List<String> filter = new ArrayList<>();
	private final String compId;

	private final Set<String> applicableIds = new HashSet<>();
	private boolean verbose = false;

	public TimedataEdgeFilter(String compId, String[] filterOptions) {
		this.compId = compId;
		this.filter.addAll(Arrays.stream(filterOptions).filter(entry -> !entry.equals("")) //
				.filter(Pattern.compile("\\d+").asPredicate()) //
				.toList());
	}

	public TimedataEdgeFilter(String compId, String[] filterOptions, boolean verbose) {
		this(compId, filterOptions);
		this.verbose = verbose;
	}

	/**
	 * Method for {@link Timedata} to check if their edge id number pattern, matches
	 * their given filter. When no filters are given return true.
	 *
	 * @param id the given edge id, later trimmed down to only numbers
	 * 
	 * @return a Boolean
	 */
	public boolean _isEdgeIdApplicable(String id) {
		if (this.filter.isEmpty()) {
			this.logAllowedEdgeId(id);
			return true;
		}
		var applicable = this.filter//
				.stream() //
				.anyMatch(id.replaceAll("\\D+", "")::startsWith);
		if (applicable) {
			this.logAllowedEdgeId(id);
		}
		return applicable;
	}

	private void logAllowedEdgeId(String id) {
		if (!this.verbose) {
			return;
		}
		if (!this.applicableIds.add(id)) {
			return;
		}
		this.log.info(this.compId + ": Allowed EdgeId: " + id);
	}
}
