// IDiscordRpcCallback.aidl
package com.discord.socialsdk.rpc;

// Declare any non-default types here with import statements

interface IDiscordRpcCallback {

    /**
     * Called when a frame data string is received.
     * Transaction ID: 1
     */
    void onFrame(String str);

    /**
     * Called when the RPC connection is closed.
     * Transaction ID: 2
     */
    void onClose(int i, String str);
}