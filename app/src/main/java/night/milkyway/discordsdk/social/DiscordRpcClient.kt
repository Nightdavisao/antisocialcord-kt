package night.milkyway.discordsdk.social

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.discord.socialsdk.rpc.IDiscordRpcCallback
import com.discord.socialsdk.rpc.IDiscordRpcConnection
import com.discord.socialsdk.rpc.IDiscordRpcService
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DiscordRpcClient(
    private val context: Context,
    private val activity: Activity
) {
    companion object {
        private val DISCORD_PACKAGES = listOf("com.discord")
        const val CLOSE_ABNORMAL = 1006
        const val INVALID_CLIENTID = 4000
        const val RPC_VERSION = "1"
        private const val TAG = "DiscordSocialSdk"
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private var isBound: Boolean = false
    private var pendingApplicationId: Long? = null
    private var service: IDiscordRpcService? = null
    private var rpcConnection: IDiscordRpcConnection? = null
    private val aids: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            this@DiscordRpcClient.let {
                it.service = IDiscordRpcService.Stub.asInterface(service)
                it.onConnected()
                try {
                    it.apply {
                        val applicationId = it.pendingApplicationId

                        if (applicationId != null) {
                            rpcConnection =
                                it.service?.connect(applicationId, RPC_VERSION, callback)
                        }
                    }
                } catch (e: RemoteException) {
                    Log.d(TAG, "exception when trying to connect", e)
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            this@DiscordRpcClient.let {
                it.service = null
                it.rpcConnection = null
                Log.d(TAG, "CLOSE_ABNORMAL, service disconnected")
            }
        }
    }
    private val callback: IDiscordRpcCallback = object : IDiscordRpcCallback.Stub() {
        override fun onFrame(str: String?) {
            Log.d(TAG, "callback onFrame $str")
            val json = JsonParser.`object`().from(str)
            val command = json.getString("cmd")
            val event = json.getString("evt")
            if (command == "DISPATCH") {
                Log.d(TAG, "onFrame: dispatch cmd received")
                if (event == "READY") {
                    val payload = JsonWriter.string()
                        .`object`()
                        .value("cmd", "SET_ACTIVITY")
                        .value("nonce", "test")
                        .nul("evt")
                        .`object`("args")
                        .value("pid", 67)
                            .`object`("activity")
                                .value("application_id", this@DiscordRpcClient.pendingApplicationId.toString())
                                .value("name", "cock")
                                .value("details", "OOOOOOOOMAGA")
                                .value("state", "oaoaoaoaoaoaoaoaooa")
                                .value("type", 2) // "activity"
                                .`object`("assets")
                                    .value("large_image", "https://paige.moe/88x31.png")
                                    .value("large_text", "ssssss")
                                    .value("small_image", "https://paige.moe/88x31.png")
                                    .value("small_text", "fwqefwefwqefqwe")
                                    .end() // "assets"
                                .end()
                            .end() // "args"
                            .end() // root
                        .done()

                    rpcConnection?.sendFrame(payload)
                }
            }
        }

        override fun onClose(i: Int, str: String?) {
            Log.d(TAG, "callback onClose $str")
        }
    }

    private fun onConnected() {
        Log.d(TAG, "binder is connected")
    }

    fun connect(applicationId: Long) = scope.launch {
        pendingApplicationId = applicationId
        val serviceResolution = resolveServiceIntent()
        if (serviceResolution == null) {
            Log.d(TAG, "$INVALID_CLIENTID, discord not installed?")
            return@launch
        }
        try {
            isBound = activity.bindService(serviceResolution, this@DiscordRpcClient.aids, 1)
            if (!isBound) {
                Log.d(TAG, "$CLOSE_ABNORMAL, bindService returned false?")
            }
        } catch (e: SecurityException) {
            Log.d(TAG, "bindService denied, CLOSE_ABNORMAL", e)
        }
    }

    fun disconnect() = scope.launch {
        try {
            rpcConnection?.disconnect()
        } catch (e: RemoteException) {
            // noop
        }
    }

    fun sendFrame(str: String) = scope.launch {
        try {
            rpcConnection?.sendFrame(str)
        } catch(e: RemoteException) {
            Log.e(TAG, "remoteException when sending frame", e)
        }
    }

    fun resolveServiceIntent(): Intent? {
        DISCORD_PACKAGES.forEach {
            val intent = Intent("com.discord.socialsdk.rpc.IDiscordRpcService").apply {
                setPackage(it)
            }
            if (context.packageManager.resolveService(intent, 0) != null) {
                Log.d(TAG, "found intent for package $it, intent: $intent")
                return intent
            }
        }
        return null
    }
}