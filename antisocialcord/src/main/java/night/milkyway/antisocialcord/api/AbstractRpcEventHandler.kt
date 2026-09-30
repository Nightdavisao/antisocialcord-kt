package night.milkyway.antisocialcord.api

import night.milkyway.antisocialcord.model.ActivityArguments
import night.milkyway.antisocialcord.model.RpcEvent
import night.milkyway.antisocialcord.model.RpcPayload

/**
 * Abstract RPC event handler. Should be implemented by event consumers
 */
abstract class AbstractRpcEventHandler {
    /**
     * Emitted when the connection is ready (often as soon as Discord sends the DISPATCH frame)
     */
    open fun onReady(payload: RpcPayload) {}
    /**
     * Emitted when Discord sends the first frame after connecting to the IPC service
     */
    open fun onDispatch(payload: RpcPayload) {}

    /**
     * @param arguments Activity arguments
     * @param event Event type (might be null on most cases)
     */
    open fun onSetActivity(arguments: ActivityArguments?, event: RpcEvent?, nonce: String?) {}

    /**
     * Emitted when the RPC client has successfully connected to Discord
     */
    open fun onConnected() {}
    /**
     * Emitted when the RPC client has disconnected from Discord
     */
    open fun onDisconnected() {}
}