import { JsonrpcNotification } from "../base";

// oEMS EdgeOnlineNotification == When Edge goes offline -> backend sends this Notification

/**
 * Represents a JSON-RPC Notification for sending the current data of all
 * subscribed Channels.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "edgeOnline",
 *   "params": {
 *    isOnline: boolean
 *   }
 * }
 * </pre>
 */
export class EdgeOnlineNotification extends JsonrpcNotification {

    public static readonly METHOD: string = "edgeOnline";

    public constructor(
        public override readonly params: {
            isOnline: boolean
        },
    ) {
        super(EdgeOnlineNotification.METHOD, params);
    }

}
