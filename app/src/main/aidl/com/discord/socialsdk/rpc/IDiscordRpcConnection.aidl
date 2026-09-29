// IDiscordRpcConnection.aidl
package com.discord.socialsdk.rpc;

// Declare any non-default types here with import statements

interface IDiscordRpcConnection {
    void sendFrame(String str);

    void disconnect();
}