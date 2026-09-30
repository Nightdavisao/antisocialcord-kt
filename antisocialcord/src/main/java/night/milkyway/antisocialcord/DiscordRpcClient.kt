package night.milkyway.antisocialcord

import android.content.Context
import night.milkyway.antisocialcord.api.AbstractRpcEventHandler
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

/**
 * The RPC/IPC client for communicating with the Discord app.
 *
 * @param context Android application context
 * @param lruNonceSize LRU size for temporarily storing nonces for function callbacks
 * @param channelCapacity Channel capacity (used for queuing frames in case immediately sending them fails)
 * @param packageWhitelist Package name whitelist (by default, it should cover the official Discord package and some client mods)
 */
class DiscordRpcClient(
    context: Context,
    lruNonceSize: Int = 100,
    channelCapacity: Int = 50,
    packageWhitelist: List<String> = SocialSdkConsts.DISCORD_PACKAGES
) {
    private val lruNonce = LRUCache<Callback>(lruNonceSize)

    private class NonceCallbackListener(val lruNonce: LRUCache<Callback>) : AbstractRpcEventHandler() {
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

    private val listeners = mutableListOf<AbstractRpcEventHandler>()

    private val client = RpcInternalClient(context, listeners, packageWhitelist, channelCapacity = channelCapacity)

    init {
        this.addListener(NonceCallbackListener(lruNonce))
    }

    /**
     * Whether the connection is ready
     */
    val isConnectionReady: Boolean
        get() = client.isConnectionReady

    /**
     * Adds a listener to the listeners list
     * @param listener Listener handler
     */
    fun addListener(listener: AbstractRpcEventHandler) {
        listeners.add(listener)
    }

    /**
     * Removes a listener from the listeners list
     * @param listener Listener handler
     */
    fun removeListener(listener: AbstractRpcEventHandler) {
        listeners.remove(listener)
    }

    /**
     * Connects to the Discord client via the Android binder interface (effectively binding the application context to the IPC service)
     * @param applicationId Application ID (you should be able to create one from the Discord's Developer Portal)
     */
    @Throws(GenericSdkException::class)
    fun connect(applicationId: Long) {
        client.connect(applicationId)
    }

    /**
     * Shorthand for the `connect(applicationId: Long)` function.
     *
     * @param applicationId Application ID (you should be able to create one from the Discord's Developer Portal)
     */
    @Throws(GenericSdkException::class)
    fun connect(applicationId: String) {
        this.connect(applicationId.toLong())
    }

    /**
     * Disconnects the application context from Discord, effectively unbinding it from the IPC service
     */
    fun disconnect() {
        client.disconnect()
    }

    /**
     * Set an activity on the user's status.
     * @param activity Discord activity (passing a null value will clear the activity)
     * @param callback Callback for when the request has been successful
     * @see clearActivity
     */
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

    /**
     * Shorthand for `setActivity(null)`.
     * @see setActivity
     */
    fun clearActivity() = setActivity(null)
}