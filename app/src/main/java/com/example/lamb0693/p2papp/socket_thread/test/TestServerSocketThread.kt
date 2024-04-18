package com.example.lamb0693.p2papp.socket_thread.test

import android.util.Log
import com.example.lamb0693.p2papp.Constant
import com.example.lamb0693.p2papp.socket_thread.ServerSocketThread
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback


class TestServerSocketThread (
    private val messageCallback: ThreadMessageCallback,
) : ServerSocketThread(messageCallback){

    override var timerInterval : Long = TestGameCons.TEST_GAME_INTERVAL
    private var gameData = TestGameData()

    override fun proceedGame() {
        super.proceedGame()
        if(isPaused) return
        // 게임 진행
        gameData.ballX += gameData.ballMoveX
        gameData.ballY += gameData.ballMoveY

        // Check if the ball reaches the left or right edge of the view
        if (gameData.ballX <= 0 || gameData.ballX >= TestGameCons.BITMAP_WIDTH) {
            // Reverse the horizontal movement
            gameData.ballMoveX = -gameData.ballMoveX
        }

        // Check if the ball reaches the top or bottom edge of the view
        if (gameData.ballY <= 0 || gameData.ballY >= 500) {
            // Reverse the vertical movement
            gameData.ballMoveY = -gameData.ballMoveY
        }
    }

    override fun processGameDataInServer(strAction : String, manualRedraw : Boolean) {
        Log.i(">>>>", "processGameDataInServer() $strAction")

        if(isPaused) return

        if(strAction.startsWith("ACTION:")){
            val action = strAction.split(":")[1]
            when(action){
                "CLIENT_LEFT" -> {
                    gameData.clientX -= 10
                    if(gameData.clientX < TestGameCons.barWidth/2f) gameData.clientX = TestGameCons.barWidth/2f
                }
                "CLIENT_RIGHT" -> {
                    gameData.clientX += 10
                    if(gameData.clientX > TestGameCons.BITMAP_WIDTH - TestGameCons.barWidth/2f)
                        gameData.clientX = TestGameCons.BITMAP_WIDTH- TestGameCons.barWidth/2f
                    }
                "SERVER_LEFT" -> {
                    gameData.serverX -= 10
                    if(gameData.serverX < TestGameCons.barWidth/2) gameData.serverX = TestGameCons.barWidth/2f
                }
                "SERVER_RIGHT" -> {
                    gameData.serverX +=10
                    if(gameData.serverX > TestGameCons.BITMAP_WIDTH - TestGameCons.barWidth/2f)
                        gameData.serverX = TestGameCons.BITMAP_WIDTH - TestGameCons.barWidth/2f
                }
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