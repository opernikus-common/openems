package io.openems.edge.consolinno.leaflet.bsp.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.ConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.edge.consolinno.leaflet.bsp.core.Config;
import io.openems.edge.consolinno.leaflet.bsp.core.LeafletCoreImpl;
import io.openems.edge.consolinno.leaflet.bsp.core.enums.ModuleType;

public class DataUtilities {

	private static final int HEADER_INFORMATION_OFFSET = 1;
	private static final int REGISTER_TYPE_COUNT = 4;
	private static final int GROUP_SIZE = 3;
	private static final int MINIMUM_COMPATIBLE_FIRMWARE_VERSION = 78;

	// leafletModule_x are Binary Representations of the Decimal ModuleNumber
	private static final int LEAFLET_MODULE_ONE = 1;
	private static final int LEAFLET_MODULE_TWO = 2;
	private static final int LEAFLET_MODULE_THREE = 4;
	private static final int LEAFLET_MODULE_FOUR = 8;
	private static final int LEAFLET_MODULE_FIVE = 16;
	private static final int LEAFLET_MODULE_SIX = 32;
	private static final int LEAFLET_MODULE_SEVEN = 64;
	private static final int LEAFLET_MODULE_EIGHT = 128;

	private static final String MODULE_TYPE = "Modul Typ";
	private static final String MODULE_NR = "ModulNr";
	private static final String M_REG = "Mreg";

	private final Logger log = LoggerFactory.getLogger(DataUtilities.class);

	private List<List<String>> source;
	public final Map<ModuleRegister, Integer> discreteOutputCoils = new HashMap<>();
	public final Map<ModuleRegister, Integer> discreteInputContacts = new HashMap<>();
	public final Map<ModuleRegister, Integer> analogInputRegisters = new HashMap<>();
	public final Map<ModuleRegister, Integer> analogOutputHoldingRegisters = new HashMap<>();
	public final Map<String, PinOwner> ownerMap = new HashMap<>();
	public final Map<ModuleType, PositionMap> positionMap = new HashMap<>();

	private int moduleTypeOffset;
	private int moduleNumberOffset;
	private int mRegOffset;
	// True if we are compatible
	private boolean compatible;
	// True if the compatibility Check was done once
	private boolean compatibleFlag;

	private boolean remoteLeaflet = false;

	protected SourceReader sourceReader = new SourceReader();
	private LeafletCoreImpl leaflet;

	public DataUtilities(LeafletCoreImpl leaflet, Config config) throws ConfigurationException {
		this.leaflet = leaflet;
		this.remoteLeaflet = config.runningOnDifferentLeaflet();
		// Reads Source file CSV with the Register information
		if (config.source() != null && config.source().startsWith("/")) {
			this.source = this.sourceReader.readCsv(config.source());
		} else {
			this.source = this.sourceReader.readCsv(OpenemsConstants.getOpenemsDataDir() + "/" + config.source());
		}

		if (this.source.size() == 1) {
			throw new ConfigurationException("The Source file could not be found! Check Config!");
		}

	}

	/**
	 * Simple Check if the current Group doesn't have more members.
	 *
	 * @param row   The line of the big csvOutput List
	 * @param group The number of the Group
	 *              (DiscreteOutputCoil=0,DiscreteInputContact=1,...)
	 * @return true if this is part of the Header
	 */
	public boolean checkForLastGroupMember(List<String> row, int group) {
		// Checks with +1 if the next member exists
		return (row.get(group * GROUP_SIZE + 1).equals("") || row.get(group * GROUP_SIZE + 1).equals("0"));
	}

	/**
	 * Splits the Source file of all modules into the correct type (Discrete Output
	 * Coil, Discrete Input Contact, Analog Input Register , Analog Output Holding
	 * Register).
	 */
	public void splitArrayIntoType() {
		this.getSourceHeaderOrder();
		AtomicInteger currentGroup = new AtomicInteger(0);
		// Modbus can address 4 different types of Registers. So the for loop sorts the
		// values into those 4.
		for (int group = 0; group <= REGISTER_TYPE_COUNT; group++) {
			this.source.forEach(row -> {
				if (!(row.get(0).equals("") || row.get(0).equals("Modbus Offset") || row.toString().contains("Register")
						|| row.toString().contains("Version"))) {
					if (currentGroup.get() < REGISTER_TYPE_COUNT
							&& !this.checkForLastGroupMember(row, currentGroup.get())) {
						switch (currentGroup.get()) {
						case (0): {
							this.putElementInCorrectMap(this.discreteOutputCoils, currentGroup.get(), row);
						}
							break;
						case (1): {
							this.putElementInCorrectMap(this.discreteInputContacts, currentGroup.get(), row);
						}
							break;
						case (2): {
							this.putElementInCorrectMap(this.analogInputRegisters, currentGroup.get(), row);
						}
							break;
						case (3): {
							this.putElementInCorrectMap(this.analogOutputHoldingRegisters, currentGroup.get(), row);
						}
							break;
						}
					}
				}
			});
			currentGroup.getAndIncrement();
		}
	}

