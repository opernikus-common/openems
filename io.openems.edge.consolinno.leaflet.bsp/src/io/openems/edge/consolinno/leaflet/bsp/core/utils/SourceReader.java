package io.openems.edge.consolinno.leaflet.bsp.core.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Reads a CSV File and puts it into one big List. Every Element of that List is
 * another List(String row)
 */
public class SourceReader {

	/**
	 * Reads a CSV file from the given path.
	 * 
	 * @param path the path to read the CSV file from.
	 * @return a list with a list of strings.
	 */
	public List<List<String>> readCsv(String path) {
		List<List<String>> output = new ArrayList<>();
		try (Scanner csvScan = new Scanner(new File(path))) {
			while (csvScan.hasNextLine()) {
				output.add(this.getOutputFromLine(csvScan.nextLine()));

			}
			return output;

		} catch (Exception e) {

			List<String> error = new ArrayList<>();
			error.add("Source file not Found");
			output.add(error);

		}
		return output;
	}

	private List<String> getOutputFromLine(String thisLine) {
		List<String> content = new ArrayList<>();
		try (Scanner rowScanner = new Scanner(thisLine)) {
			rowScanner.useDelimiter(",");
			while (rowScanner.hasNext()) {

				content.add(rowScanner.next());
			}
		}
		return content;
	}
}
