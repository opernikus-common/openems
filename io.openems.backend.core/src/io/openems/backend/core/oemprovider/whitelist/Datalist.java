package io.openems.backend.core.oemprovider.whitelist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Datalist {

	private final List<Pattern> patterns = new ArrayList<>();

	/**
	 * Loads a list of channel names used for writing to the database.
	 *
	 * <p>
	 * Each line represents a regular expression, for example:
	 * meter[0-9]/ActivePower[L1-L3]
	 *
	 * @param fname the location of the file to read
	 * @throws IOException on any read exception
	 */
	public void load(String fname) throws IOException {

		this.patterns.clear();

		File path = new File(fname);
		try (var br = new BufferedReader(new FileReader(path))) {
			var lines = br.lines().toList();
			lines.forEach(line -> {
				if (!line.startsWith("#") && !line.trim().isEmpty()) {
					this.patterns.add(Pattern.compile(line.trim()));
				}
			});
		}
	}

	public List<Pattern> getAllDatalistPatterns() {
		return this.patterns;
	}

}
