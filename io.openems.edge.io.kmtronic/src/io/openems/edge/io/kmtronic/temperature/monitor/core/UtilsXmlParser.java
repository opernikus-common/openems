package io.openems.edge.io.kmtronic.temperature.monitor.core;

import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.google.common.io.CharSource;

import io.openems.common.exceptions.OpenemsException;

public class UtilsXmlParser {

	private static XMLInputFactory inputFactory;

	static {
		inputFactory = XMLInputFactory.newInstance();
		inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
	}

	/**
	 * Parses the given xml string.
	 * 
	 * @param xmlData the xmlData to parse
	 * @return an object of type DataRawTemp
	 * @throws OpenemsException on any error
	 */
	public static DataRawTemp parseData(String xmlData) throws OpenemsException {
		try {
			var inputStream = CharSource.wrap(xmlData).asByteSource(StandardCharsets.UTF_8).openStream();
			var reader = inputFactory.createXMLStreamReader(inputStream);
			var result = new DataRawTemp();
			while (reader.hasNext()) {
				var eventType = reader.next();
				switch (eventType) {
				case XMLStreamConstants.START_ELEMENT: {
					var elementName = reader.getLocalName();
					if (elementName.equals("id")) {
						result.setId(readCharacters(reader));
					} else if (elementName.equals("name")) {
						result.setName(readCharacters(reader));
					} else if (elementName.equals("temp")) {
						result.setTemp(readFloat(reader));
					}
				}
				}
			}

			return result;
		} catch (Exception e) {
			throw new OpenemsException("Unable to parse xml " + e.getMessage());
		}
	}

	private static String readCharacters(XMLStreamReader reader) throws XMLStreamException {
		var result = new StringBuilder();
		while (reader.hasNext()) {
			var eventType = reader.next();
			switch (eventType) {
			case XMLStreamConstants.CHARACTERS:
			case XMLStreamConstants.CDATA:
				result.append(reader.getText());
				break;
			case XMLStreamConstants.END_ELEMENT:
				return result.toString();
			}
		}
		throw new XMLStreamException("Premature end of file");
	}

	private static Float readFloat(XMLStreamReader reader) throws NumberFormatException, XMLStreamException {
		var input = readCharacters(reader);
		if (input.contains("---")) {
			return null;
		}
		return Float.parseFloat(input);
	}

}
