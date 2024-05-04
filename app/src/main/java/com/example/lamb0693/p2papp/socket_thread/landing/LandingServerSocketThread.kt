package com.example.lamb0693.p2papp.socket_thread.landing

import android.util.Log
import com.example.lamb0693.p2papp.socket_thread.ServerSocketThread
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback
import com.example.lamb0693.p2papp.socket_thread.bounce.BounceCons
import com.example.lamb0693.p2papp.viewmodel.GameState

class LandingServerSocketThread (
    private val messageCallback: ThreadMessageCallback,
) : ServerSocketThread(messageCallback){

    override var timerInterval : Long = LandingCons.REDRAW_INTERVAL
    var gameData = LandingData()

    private var count = 0

    override fun proceedGame() {
        super.proceedGame()
        if(isPaused) return

        count ++
        if( count%100 ==0) Log.i("proceedGame", "executed")
        gameData.serverLander.move()

        // 위 아래로 벗어나면 게임 중단
        if (gameData.serverLander.pos.y > 500) {
            isPaused = true
            gameData.resetData()
            sendMessageToClientViaSocket("SERVER_STOPPED_GAME")
            messageCallback.onGameStateMessageFromThread(GameState.STOPPED)
//            if(tempPoint.y <0) {
//                sendMessageToClientViaSocket("SERVER_WIN")
//                messageCallback.onGameWinnerFromThread(true)
//            } else {
//                sendMessageToClientViaSocket("CLIENT_WIN")
//                messageCallback.onGameWinnerFromThread(false)
//            }
            return
        }

        Log.i("proceedGame", "${gameData.serverLander.pos}")
    }

    override fun processGameDataInServer(strAction: String, manualRedraw: Boolean) {
        Log.i("processGameDataInServer", "processGameDataInServer() $strAction")

        if(isPaused) return

        if(strAction.startsWith("ACTION:")){
            val actions = strAction.split(":")
            if(actions.size != 5) {
                Log.e("processGameDataInServer", "error in actions structure")
                return
            }
            val left = actions[2].toBooleanStrictOrNull()
            val right = actions[3].toBooleanStrictOrNull()
            val upward = actions[4].toBooleanStrictOrNull()
            if(left != null && right != null && upward != null) {
                // 동시에 flame set
                if(actions[1] == "SERVER") gameData.serverLander.changeForce(left, right, upward)
                else if(actions[1] == "CLIENT") gameData.clientLander.changeForce(left, right, upward)
                else Log.e("processGameDataInServer", "error not SERVER either not CLIENT")
                if(manualRedraw) sendGameDataToFragments()
            }else {
                Log.e("processGameDataInServer", "one of parameter is not boolean")
                return
            }
        } else {
            Log.e("processGameDataInServer", "processGameDataInServer error in String. $strAction")
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