	/**
	 * Converts a String into a ModuleType.
	 *
	 * @param cast The String that has to be converted
	 * @return ModuleType (ERROR if the String is not a ModuleType)
	 */
	private ModuleType stringToType(String cast) {
		if (ModuleType.contains(cast.toUpperCase().trim())) {
			return ModuleType.valueOf(cast.toUpperCase().trim());
		} else {
			return ModuleType.ERROR;
		}
	}

	/**
	 * Help method for the above Switch case.
	 *
	 * @param map   The Map this Element has to be put in (DiscreteOutputCoil etc.)
	 * @param group The Group number of the above map (0 for
	 *              DiscreteOutputCoil,etc.)
	 * @param row   The current Row of the Big Output list
	 */
	private void putElementInCorrectMap(Map<ModuleRegister, Integer> map, int group, List<String> row) {
		map.put(new ModuleRegister(this.stringToType(row.get(this.moduleTypeOffset + (group * GROUP_SIZE))),
				Integer.parseInt(row.get(this.moduleNumberOffset + (group * GROUP_SIZE))),
				Integer.parseInt(row.get(this.mRegOffset + (group * GROUP_SIZE)))), Integer.parseInt(row.get(0)));
	}

	/**
	 * Searches through the Big Source file and writes in the appropriate variable
	 * which column contains the types.
	 */
	private void getSourceHeaderOrder() {
		// The Csv source file will hold the header either in line 1 or 2
		for (int n = 0; n < 2; n++) {
			for (int i = HEADER_INFORMATION_OFFSET; i <= GROUP_SIZE; i++) {
				String current = (this.source.get(n).get(i));
				if (this.moduleNumberOffset == 0 && current.contains(MODULE_NR)) {
					this.moduleNumberOffset = i;
				} else if (this.moduleTypeOffset == 0 && current.contains(MODULE_TYPE)) {
					this.moduleTypeOffset = i;
				} else if (this.mRegOffset == 0 && current.contains(M_REG)) {
					this.mRegOffset = i;
				}
			}
		}
	}

	/**
	 * Checks if the Firmware version is at least the minimum required version for
	 * the Configurator to run properly.
	 *
	 * @return true if the Firmware is compatible
	 */
	public boolean checkFirmwareCompatibility() {
		if (!this.compatible && !this.compatibleFlag) {
			String response = "";
			if (!this.remoteLeaflet) {
				try {
					Process p = Runtime.getRuntime().exec("leafletbs -v");

					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

					String line = "";
					StringBuilder responseBuilder = new StringBuilder();
					while ((line = reader.readLine()) != null) {
						responseBuilder.append(line);
					}
					response = responseBuilder.toString();
				} catch (IOException ioException) {
					this.log.error("The Firmware is not Running!");
				}
				if (response.equals("") == false) {
					String[] partOne = response.split("V");
					String[] partTwo = partOne[1].split(" ");
					if (MINIMUM_COMPATIBLE_FIRMWARE_VERSION <= Integer.parseInt(partTwo[0].replace(".", ""))) {
						this.compatible = true;
					}
				}
				this.compatibleFlag = true;
			} else {
				this.compatible = true;
			}
		}
		return this.compatible;
	}

	/**
	 * Checks if the Pin is already in use by another Modbus device.
	 *
	 * @param type         Type of the Module (TMP,RELAY,etc.)
	 * @param moduleNumber Module number specified on the Device
	 * @param position     Position of the device on the module
	 * @param id           identifier of the device (e.g. TMP0)
	 * @return boolean
	 */
	private boolean checkPin(ModuleType type, int moduleNumber, int position, String id) {
		if (!this.positionMap.containsKey(type)) {
			this.positionMap.put(type, new PositionMap(moduleNumber, position));
			PinOwner firstRun = new PinOwner(type, moduleNumber, position);
			this.ownerMap.put(id, firstRun);
			return true;
		} else if (!this.positionMap.get(type).getPositionMap().containsKey(moduleNumber)) {
			List<Integer> initList = new ArrayList<>(position);
			this.positionMap.get(type).getPositionMap().put(moduleNumber, initList);
			PinOwner firstRun = new PinOwner(type, moduleNumber, position);
			this.ownerMap.put(id, firstRun);
			return true;
		} else if (!this.positionMap.get(type).getPositionMap().get(moduleNumber).contains(position)) {
			this.positionMap.get(type).getPositionMap().get(moduleNumber).add(position);
			PinOwner firstRun = new PinOwner(type, moduleNumber, position);
			this.ownerMap.put(id, firstRun);
			return true;
		} else {
			if (this.ownerMap.containsKey(id)) {
				return this.ownerMap.get(id).equals(new PinOwner(type, moduleNumber, position));
			} else {
				return false;
			}
		}
	}

