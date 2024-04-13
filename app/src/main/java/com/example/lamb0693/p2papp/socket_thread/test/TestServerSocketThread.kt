package com.example.lamb0693.p2papp.socket_thread.test

import android.util.Log
import com.example.lamb0693.p2papp.socket_thread.ServerSocketThread
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback


class TestServerSocketThread (
    private val messageCallback: ThreadMessageCallback,
    private val timer : Long
) : ServerSocketThread(messageCallback, timer){
    private var gameData = TestGameData(10.0F, 10.0F)

    override fun proceedGame() {
        super.proceedGame()
        if(gameData.charY< 400) gameData.charY += 5
    }

    override fun processGameDataInServer(strAction : String, manualRedraw : Boolean) {
        Log.i(">>>>", "processGameDataInServer() $strAction")

        if(strAction.startsWith("ACTION:")){
            val action = strAction.split(":")[1]
            when(action){
                "LEFT" -> gameData.charX -= 10
                "RIGHT" -> gameData.charX += 10
                "UP" -> gameData.charY -= 10
                "DOWN" -> gameData.charY +=10
            }
            if(manualRedraw) sendGameDataToFragments()
        } else {
            Log.e(">>>>", "processGameDataInServer error in String. $strAction")
        }
    }

    override fun sendGameDataToFragments(){
        synchronized(this){
            val strToSend = gameData.getStringToSendViaSocket()
            messageCallback.onGameDataReceivedFromThread(gameData)
            sendMessageToClientViaSocket(strToSend)
        }
    }
}