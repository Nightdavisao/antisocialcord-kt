package night.milkyway.antisocialcord.api

import night.milkyway.antisocialcord.model.Activity
import night.milkyway.antisocialcord.model.ActivityArguments
import night.milkyway.antisocialcord.model.RpcEvent
import night.milkyway.antisocialcord.model.RpcPayload

abstract class AbstractRpcHandler {
    open fun onReady(payload: RpcPayload) {}
    open fun onDispatch(payload: RpcPayload) {}
    open fun onSetActivity(arguments: ActivityArguments?, event: RpcEvent?, nonce: String?) {}
    open fun onConnected() {}
    open fun onDisconnected() {}
}