package com.example.lamb0693.p2papp.socket_thread

import com.example.lamb0693.p2papp.socket_thread.test.TestGameData
import com.example.lamb0693.p2papp.viewmodel.GameState

interface ThreadMessageCallback {
    fun onThreadTerminated()
    fun onThreadStarted()
    fun onGameStateMessageFromThread(gameState: GameState)
    fun onGameStateFromServerViaSocket(gameState : GameState)
    fun onGameDataReceivedFromThread(gameData: TestGameData)
    fun onGameDataReceivedFromServerViaSocket(strGameData: String)
    fun onConnectionMade()
    fun onOtherMessageReceivedFromServerViaSocket(receivedMessage: String)
    fun onOtherMessageFromClientViaSocket(receivedMessage: String)
}