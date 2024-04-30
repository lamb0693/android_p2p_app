package com.example.lamb0693.p2papp.socket_thread

import com.example.lamb0693.p2papp.socket_thread.bounce.BounceData
import com.example.lamb0693.p2papp.viewmodel.GameState

/******************************
 * ServerSocketThread 나 ClientSocketThread에서 해당 Fragment의 함수를 실행 시키기 위한
 * Fragment : ThreadMesssageCallback(this){
 *      override fun onFunction()
 * }
 *
 * SocketThread(private val callback : ThreadMessageCallback){
 *      callback.onFunction()
 * }
 * *****************************/

interface ThreadMessageCallback {
    fun onThreadTerminated()
    fun onThreadStarted()
    fun onGameStateMessageFromThread(gameState: GameState)
    fun onGameStateFromServerViaSocket(gameState : GameState)
    fun onGameDataReceivedFromThread(gameData : Any)
    fun onGameDataReceivedFromServerViaSocket(strGameData: String)
    fun onGameWinnerFromThread(isServerWin: Boolean)
    fun onGameWinnerFromServerViaSocket(isServerWin: Boolean)
    fun onConnectionMade()
    fun onOtherMessageReceivedFromServerViaSocket(receivedMessage: String)
    fun onOtherMessageFromClientViaSocket(receivedMessage: String)
}