package night.milkyway.antisocialcord.internal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.discord.socialsdk.rpc.IDiscordRpcCallback
import com.discord.socialsdk.rpc.IDiscordRpcConnection
import com.discord.socialsdk.rpc.IDiscordRpcService
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import night.milkyway.antisocialcord.api.AbstractRpcEventHandler
import night.milkyway.antisocialcord.internal.utils.SocialSdkConsts
import night.milkyway.antisocialcord.model.ActivityArguments
import night.milkyway.antisocialcord.model.RpcCommand
import night.milkyway.antisocialcord.model.RpcEvent
import night.milkyway.antisocialcord.model.RpcPayload
import night.milkyway.antisocialcord.model.exception.GenericSdkException

internal open class RpcInternalClient(
    private val context: Context,
    private val listeners: List<AbstractRpcEventHandler>?,
    private val discordPackages: List<String> = SocialSdkConsts.DISCORD_PACKAGES,
    channelCapacity: Int = 50,
) : AbstractRpcEventHandler() {
    private enum class RpcStateConnection {
        Connected,
        Disconnected
    }

    companion object {
        private const val TAG = "DiscordSocialSdk"

        @OptIn(ExperimentalSerializationApi::class)
        private val json = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            classDiscriminatorMode = ClassDiscriminatorMode.NONE
        }
    }

    private var connectionState: RpcStateConnection = RpcStateConnection.Connected
    private var readyFlag: Boolean = false
    val isConnectionReady: Boolean
        get() {
            return rpcConnection != null && isBound && connectionState == RpcStateConnection.Connected && readyFlag
        }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val channel = Channel<RpcPayload>(channelCapacity)
    private var isBound: Boolean = false
    private var pendingApplicationId: Long? = null
    private var rpcService: IDiscordRpcService? = null
    private var rpcConnection: IDiscordRpcConnection? = null


    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            this@RpcInternalClient.apply {
                rpcService = IDiscordRpcService.Stub.asInterface(service)
                val applicationId = pendingApplicationId

                if (applicationId != null) {
                    rpcConnection =
                        rpcService?.connect(
                            applicationId,
                            SocialSdkConsts.RPC_VERSION,
                            callback
                        )
                    onConnected()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            onDisconnected(SocialSdkConsts.CLOSE_ABNORMAL)
        }
    }
    private val callback: IDiscordRpcCallback = object : IDiscordRpcCallback.Stub() {
        override fun onFrame(str: String?) {
            if (str == null) {
                return
            }
            try {
                val frame = json.decodeFromString(RpcPayload.serializer(), str)

                when (frame.command) {
                    RpcCommand.DISPATCH -> onDispatch(frame) // this returns a mocked user, discord doesn't return the actual user
                    RpcCommand.SET_ACTIVITY -> onSetActivity(
                        frame.args as ActivityArguments?, frame.event, frame.nonce
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "onFrame: $e")
            }
        }

        override fun onClose(i: Int, str: String?) {
            disconnect()
        }
    }

    fun connect(applicationId: Long) = scope.launch {
        mutex.withLock {
            pendingApplicationId = applicationId
            val serviceResolution = resolveServiceIntent() ?: throw GenericSdkException(
                "Either the Discord app is not installed, or you forgot to add the intent-filter to AndroidManifest.xml",
                SocialSdkConsts.INVALID_CLIENT_ID
            )

            isBound =
                context.bindService(serviceResolution, this@RpcInternalClient.serviceConnection, 1)
            if (!isBound) {
                throw GenericSdkException(
                    "isBound returned false (when trying to bind the application context to the service)",
                    SocialSdkConsts.CLOSE_ABNORMAL
                )
            }

            connectionState = RpcStateConnection.Connected
        }
    }

    fun disconnect() = scope.launch {
        mutex.withLock {
            try {
                pendingApplicationId = null
                rpcConnection?.disconnect()
                context.unbindService(serviceConnection)
                onDisconnected()
            } catch (_: RemoteException) {
                // noop
            }
            connectionState = RpcStateConnection.Disconnected
        }
    }

    private fun sendFrame(str: String) = scope.launch {
        if (!isConnectionReady) return@launch
        rpcConnection?.sendFrame(str)
    }

    fun sendSerializedFrame(framePayload: RpcPayload) {
        try {
            val frame = json.encodeToString(framePayload)
            this.sendFrame(frame)
        } catch (_: Exception) {
            if (connectionState != RpcStateConnection.Connected) return

            scope.launch {
                channel.send(framePayload)
            }
        }
    }

    private fun resolveServiceIntent(): Intent? {
        discordPackages.forEach {
            val intent = Intent(SocialSdkConsts.INTENT_ACTION).apply {
                setPackage(it)
            }
            if (context.packageManager.resolveService(intent, 0) != null) {
                return intent
            }
        }
        return null
    }

    override fun onReady(payload: RpcPayload) {
        readyFlag = true
        listeners?.forEach { it.onReady(payload) }

        // i don't know about rate limits
        scope.launch {
            var receivedFrame = channel.tryReceive().getOrNull()

            while (receivedFrame != null) {
                sendSerializedFrame(receivedFrame)
                receivedFrame = channel.tryReceive().getOrNull()
            }
        }
    }

    override fun onDispatch(payload: RpcPayload) {
        if (payload.event == RpcEvent.READY) {
            onReady(payload)
        }
    }

    override fun onSetActivity(arguments: ActivityArguments?, event: RpcEvent?, nonce: String?) {
        listeners?.forEach { it.onSetActivity(arguments, event, nonce) }
    }

    override fun onConnected() {
        connectionState = RpcStateConnection.Connected
        listeners?.forEach { it.onConnected() }
    }

    override fun onDisconnected(code: Int?) {
        rpcService = null
        rpcConnection = null
        readyFlag = false
        connectionState = RpcStateConnection.Disconnected
        listeners?.forEach { it.onDisconnected() }
    }
}

