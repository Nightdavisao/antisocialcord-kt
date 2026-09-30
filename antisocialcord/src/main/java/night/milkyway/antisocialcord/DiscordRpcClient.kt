package night.milkyway.antisocialcord

import android.content.Context
import night.milkyway.antisocialcord.api.AbstractRpcHandler
import night.milkyway.antisocialcord.internal.utils.LRUCache
import night.milkyway.antisocialcord.internal.RpcInternalClient
import night.milkyway.antisocialcord.internal.utils.SocialSdkConsts
import night.milkyway.antisocialcord.model.Activity
import night.milkyway.antisocialcord.model.ActivityArguments
import night.milkyway.antisocialcord.model.RpcCommand
import night.milkyway.antisocialcord.model.RpcEvent
import night.milkyway.antisocialcord.model.RpcPayload
import night.milkyway.antisocialcord.model.exception.GenericSdkException
import java.util.UUID

typealias Callback = (() -> Unit)

class DiscordRpcClient(
    context: Context,
    lruNonceSize: Int = 100,
    discordPackages: List<String> = SocialSdkConsts.DISCORD_PACKAGES
) {
    private val lruNonce = LRUCache<Callback>(lruNonceSize)

    private class NonceCallbackListener(val lruNonce: LRUCache<Callback>) : AbstractRpcHandler() {
        override fun onSetActivity(
            arguments: ActivityArguments?,
            event: RpcEvent?,
            nonce: String?
        ) {
            nonce?.let {
                val cb = lruNonce.get(it)
                lruNonce.delete(it)
                cb?.invoke()
            }
        }
    }

    private val listeners = mutableListOf<AbstractRpcHandler>()

    private val client = RpcInternalClient(context, listeners, discordPackages)

    init {
        this.addListener(NonceCallbackListener(lruNonce))
    }

    fun addListener(listener: AbstractRpcHandler) {
        listeners.add(listener)
    }

    fun removeListener(listener: AbstractRpcHandler) {
        listeners.remove(listener)
    }

    @Throws(GenericSdkException::class)
    fun connect(applicationId: Long) {
        client.connect(applicationId)
    }

    @Throws(GenericSdkException::class)
    fun connect(applicationId: String) {
        this.connect(applicationId.toLong())
    }

    fun disconnect() {
        client.disconnect()
    }

    fun setActivity(activity: Activity?, callback: Callback? = null) {
        val nonce = if (callback != null) {
            UUID.randomUUID().toString()
        } else {
            null
        }

        if (nonce != null && callback != null) {
            lruNonce.put(nonce, callback)
        }

        client.sendSerializedFrame(
            RpcPayload(
                command = RpcCommand.SET_ACTIVITY,
                nonce = nonce,
                event = null,
                args = activity?.let {
                    ActivityArguments(
                        activity = it
                    )
                }
            )
        )
    }

    fun clearActivity() = setActivity(null)
}