	/**
	 * Check if the Module that is trying to activate is physically present.
	 *
	 * @param moduleType   TMP,RELAY,PWM
	 * @param moduleNumber Internal Number of the module
	 * @param position     Pin position of the Module
	 * @param id           Unique Id of the Device
	 * @return boolean true if present
	 */
	public boolean modbusModuleCheckout(ModuleType moduleType, int moduleNumber, int position, String id) {
		switch (moduleType) {
		case TMP:
			switch (moduleNumber) {
			case 1:
				// 0b001
				if (((this.leaflet.getTemperatureModules() & LEAFLET_MODULE_ONE) == LEAFLET_MODULE_ONE)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;
			case 2:
				// 0b010
				if (((this.leaflet.getTemperatureModules() & LEAFLET_MODULE_TWO) == LEAFLET_MODULE_TWO)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;
			case 3:
				// 0b100
				if (((this.leaflet.getTemperatureModules() & LEAFLET_MODULE_THREE) == LEAFLET_MODULE_THREE)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;
			}
			break;
		case REL:
			switch (moduleNumber) {
			case 1:
				// 0b0001
				if (((this.leaflet.getRelayModules() & LEAFLET_MODULE_ONE) == LEAFLET_MODULE_ONE)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;

			case 2:
				// 0b0010
				if (((this.leaflet.getRelayModules() & LEAFLET_MODULE_TWO) == LEAFLET_MODULE_TWO)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;

			case 3:
				// 0b0100
				if (((this.leaflet.getRelayModules() & LEAFLET_MODULE_THREE) == LEAFLET_MODULE_THREE)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;
			case 4:
				// 0b1000
				if (((this.leaflet.getRelayModules() & LEAFLET_MODULE_FOUR) == LEAFLET_MODULE_FOUR)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;

			}
			break;
		case PWM:
			switch (moduleNumber) {
			case 1:
				// 0b00000001
				if (((this.leaflet.getPwmModules() & LEAFLET_MODULE_ONE) == LEAFLET_MODULE_ONE)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;

			case 2:
				// 0b00000010
				if (((this.leaflet.getPwmModules() & LEAFLET_MODULE_TWO) == LEAFLET_MODULE_TWO)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;

			case 3:
				// 0b00000100
				if (((this.leaflet.getPwmModules() & LEAFLET_MODULE_THREE) == LEAFLET_MODULE_THREE)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;
			case 4:
				// 0b00001000
				if (((this.leaflet.getPwmModules() & LEAFLET_MODULE_FOUR) == LEAFLET_MODULE_FOUR)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;
			case 5:
				// 0b00010000
				if (((this.leaflet.getPwmModules() & LEAFLET_MODULE_FIVE) == LEAFLET_MODULE_FIVE)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;

			case 6:
				// 0b00100000
				if (((this.leaflet.getPwmModules() & LEAFLET_MODULE_SIX) == LEAFLET_MODULE_SIX)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;

			case 7:
				// 0b01000000
				if (((this.leaflet.getPwmModules() & LEAFLET_MODULE_SEVEN) == LEAFLET_MODULE_SEVEN)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;
			case 8:
				// 0b10000000
				if (((this.leaflet.getPwmModules() & LEAFLET_MODULE_EIGHT) == LEAFLET_MODULE_EIGHT)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;
			}

			break;

		case AIO:
			switch (moduleNumber) {
			case 1:
				// 0b00000001
				if (((this.leaflet.getAioModules() & LEAFLET_MODULE_ONE) == LEAFLET_MODULE_ONE)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;

			case 2:
				// 0b00000010
				if (((this.leaflet.getAioModules() & LEAFLET_MODULE_TWO) == LEAFLET_MODULE_TWO)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;

			case 3:
				// 0b00000100
				if (((this.leaflet.getAioModules() & LEAFLET_MODULE_THREE) == LEAFLET_MODULE_THREE)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;
			case 4:
				// 0b00001000
				if (((this.leaflet.getAioModules() & LEAFLET_MODULE_FOUR) == LEAFLET_MODULE_FOUR)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;
			case 5:
				// 0b00010000
				if (((this.leaflet.getAioModules() & LEAFLET_MODULE_FIVE) == LEAFLET_MODULE_FIVE)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;

			case 6:
				// 0b00100000
				if (((this.leaflet.getAioModules() & LEAFLET_MODULE_SIX) == LEAFLET_MODULE_SIX)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;

			case 7:
				// 0b01000000
				if (((this.leaflet.getAioModules() & LEAFLET_MODULE_SEVEN) == LEAFLET_MODULE_SEVEN)
						&& this.checkPin(moduleType, moduleNumber, position, id)) {
					this.positionMap.get(moduleType).getPositionMap().get(moduleNumber).add(position);
					return true;
				}
				break;
			}
			break;
		case LEAFLET: {
			this.log.error("This should never happen. LEAFLET called modbusModuleCheckout");
			break;
		}
		case GPIO:
			// IS on Leaflet itself -> checkPin
			return this.checkPin(ModuleType.LEAFLET, moduleNumber, position, id);
		case ERROR: {
			this.log.error("This should never happen. ERROR called modbusModuleCheckout");
			break;
		}

		}
		return false;
	}

}
