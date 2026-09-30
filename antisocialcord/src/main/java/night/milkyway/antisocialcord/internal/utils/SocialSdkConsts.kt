package night.milkyway.antisocialcord.internal.utils

object SocialSdkConsts {
    // you can grab the package by running something like this: adb shell dumpsys package | grep -A5 "com.discord.socialrpc.DiscordRpcService"
    val DISCORD_PACKAGES = listOf("com.discord", "cocobo1.pupu.app")
    const val CLOSE_ABNORMAL = 1006
    const val INVALID_CLIENT_ID = 4000
    internal const val RPC_VERSION = "1"
}