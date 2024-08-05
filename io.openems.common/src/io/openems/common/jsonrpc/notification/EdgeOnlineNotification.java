package io.openems.common.jsonrpc.notification;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.GenericJsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.utils.JsonUtils;

// oEMS This Notifies the UI if an Edge goes offline.
/**
 * Represents a JSON-RPC Notification for OpenEMS Edge OnlineNotification.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "edgeOnline",
 *   "params": {
 *     isOnline: boolean
 *   }
 * }
 * </pre>
 */
public class EdgeOnlineNotification extends JsonrpcNotification {

    /**
     * Parses a {@link JsonObject} to a {@link EdgeOnlineNotification}.
     *
     * @param j the {@link JsonObject}
     * @return the {@link EdgeOnlineNotification}
     * @throws OpenemsError.OpenemsNamedException on error
     */
    public static EdgeOnlineNotification from(JsonObject j) throws OpenemsError.OpenemsNamedException {
        return EdgeOnlineNotification.from(GenericJsonrpcNotification.from(j));
    }

    /**
     * Parses a {@link JsonrpcNotification} to a {@link EdgeOnlineNotification}.
     *
     * @param n the {@link JsonrpcNotification}
     * @return the {@link EdgeOnlineNotification}
     * @throws OpenemsError.OpenemsNamedException on error
     */
    public static EdgeOnlineNotification from(JsonrpcNotification n) throws OpenemsError.OpenemsNamedException {
        var obj = (n.getParams());
        return new EdgeOnlineNotification(obj.get("isOnline").getAsBoolean());
    }

    public static final String METHOD = "edgeOnline";
    private final boolean isOnline;

    public EdgeOnlineNotification(boolean isOnline) {
        super(EdgeOnlineNotification.METHOD);
        this.isOnline = isOnline;
    }

    @Override
    public JsonObject getParams() {
        return JsonUtils.buildJsonObject() //
                .addProperty("isOnline", this.isOnline) //
                .build();
    }

}
