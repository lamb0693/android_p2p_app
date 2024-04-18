package com.example.lamb0693.p2papp.socket_thread.test

import android.util.Log
import com.example.lamb0693.p2papp.socket_thread.ServerSocketThread
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback
import com.example.lamb0693.p2papp.viewmodel.GameState


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
        // send message to server fragment and client thread
        if (gameData.ballY < 0 || gameData.ballY > 500) {
            isPaused = true
            sendMessageToClientViaSocket("SERVER_STOPPED_GAME")
            messageCallback.onGameStateMessageFromThread(GameState.STOPPED)
            if(gameData.ballY <0) {
                sendMessageToClientViaSocket("SERVER_WIN")
                messageCallback.onGameWinnerFromThread(true)
            } else {
                sendMessageToClientViaSocket("CLIENT_WIN")
                messageCallback.onGameWinnerFromThread(false)
            }
            gameData.resetData()
        }

        // server paddle(lower)에 충돌 여부 체크
        if (gameData.ballMoveY > 0 && isBallInThePaddle(true)) {
            gameData.ballMoveY *= (-1)
            gameData.ballY += gameData.ballMoveY
        }

        // client paddle(upper)에 충돌 여부 체크
        if (gameData.ballMoveY < 0 && isBallInThePaddle(false)) {
            gameData.ballMoveY *= (-1)
            gameData.ballY += gameData.ballMoveY
        }
    }

    private fun isBallInThePaddle(isServer : Boolean) : Boolean {
        if(isServer) {
            if(gameData.ballY < gameData.serverY - TestGameCons.BAR_HEIGHT/2.0f) return false
            if(gameData.ballY > gameData.serverY + TestGameCons.BAR_HEIGHT/2.0f) return false
            if(gameData.ballX < gameData.serverX - TestGameCons.BAR_WIDTH/2.0f) return false
            if(gameData.ballX > gameData.serverX + TestGameCons.BAR_WIDTH/2.0f) return false
            return true
        } else {
            if(gameData.ballY < gameData.clientY - TestGameCons.BAR_HEIGHT/2.0f) return false
            if(gameData.ballY > gameData.clientY + TestGameCons.BAR_HEIGHT/2.0f) return false
            if(gameData.ballX < gameData.clientX - TestGameCons.BAR_WIDTH/2.0f) return false
            if(gameData.ballX > gameData.clientX + TestGameCons.BAR_WIDTH/2.0f) return false
            return true
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
                    if(gameData.clientX < TestGameCons.BAR_WIDTH/2f) gameData.clientX = TestGameCons.BAR_WIDTH/2f
                }
                "CLIENT_RIGHT" -> {
                    gameData.clientX += 10
                    if(gameData.clientX > TestGameCons.BITMAP_WIDTH - TestGameCons.BAR_WIDTH/2f)
                        gameData.clientX = TestGameCons.BITMAP_WIDTH- TestGameCons.BAR_WIDTH/2f
                    }
                "SERVER_LEFT" -> {
                    gameData.serverX -= 10
                    if(gameData.serverX < TestGameCons.BAR_WIDTH/2) gameData.serverX = TestGameCons.BAR_WIDTH/2f
                }
                "SERVER_RIGHT" -> {
                    gameData.serverX +=10
                    if(gameData.serverX > TestGameCons.BITMAP_WIDTH - TestGameCons.BAR_WIDTH/2f)
                        gameData.serverX = TestGameCons.BITMAP_WIDTH - TestGameCons.BAR_WIDTH/2f
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