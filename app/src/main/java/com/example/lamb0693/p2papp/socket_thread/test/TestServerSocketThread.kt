package com.example.lamb0693.p2papp.socket_thread.test

import android.util.Log
import com.example.lamb0693.p2papp.Constant
import com.example.lamb0693.p2papp.socket_thread.ServerSocketThread
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback


class TestServerSocketThread (
    private val messageCallback: ThreadMessageCallback,
) : ServerSocketThread(messageCallback){

    override var timerInterval : Long = Constant.TEST_GAME_INTERVAL
    private var gameData = TestGameData()

    override fun proceedGame() {
        super.proceedGame()
        if(isPaused) return
        // 게임 진행
        gameData.ballX += gameData.ballMoveX
        gameData.ballY += gameData.ballMoveY
    }

    override fun processGameDataInServer(strAction : String, manualRedraw : Boolean) {
        Log.i(">>>>", "processGameDataInServer() $strAction")

        if(isPaused) return

        if(strAction.startsWith("ACTION:")){
            val action = strAction.split(":")[1]
            when(action){
                "CLIENT_LEFT" -> gameData.clientX -= 10
                "CLIENT_RIGHT" -> gameData.clientX += 10
                "SERVER_LEFT" -> gameData.serverX -= 10
                "SERVER_RIGHT" -> gameData.serverX +=10
            }
            if(manualRedraw) sendGameDataToFragments()
        } else {
            Log.e(">>>>", "processGameDataInServer error in String. $strAction")
        }
    }

    override fun sendGameDataToFragments(){
        if(isPaused) return

        synchronized(this){
            val strToSend = gameData.getStringToSendViaSocket()
            messageCallback.onGameDataReceivedFromThread(gameData)
            sendMessageToClientViaSocket(strToSend)
        }
    }
}