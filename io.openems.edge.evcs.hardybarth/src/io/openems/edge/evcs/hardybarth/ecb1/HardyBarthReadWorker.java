package io.openems.edge.evcs.hardybarth.ecb1;

import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.common.channel.ChannelId;

public class HardyBarthReadWorker extends AbstractCycleWorker {

	public static final String SLASH								= "/";
	
	private final HardyBarthImpl parent;

	private int meterIndex;
	private int chargeControlIndex;
	
	public HardyBarthReadWorker(HardyBarthImpl parent, int meterIndex, int chargeControlIndex) {
		super();
		this.parent = parent;
		this.meterIndex = meterIndex;
		this.chargeControlIndex = chargeControlIndex;
	}

	@Override
	protected void forever() throws OpenemsNamedException {

		try {
			//http://apidoc.ecb1.de/
			JsonElement json = this.parent.api.sendGetRequest(Consts.Endpoint.METERS.toUri() + SLASH + this.meterIndex);
			if (json == null) {
				return;
			}
			this.mapJsonToChannel(json);
			
			json = this.parent.api.sendGetRequest(Consts.Endpoint.CHARGE_CONTROLS.toUri());
			if (json == null) {
				return;
			}
			this.setChargeControls(json);
			this.parent.setCommunicationFailedRead(false);
			
		}catch(Exception e) {
			this.parent.setCommunicationFailedRead(true);
			this.parent.logError(this.parent.log, "Error communicating with charging station " + e.getMessage());
		}
	}
	
	private void mapJsonToChannel(JsonElement json) {
		for (HardyBarth.ChannelId channelId : HardyBarth.ChannelId.values()) {
			String[] jsonPaths = channelId.getJsonPaths();
			Object value = this.getValueFromJson(channelId, json, channelId.converter, jsonPaths);
			if(value != null) {
				value = this.adoptChannel(channelId, value);
				this.parent.channel(channelId).setNextValue(value);
			}
		}
	}
	
	
	private Object adoptChannel(HardyBarth.ChannelId channelId,
			Object value) {
		
		if(channelId == HardyBarth.ChannelId.RAW_ACTIVE_VOLTAGE_L1) {
			return floatWithPrecision((float) value, 1) * 1000;
		}else if(channelId == HardyBarth.ChannelId.RAW_ACTIVE_VOLTAGE_L2) {
			return floatWithPrecision((float) value, 1) * 1000;
		}else if(channelId == HardyBarth.ChannelId.RAW_ACTIVE_VOLTAGE_L3) {
			return floatWithPrecision((float) value, 1) * 1000;
		}else if(channelId == HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS) {
			return floatWithPrecision((float) value, 0);
		}else if(channelId == HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L1) {
			return floatWithPrecision((float) value, 0);
		}else if(channelId == HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L2) {
			return floatWithPrecision((float) value, 0);
		}else if(channelId == HardyBarth.ChannelId.RAW_ACTIVE_POWER_PLUS_L3) {
			return floatWithPrecision((float) value, 0);
		}
		return value;
	}

	private Float floatWithPrecision(Float value, int precision) {
		Double d =  ((Float) value).doubleValue();
		d = d * Math.pow(10, precision);
		d = (double) Math.round(d)/Math.pow(10, precision);
		return d.floatValue();
	}

	/**
	 * Set the value for every Evcs.ChannelId.
	 * 
	 * @param json Given raw data in JSON
	 */
	private void setChargeControls(JsonElement jsonResponse) {
		
		try {
			JsonArray jarr = JsonUtils.getAsJsonArray(jsonResponse, Consts.Endpoint.CHARGE_CONTROLS.toString());
			JsonElement json = jarr.getAsJsonArray().get(this.chargeControlIndex);
			
			for (HardyBarth.ChannelId channelId : HardyBarth.ChannelId.values()) {
				String[] jsonPaths = channelId.getJsonPaths();
				if(Consts.Endpoint.CHARGE_CONTROL.toString().compareTo(jsonPaths[0]) != 0) {
					continue;
				}
				jsonPaths = new String[] { jsonPaths[1] };
				Object value = this.getValueFromJson(channelId, json, channelId.converter, jsonPaths);
				if(value != null) {
					value = this.adoptChannel(channelId, value);
					this.parent.channel(channelId).setNextValue(value);
				}
			}

			
		} catch (OpenemsNamedException e) {
			this.parent.logError(this.parent.log, "Hardy Barth: Unable to get ChargeControl param" + e.getMessage());
		}
		
	}

