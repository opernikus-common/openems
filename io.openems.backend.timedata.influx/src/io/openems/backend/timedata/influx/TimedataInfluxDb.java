package io.openems.backend.timedata.influx;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.debugcycle.DebugLoggable;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.oemprovider.OemProvider;
import io.openems.backend.common.oemprovider.WhiteList;
import io.openems.backend.common.timedata.Timedata;
import io.openems.backend.common.timedata.TimedataEdgeFilter;
import io.openems.common.event.EventReader;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.AbstractDataNotification;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.oem.OpenemsBackendOem;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.shared.influxdb.InfluxConnector;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Timedata.InfluxDB", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"service.ranking:Integer=1" // oEMS ranking order (highest first)
		}, //
		immediate = true //
)
@EventTopics({ //
		Edge.Events.ON_SET_ONLINE //
})
public class TimedataInfluxDb extends AbstractOpenemsBackendComponent implements Timedata, EventHandler, DebugLoggable {

	private final Logger log = LoggerFactory.getLogger(TimedataInfluxDb.class);
	private final FieldTypeConflictHandler fieldTypeConflictHandler;

	@Reference
	private OpenemsBackendOem oem;

	@Reference
	protected volatile Metadata metadata;

	@Reference
	protected volatile OemProvider oemProvider; // oEMS

	private Config config;
	private InfluxConnector influxConnector = null;
	private TimedataEdgeFilter filter; // oEMS
	private WhiteList whitelist; // oEMS
	// private TimeFilter timeFilter; // oEMS

	// edgeId, channelIds which are timestamped channels
	private final Multimap<Integer, String> timestampedChannelsForEdge = HashMultimap.create();

	public TimedataInfluxDb() {
		super("Timedata.InfluxDB");
		this.fieldTypeConflictHandler = new FieldTypeConflictHandler(this);
	}

