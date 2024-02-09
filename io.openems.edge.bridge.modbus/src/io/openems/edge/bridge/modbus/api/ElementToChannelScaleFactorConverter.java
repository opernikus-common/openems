package io.openems.edge.bridge.modbus.api;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;

/**
 * Converts between Element and Channel by applying a scale factor.
 *
 * <p>
 * (channel = element * 10^scaleFactor)
 *
 * <p>
 * Example: if the Register is in unit [0.1 V] and this converter has a
 * scaleFactor of '2', it converts to unit [1 mV]
 */
public class ElementToChannelScaleFactorConverter extends ElementToChannelConverter {

	private static int getValueOrError(OpenemsComponent component, ChannelId channelId)
			throws InvalidValueException, IllegalArgumentException {
		var channel = (IntegerReadChannel) component.channel(channelId);
		var value = channel.getNextValue().orElse(null);
		if (value != null) {
			return value;
		}
		return channel.value().getOrError();
	}

	public static class ElementDataQueue {
		private Float valT2 = null;
		private Float valT1 = null;
		private Integer sfT2 = null;
		private Integer sfT1 = null;
		private int tOffset = 0;
	}

	/**
	 * use this for sunspec devices with scalefactors which changes at runtime.
	 * 
	 * <p>
	 * Note that it will delay channel output by 2 core cycle times and that it will
	 * set the channel value to null during a scale factor change
	 * 
	 * @param component          the component
	 * @param point              the point
	 * @param scaleFactorChannel the scaleFactorChannel
	 * @param histData           the ElementDataQueue to store temporary values
	 */
	public ElementToChannelScaleFactorConverter(OpenemsComponent component, SunSpecPoint point,
			ChannelId scaleFactorChannel, ElementDataQueue histData) {
		super(//

				// element -> channel
				value -> {
					if (!point.isDefined(value)) {
						return null;
					}
					try {
						Integer sf = ((IntegerReadChannel) component.channel(scaleFactorChannel)).value().getOrError()
								* -1;
						if (sf != histData.sfT1) {
							/*
							 * <p> Note that t[0] represends the critical point in time (when sf!=sfPrev)
							 * 
							 * <p> t[-2]: valT2, sfT2 <- value and sf are in sync
							 * 
							 * <p> t[-1]: valT1, sfT1 <- possible inconsistency
							 * 
							 * <p> t[ 0]: value, sf <- possible inconsistency
							 * 
							 * <p> t[ 1]: valT+1, sf+1 <- possible inconsistency
							 * 
							 * <p> t[ 2]: vatT+2, sf+2 <- value and sf are in sync
							 */
							histData.sfT1 = sf;
							histData.valT1 = null;
							histData.tOffset = 2;
							if (histData.sfT2 != null) {
								return apply(histData.valT2, histData.sfT2);
							}
							return null;
						}
						Object ret = null;
						Float val = TypeUtils.getAsType(OpenemsType.FLOAT, value);
						if (histData.tOffset <= 0) {
							if (histData.sfT2 != null) {
								// always return t[-2]
								ret = apply(histData.valT2, histData.sfT2);
							}
						} else {
							// t[+1] or t [+2] - ignore values but still update them
							histData.tOffset--;
						}
						histData.valT2 = histData.valT1;
						histData.valT1 = val;
						histData.sfT2 = histData.sfT1;
						histData.sfT1 = sf;
						return ret;

					} catch (InvalidValueException | IllegalArgumentException e) {
						return null;
					}
				}, //

				// channel -> element
				value -> {
					try {
						return apply(value,
								((IntegerReadChannel) component.channel(scaleFactorChannel)).value().getOrError());
					} catch (InvalidValueException | IllegalArgumentException e) {
						return null;
					}
				});
	}

	public ElementToChannelScaleFactorConverter(OpenemsComponent component, SunSpecPoint point,
			ChannelId scaleFactorChannel) {
		super(//
				// element -> channel
				value -> {
					if (!point.isDefined(value)) {
						return null;
					}
					try {
						return apply(value, getValueOrError(component, scaleFactorChannel) * -1);
					} catch (InvalidValueException | IllegalArgumentException e) {
						return null;
					}
				}, //

				// channel -> element
				value -> {
					try {
						return apply(value, getValueOrError(component, scaleFactorChannel));
					} catch (InvalidValueException | IllegalArgumentException e) {
						return null;
					}
				});
	}

	public ElementToChannelScaleFactorConverter(int scaleFactor) {
		super(//
				// element -> channel
				value -> apply(value, scaleFactor * -1), //

				// channel -> element
				value -> apply(value, scaleFactor));
	}

	private static Object apply(Object value, int scaleFactor) {
		var factor = Math.pow(10, scaleFactor * -1);
		if (value == null) {
			return null;
		}
		if (value instanceof Boolean) {
			return (boolean) value;
		}
		if (value instanceof Short) {
			var result = (Short) value * factor;
			if (result >= Short.MIN_VALUE && result <= Short.MAX_VALUE) {
				return Short.valueOf((short) result);
			}
			if (result > Integer.MIN_VALUE && result < Integer.MAX_VALUE) {
				return Integer.valueOf((int) result);
			} else {
				return Double.valueOf(Math.round(result));
			}
		}
		if (value instanceof Integer) {
			var result = (Integer) value * factor;
			if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
				return Integer.valueOf((int) result);
			}
			return Double.valueOf(Math.round(result));
		}
		if (value instanceof Long) {
			var result = (Long) value * factor;
			return Math.round(result);
		}
		if (value instanceof Float) {
			var result = (Float) value * factor;
			if (result >= Float.MIN_VALUE && result <= Float.MAX_VALUE) {
				return Float.valueOf((float) result);
			}
			return Double.valueOf(result);
		}
		if (value instanceof Double) {
			return Double.valueOf((Double) value * factor);
		}
		if (value instanceof String) {
			return value;
		}
		throw new IllegalArgumentException(
				"Type [" + value.getClass().getName() + "] not supported by SCALE_FACTOR converter");
	}
}