	/**
	 * Call the getValueFromJson without a divergent type in the raw json.
	 * 
	 * @param channelId Channel that value will be detect.
	 * @param json      Raw JsonElement.
	 * @param converter Converter, to convert the raw JSON value into a proper
	 *                  Channel.
	 * @param jsonPaths Whole JSON path, where the JsonElement for the given channel
	 *                  is located.
	 * @return Value of the last JsonElement by running through the specified JSON
	 *         path.
	 */
	private Object getValueFromJson(ChannelId channelId, JsonElement json, Function<Object, Object> converter,
			String... jsonPaths) {
		return this.getValueFromJson(channelId, null, json, converter, jsonPaths);
	}

	/**
	 * Get the last JSON element and it's value, by running through the given
	 * jsonPath.
	 * 
	 * @param channelId              Channel that value will be detect.
	 * @param divergentTypeInRawJson Divergent type of the value in the depending
	 *                               JsonElement.
	 * @param json                   Raw JsonElement.
	 * @param converter              Converter, to convert the raw JSON value into a
	 *                               proper Channel.
	 * @param jsonPaths              Whole JSON path, where the JsonElement for the
	 *                               given channel is located.
	 * @return Value of the last JsonElement by running through the specified JSON
	 *         path.
	 */
	private Object getValueFromJson(ChannelId channelId, OpenemsType divergentTypeInRawJson, JsonElement json,
			Function<Object, Object> converter, String... jsonPaths) {

		JsonElement currentJsonElement = json;
		// Go through the whole jsonPath of the current channelId
		for (int i = 0; i < jsonPaths.length; i++) {
			String currentPathMember = jsonPaths[i];
			// System.out.println(currentPathMember);
			try {
				if (i != jsonPaths.length - 1) {
					// Not last path element
					currentJsonElement = JsonUtils.getAsJsonObject(currentJsonElement, currentPathMember);
				} else {
					//
					OpenemsType openemsType = divergentTypeInRawJson == null ? channelId.doc().getType()
							: divergentTypeInRawJson;

					// Last path element
					Object value = this.getJsonElementValue(currentJsonElement, openemsType, jsonPaths[i]);

					// Apply value converter
					value = converter.apply(value);

					// Return the converted value
					return value;
				}
			} catch (OpenemsNamedException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Get Value of the given JsonElement in the required type.
	 * 
	 * @param jsonElement Element as JSON.
	 * @param openemsType Required type.
	 * @param memberName  Member name of the JSON Element.
	 * @return Value in the required type.
	 * @throws OpenemsNamedException Failed to get the value.
	 */
	private Object getJsonElementValue(JsonElement jsonElement, OpenemsType openemsType, String memberName)
			throws OpenemsNamedException {
		final Object value;

		switch (openemsType) {
		case BOOLEAN:
			value = JsonUtils.getAsBoolean(jsonElement, memberName);
			break;
		case DOUBLE:
			value = JsonUtils.getAsDouble(jsonElement, memberName);
			break;
		case FLOAT:
			value = JsonUtils.getAsFloat(jsonElement, memberName);
			break;
		case INTEGER:
			value = JsonUtils.getAsInt(jsonElement, memberName);
			break;
		case LONG:
			value = JsonUtils.getAsLong(jsonElement, memberName);
			break;
		case SHORT:
			value = JsonUtils.getAsShort(jsonElement, memberName);
			break;
		case STRING:
			value = JsonUtils.getAsString(jsonElement, memberName);
			break;
		default:
			value = JsonUtils.getAsString(jsonElement, memberName);
			break;
		}
		return value;
	}
}
