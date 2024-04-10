package com.example.lamb0693.p2papp.socket_thread

import com.example.lamb0693.p2papp.socket_thread.test.TestGameData

interface ThreadMessageCallback {
    fun onMessageReceivedFromThread(message : String)
    fun onThreadTerminated()
    fun onThreadStarted()
    fun onGameDataReceivedFromThread(gameData: TestGameData)
    fun onGameDataReceivedFromServerViaSocket(strGameData: String)
    fun onConnectionMade()
}