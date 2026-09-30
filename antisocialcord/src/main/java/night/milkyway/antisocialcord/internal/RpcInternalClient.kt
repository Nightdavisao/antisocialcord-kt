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

open class RpcInternalClient(
    private val context: Context,
    private val listeners: List<AbstractRpcEventHandler>?,
    private val discordPackages: List<String> = SocialSdkConsts.DISCORD_PACKAGES
) : AbstractRpcEventHandler() {
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
    private val isConnectionReady: Boolean
        get() {
            return rpcConnection != null && isBound && connectionState == RpcStateConnection.Connected && readyFlag
        }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
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
            this@RpcInternalClient.let {
                Log.d(TAG, "CLOSE_ABNORMAL, service disconnected")
                onDisconnected()
            }
        }
    }
    private val callback: IDiscordRpcCallback = object : IDiscordRpcCallback.Stub() {
        override fun onFrame(str: String?) {
            if (str == null) {
                return
            }
            try {
                Log.d(TAG, "onFrame: $str")
                val frame = json.decodeFromString(RpcPayload.serializer(), str)
                Log.d(TAG, "onFrame: $frame")

                when (frame.command) {
                    RpcCommand.DISPATCH -> onDispatch(frame)
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
            } catch (e: RemoteException) {
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
        val frame = json.encodeToString(framePayload)
        this.sendFrame(frame)
    }

    private fun resolveServiceIntent(): Intent? {
        discordPackages.forEach {
            val intent = Intent("com.discord.socialsdk.rpc.IDiscordRpcService").apply {
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

    override fun onDisconnected() {
        rpcService = null
        rpcConnection = null
        readyFlag = false
        connectionState = RpcStateConnection.Disconnected
        listeners?.forEach { it.onDisconnected() }
    }
}

enum class RpcStateConnection {
    Connected,
    Disconnected
}