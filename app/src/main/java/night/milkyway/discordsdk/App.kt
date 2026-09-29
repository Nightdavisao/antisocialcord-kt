package night.milkyway.discordsdk

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import night.milkyway.discordsdk.social.DiscordRpcClient
import night.milkyway.discordsdk.ui.theme.DiscordIPCBinderTheme

@Composable
fun ComposeApp() {
    var discordClient: DiscordRpcClient? = null
    val activity = LocalActivity.current

    if (activity != null) {
        discordClient = DiscordRpcClient(
            LocalContext.current,
            activity
        )
    }

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
                    Button(
                        onClick = {
                            discordClient?.connect(1483888357686771863L)
                        }
                    ) {
                        Text("Connect to service")
                    }
                }
            }
        }
    }
}