	@Activate
	private void activate(Config config) throws OpenemsException, IllegalArgumentException {
		this.config = config;
		// this.timeFilter = TimeFilter.from(config.startDate(), config.endDate()); //oEMS

		this.filter = new TimedataEdgeFilter(this.config.id(), this.config.edgeFilter(), this.config.verbose()); // oEMS edgeFilter

		this.logInfo(this.log, "Activate [" //
				+ "url=" + config.url() + ";"//
				+ "bucket=" + config.bucket() + ";"//
				+ "apiKey=" + (config.apiKey() != null ? "ok" : "NOT_SET") + ";"//
				+ "measurement=" + config.measurement() //
				+ (config.isReadOnly() ? ";READ_ONLY_MODE" : "") //
				+ "]");

		this.whitelist = this.oemProvider.getWhitelist(this.config.id(), this.config.channelWhitelist());

		this.influxConnector = new InfluxConnector(config.id(), config.queryLanguage(), URI.create(config.url()),
				config.org(), config.apiKey(), config.bucket(), this.oem.getInfluxdbTag(), config.isReadOnly(),
				config.poolSize(), config.maxQueueSize(), //
				(e) -> {
					this.fieldTypeConflictHandler.handleException(e);
				});
	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");
		if (this.influxConnector != null) {
			this.influxConnector.deactivate();
		}
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case Edge.Events.ON_SET_ONLINE:
			final var reader = new EventReader(event);
			final var edgeId = reader.getString(Edge.Events.OnSetOnline.EDGE_ID);
			final var isOnline = reader.getBoolean(Edge.Events.OnSetOnline.IS_ONLINE);
			if (!isOnline) {
				try {
					var influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);
					this.timestampedChannelsForEdge.removeAll(influxEdgeId);
				} catch (OpenemsException e) {
					e.printStackTrace();
				}
			}
			break;
		}
	}

	@Override
	public void write(String edgeId, TimestampedDataNotification notification) {
		if (this.config.isReadOnly()) {
			return;
		}
		// oEMS Start
		// parse the numeric EdgeId
		try {
			int influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);
			if (!this.filter._isEdgeIdApplicable(String.valueOf(influxEdgeId))) {
				return;
			}

			// Write data to default location
			this.writeData(//
					influxEdgeId, //
					notification, //
					channel -> {
						this.timestampedChannelsForEdge.put(influxEdgeId, channel);
						return true;
					});
			// oEMS end
		} catch (OpenemsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void write(String edgeId, AggregatedDataNotification notification) {
		if (this.config.isReadOnly()) {
			return;
		}
		// oEMS Start
		try {
			int influxEdgeId = InfluxConnector.parseNumberFromName(edgeId);
			if (!this.filter._isEdgeIdApplicable(String.valueOf(influxEdgeId))) {
				return;
			}

			// Write data to default location
			this.writeData(//
					influxEdgeId, //
					notification, //
					channel -> !this.isTimestampedChannel(influxEdgeId, channel));
		} catch (OpenemsException e) {
			e.printStackTrace();
		}
		// oEMS End
	}

	@Override
	public void write(String edgeId, ResendDataNotification data) {
		// TODO Auto-generated method stub
	}

	private boolean isTimestampedChannel(int edgeId, String channel) {
		final var channelSet = this.timestampedChannelsForEdge.get(edgeId);
		// if edge is not set the checked channel may be timestamped channel so
		// initially return true
		if (channelSet == null) {
			return true;
		}
		return channelSet.contains(channel);
	}

	/**
	 * Actually writes the data to InfluxDB.
	 *
	 * @param influxEdgeId     the unique, numeric identifier of the Edge
	 * @param notification     the {@link AbstractDataNotification}
	 * @param shouldWriteValue the function which determines if the value should be
	 *                         written
	 * @throws OpenemsException on error
	 */
	private void writeData(//
			int influxEdgeId, //
			AbstractDataNotification notification, //
			Function<String, Boolean> shouldWriteValue //
	) {
		final var data = notification.getData();
		var dataEntries = data.rowMap().entrySet();
		if (dataEntries.isEmpty()) {
			// no data to write
			return;
		}

		for (var dataEntry : dataEntries) {
			var channelEntries = dataEntry.getValue().entrySet();
			if (channelEntries.isEmpty()) {
				// no points to add
				continue;
			}

			var timestamp = dataEntry.getKey();
			// oEMS no timeFilter

			/*
			 * if (!this.timeFilter.isValid(timestamp)) { // timestamp is not within the
			 * TimeFilter continue; }
			 */

			// this builds an InfluxDB record ("point") for a given timestamp
			var point = Point //
					.measurement(this.config.measurement()) //
					.addTag(this.oem.getInfluxdbTag(), String.valueOf(influxEdgeId)) //
					.time(timestamp, WritePrecision.MS);
			for (var channelEntry : channelEntries) {
				if (!shouldWriteValue.apply(channelEntry.getKey())) {
					continue;
				}
				this.addValue(//
						point, //
						channelEntry.getKey(), //
						channelEntry.getValue());
			}
			// oems begin tbr
			if (this.config.debugWrite()) {
				this.log.info("Influx Point written " + point.toLineProtocol());
			}
			// oems end tbr
			this.influxConnector.write(point);
		}
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		// oEMS no time Filter
		// parse the numeric EdgeId
		Optional<Integer> influxEdgeId = Optional.of(InfluxConnector.parseNumberFromName(edgeId));

		return this.influxConnector.queryHistoricData(influxEdgeId, fromDate, toDate, channels, resolution,
				this.config.measurement());
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		// oEMS no time Filter
		// parse the numeric EdgeId
		Optional<Integer> influxEdgeId = Optional.of(InfluxConnector.parseNumberFromName(edgeId));
		return this.influxConnector.queryHistoricEnergy(influxEdgeId, fromDate, toDate, channels,
				this.config.measurement());
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		// oEMS no time Filter
		// parse the numeric EdgeId
		Optional<Integer> influxEdgeId = Optional.of(InfluxConnector.parseNumberFromName(edgeId));
		return this.influxConnector.queryHistoricEnergyPerPeriod(influxEdgeId, fromDate, toDate, channels, resolution,
				this.config.measurement());
	}

	/**
	 * Adds the value in the correct data format for InfluxDB.
	 *
	 * @param builder the Influx PointBuilder
	 * @param field   the field name
	 * @param element the value
	 */
	private void addValue(Point builder, String field, JsonElement element) {
		if (element == null || element.isJsonNull() //
				// || !isAllowed(field) // oEMS Channel-Address is not allowed/blacklisted
				|| this.whitelist.isChannelBlacklisted(field) //
				// already handled by special case handling
				|| this.specialCaseFieldHandling(builder, field, element)) {
			return;
		}

		if (!element.isJsonPrimitive()) {
			// Non-Primitives are ignored
			this.logWarn(this.log, "Ignoring non-primitive Field [" + field + "] Value [" + element + "]");
			return;
		}

		final var p = (JsonPrimitive) element;
		if (p.isNumber()) {
			// Numbers can be directly converted
			final var n = p.getAsNumber();
			if (n.getClass().getName().equals("com.google.gson.internal.LazilyParsedNumber")) {
				// Avoid 'discouraged access'
				// LazilyParsedNumber stores value internally as String
				final var s = n.toString();

				final var longValue = Longs.tryParse(s);
				if (longValue != null) {
					builder.addField(field, longValue);
					return;
				}

				final var doubleValue = Doubles.tryParse(s);
				if (doubleValue != null) {
					builder.addField(field, doubleValue);
					return;
				}

				// unable to parse lazy number
				return;
			}

			if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte) {
				builder.addField(field, n.longValue());
				return;
			}

			builder.addField(field, n.doubleValue());
			return;
		}

		if (p.isBoolean()) {
			// Booleans are converted to integer (0/1)
			builder.addField(field, p.getAsBoolean());
			return;
		}

		if (p.isString()) {
			// Strings are parsed if they start with a number or minus
			final var s = p.getAsString();

			// try to save string value as numbers
			final var longValue = Longs.tryParse(s);
			if (longValue != null) {
				builder.addField(field, longValue);
				return;
			}

			final var doubleValue = Doubles.tryParse(s);
			if (doubleValue != null) {
				builder.addField(field, doubleValue);
				return;
			}

			// string not can not be parsed to any number => currently not saved
		}
	}

	/**
	 * Handles some special cases for fields.
	 *
	 * <p>
	 * E.g. to avoid errors like "field type conflict: input field XYZ on
	 * measurement "data" is type integer, already exists as type string"
	 *
	 * @param builder the InfluxDB Builder
	 * @param field   the fieldName, i.e. the ChannelAddress
	 * @param value   the value, guaranteed to be not-null and not JsonNull.
	 * @return true if field was handled; false otherwise
	 */
	private boolean specialCaseFieldHandling(Point builder, String field, JsonElement value) {
		var handler = this.fieldTypeConflictHandler.getHandler(field);
		if (handler == null) {
			// no special handling exists for this field
			return false;
		}
		try {
			// call special handler
			handler.accept(builder, value);
		} catch (RuntimeException e) {
			this.logError(this.log,
					"Unexpected error in special case field handler for Field [" + field + "] Value [" + value + "]");
		}
		return true;
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	public String id() {
		return this.config.id();
	}

	@Override
	public String debugLog() {
		return "[" + this.getName() + "] " + this.config.id() + " " + this.influxConnector.debugLog();
	}

	@Override
	public Map<String, JsonElement> debugMetrics() {
		return null;
	}

}
