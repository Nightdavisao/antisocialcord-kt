package night.milkyway.discordsdk

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import night.milkyway.antisocialcord.DiscordRpcClient
import night.milkyway.antisocialcord.api.AbstractRpcEventHandler
import night.milkyway.antisocialcord.model.Activity
import night.milkyway.antisocialcord.model.ActivityArguments
import night.milkyway.antisocialcord.model.ActivityType
import night.milkyway.antisocialcord.model.RpcEvent
import night.milkyway.antisocialcord.model.RpcPayload
import night.milkyway.discordsdk.ui.theme.DiscordIPCBinderTheme


const val TAG = "ComposeApp"
@Composable
fun ComposeApp() {
    var isConnectedState by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val rpcClient = DiscordRpcClient(context)
    rpcClient.addListener(object : AbstractRpcEventHandler() {
        override fun onConnected() {
            Log.d(TAG, "onConnected")
            isConnectedState = !isConnectedState
        }

        override fun onDisconnected() {
            Log.d(TAG, "onDisconnected")
            isConnectedState = !isConnectedState
        }

        override fun onReady(payload: RpcPayload) {
            Log.d(TAG, "onReady: $payload")
        }

        override fun onSetActivity(
            arguments: ActivityArguments?,
            event: RpcEvent?,
            nonce: String?
        ) {
            Log.d(TAG, "onSetActivity: $arguments, $event, $nonce")
        }
    })

    DiscordIPCBinderTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(
                modifier = Modifier.padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("discord ipc oooo")
                    Row {
                        Button(
                            onClick = {
                                rpcClient.connect(1483888357686771863L)
                                //discordClient?.connect(1483888357686771863L)
                            }
                        ) {
                            Text("Connect to service")
                        }
                        Button(
                            onClick = {
                                rpcClient.disconnect()
                            }
                        ) {
                            Text("Disconnect from service")
                        }
                    }
                    Button(
                        onClick = {
                            Activity(
                                name = "test",
                                activityType = ActivityType.PLAYING
                            ).let {
                                rpcClient.setActivity(it) {
                                    Log.d(TAG, "ComposeApp: callback was called back! wow")
                                }
                            }
                        }
                    ) {
                        Text("Set playing activity")
                    }
                    Button(
                        onClick = {
                            rpcClient.clearActivity()
                        }
                    ) {
                        Text("Clear activity")
                    }
                    Text(
                        "Is connected to IPC? $isConnectedState"
                    )
                }
            }
        }
    }
}