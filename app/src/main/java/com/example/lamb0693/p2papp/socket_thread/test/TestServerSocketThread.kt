package com.example.lamb0693.p2papp.socket_thread.test

import android.graphics.Picture
import android.graphics.PointF
import android.util.Log
import com.example.lamb0693.p2papp.socket_thread.ServerSocketThread
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback
import com.example.lamb0693.p2papp.viewmodel.GameState
import kotlin.math.*


class TestServerSocketThread (
    private val messageCallback: ThreadMessageCallback,
) : ServerSocketThread(messageCallback){

    override var timerInterval : Long = TestGameCons.TEST_GAME_INTERVAL
    private var gameData = TestGameData()

    private var count : Int = 0
    private val obstacleGenerationInterval = 20

    override fun proceedGame() {
        super.proceedGame()
        if(isPaused) return

        count ++

        // obstacle 이동 및 범위 초과하면 remove
        gameData.obstacles.forEach{it.move() }
        gameData.obstacles.removeIf {
            it.curPosX > TestGameCons.BITMAP_WIDTH || it.curPosX < 0
        }

        // 게임 진행
        val tempPoint = PointF()

        // 정상 진행 한다고 가정
        tempPoint.x = gameData.ballX + gameData.ballMoveX
        tempPoint.y = gameData.ballY + gameData.ballMoveY

        // 위 아래로 벗어나면 게임 중단
        if (tempPoint.y < 0 || tempPoint.y > 500) {
            isPaused = true
            gameData.resetData()
            sendMessageToClientViaSocket("SERVER_STOPPED_GAME")
            messageCallback.onGameStateMessageFromThread(GameState.STOPPED)
            if(tempPoint.y <0) {
                sendMessageToClientViaSocket("SERVER_WIN")
                messageCallback.onGameWinnerFromThread(true)
            } else {
                sendMessageToClientViaSocket("CLIENT_WIN")
                messageCallback.onGameWinnerFromThread(false)
            }
            return
        }

        // 왼쪽 오른쪽 벽 밖이면 방향 전환 후 x 좌표를 반대로
        if (tempPoint.x <= 0) {
            // x movement를 반대로 설정하고
            gameData.ballMoveX = -gameData.ballMoveX
            // 팅겨나간 것으로 반영
            tempPoint.x *= -1
        } else if (tempPoint.x >= TestGameCons.BITMAP_WIDTH){
            //오른쪽 벽 밖에 대한 처리
            // x movement를 반대로 설정
            gameData.ballMoveX *= (-1)
            //팅겨 나간 것으로 반영
            tempPoint.x = TestGameCons.BITMAP_WIDTH - 2 * (tempPoint.x - TestGameCons.BITMAP_WIDTH)
        }

        // obstacle 충돌 처리, tempPoint값이 함수 안에서 수정 됨
        if(tempPoint.y in 100.0..400.0) {
            processCollideWithObstacle(tempPoint)
        }

        // 패들 충돌 처리, tempPoint 값이 함수 안에서 수정 됨
        if(gameData.ballY > 420 && gameData.ballMoveY>0) processCollideWithServerPaddle(tempPoint)
        if(gameData.ballY < 100 && gameData.ballMoveY<0) processCollideWithClientPaddle(tempPoint)

        // 계산한 temp 값으로 새로 ball 위치 결정
        gameData.ballX = tempPoint.x
        gameData.ballY = tempPoint.y

        // obstacle 생성
        if( count%obstacleGenerationInterval == 0) {
            gameData.obstacles.add(Obstacle())
        }
    }

    private fun processCollideWithServerPaddle(tempPoint: PointF) : Boolean{
        Log.i(">>>>", "executing processCollideWithServerPaddle")
        //gameData.ballX, gameData.ballY 진행전  tempPoint 가상 진행 후
        val upperLineY = gameData.serverY - TestGameCons.BAR_HEIGHT/2f
        val serverXStart = gameData.serverX - TestGameCons.BAR_WIDTH/2f
        val serverXEnd = gameData.serverX + TestGameCons.BAR_WIDTH/2f

        // passed downward? 볼 엉덩이가 선을 지나갔는 지
        if(gameData.ballY + gameData.ballRadius >= upperLineY ) return false // 출발점이 접촉면이면 pass가 안됨
        if(tempPoint.y + gameData.ballRadius <  upperLineY ) return false  // 밖에서 출발 접촉선에 물리면 pass로 판정
        Log.i(">>>>", "prev ballX, ballY, ${gameData.ballX}, ${gameData.ballY}")
        Log.i(">>>>", "temp ball ${tempPoint.x} ${tempPoint.y}")
        Log.i(">>>>", "passed through Line")
        Log.i(">>>>", "server bar from $serverXStart to $serverXEnd")

        // 현재 ball의 X(들어와 있는 상태) 에서 line 안인지
        if(tempPoint.x < serverXStart-10 || tempPoint.x > serverXEnd +10) { //10정도 여유 주어야 그래픽이 맞음
            Log.i(">>>>", "line 밖이라 그냥 진행한 것으로 처리")
            // 되돌린 후에는 그냥 돌아가려면 되돌림을 취소하고 그냥 진행한 것을 적용해 주어야 함
            tempPoint.x= gameData.ballX + gameData.ballMoveX
            tempPoint.y = gameData.ballY + gameData.ballMoveY
        } else {
            Log.i(">>>>", "Line 안이라 반사 처리")
            val passedFraction = moveBackOutsideServerPaddle(tempPoint)
            resetServerDelta(tempPoint.x, serverXStart)
            tempPoint.x += (1-passedFraction) * gameData.ballMoveX
            tempPoint.y += (1-passedFraction) * gameData.ballMoveY
        }

        return true
    }

    private fun moveBackOutsideServerPaddle(tempPoint : PointF) : Float{
        Log.i(">>>>", "moveBack")
        //Log.i(">>>>", "delta ${gameData.ballMoveX}, ${gameData.ballMoveY}")
        val ballLower = tempPoint.y + gameData.ballRadius
        //Log.i(">>>>", "ballLower $ballLower")
        val paddleUpper = gameData.serverY - TestGameCons.BAR_HEIGHT/2f
        //Log.i(">>>>", "paddleUpper $paddleUpper")
        val toMoveY = ballLower - paddleUpper
        //Log.i(">>>>", "toMoveY $toMoveY")
        val toMoveX = (gameData.ballMoveX/gameData.ballMoveY) * toMoveY
        //Log.i(">>>>", "toMoveX $toMoveX")
        tempPoint.x -= toMoveX
        tempPoint.y -= toMoveY
        Log.i(">>>>", "moved back to surface $tempPoint.x, $tempPoint.y")
        val fractionProceeded = abs(toMoveY/gameData.ballMoveY)
        Log.i(">>>>", "fractionProceeded = $fractionProceeded")
        return abs(fractionProceeded)
    }

    private fun resetServerDelta(x : Float, start : Float) {
        val pos = (x - start)/TestGameCons.BAR_WIDTH
        Log.i(">>>>", "resetDelta() pos : $pos")
        if(pos > 0.9) {gameData.ballMoveX =7f; gameData.ballMoveY=-3f}
        else if(pos > 0.8) {gameData.ballMoveX = 5.4f; gameData.ballMoveY = -5.4f}
        else if(pos > 0.7) {gameData.ballMoveX = 4.1f; gameData.ballMoveY = -6.4f}
        else if(pos > 0.5) {gameData.ballMoveX = 3f; gameData.ballMoveY = -7f}
        else if(pos > 0.3) {gameData.ballMoveX = -3f; gameData.ballMoveY = -7f}
        else if(pos > 0.2) {gameData.ballMoveX = -4.1f; gameData.ballMoveY = -6.4f}
        else if(pos > 0.1) {gameData.ballMoveX = -5.4f; gameData.ballMoveY = -5.4f}
        else {gameData.ballMoveX =-7f; gameData.ballMoveY=-3f}
    }

    private fun processCollideWithClientPaddle(tempPoint: PointF) {
        Log.i(">>>>", "executing processCollideWithClientPaddle")
        //gameData.ballX, gameData.ballY 진행전  tempPoint 가상 진행 후
        val lowerLineY = gameData.clientY + TestGameCons.BAR_HEIGHT/2f
        val clientXStart = gameData.clientX - TestGameCons.BAR_WIDTH/2f
        val clientXEnd = gameData.clientX + TestGameCons.BAR_WIDTH/2f

        // passed upward? 볼 머리가 선을 지나갔는 지
        if(gameData.ballY - gameData.ballRadius <= lowerLineY ) return// 출발점이 접촉면이면 pass가 안됨
        if(tempPoint.y - gameData.ballRadius >  lowerLineY ) return// 밖에서 출발 접촉선에 물리면 pass로 판정
        Log.i(">>>>", "prev ballX, ballY, ${gameData.ballX}, ${gameData.ballY}")
        Log.i(">>>>", "temp ball ${tempPoint.x} ${tempPoint.y}")
        Log.i(">>>>", "passed through Line")
        Log.i(">>>>", "client bar from $clientXStart to $clientXEnd")

        // 현재 ball의 X(들어와 있는 상태) 에서 line 안인지
        if(tempPoint.x < clientXStart-10 || tempPoint.x > clientXEnd +10) { //10정도 여유 주어야 그래픽이 맞음
            Log.i(">>>>", "line 밖이라 그냥 진행한 것으로 처리")
            tempPoint.x= gameData.ballX + gameData.ballMoveX
            tempPoint.y = gameData.ballY + gameData.ballMoveY
        }else {
            Log.i(">>>>", "Line 안이라 새로운 반사처리")
            // 퇑과 했으면 접점으로 되 돌림  접점 물릴 때 까지
            val passedFraction = moveBackOutsideClientPaddle(tempPoint)
            resetClientDelta(tempPoint.x, clientXStart)
            tempPoint.x += (1-passedFraction) * gameData.ballMoveX
            tempPoint.y += (1-passedFraction) * gameData.ballMoveY
        }
    }

    private fun moveBackOutsideClientPaddle(tempPoint : PointF) : Float{
        Log.i(">>>>", "moveBack")
        //Log.i(">>>>", "delta ${gameData.ballMoveX}, ${gameData.ballMoveY}")
        val ballUpper = tempPoint.y - gameData.ballRadius
        //Log.i(">>>>", "ballUpper $ballUpper")
        val paddleLower = gameData.clientY + TestGameCons.BAR_HEIGHT/2f
        //Log.i(">>>>", "paddleLower $paddleLower")
        val toMoveY = paddleLower - ballUpper
        //Log.i(">>>>", "toMoveY $toMoveY")
        val toMoveX = (gameData.ballMoveX/gameData.ballMoveY) * toMoveY
        //Log.i(">>>>", "toMoveX $toMoveX")
        tempPoint.x += toMoveX
        tempPoint.y += toMoveY
        Log.i(">>>>", "moved $tempPoint.x, $tempPoint.y")

        val fractionProceeded = abs(toMoveY/gameData.ballMoveY)
        Log.i(">>>>", "fractionProceeded = $fractionProceeded")
        return abs(fractionProceeded)
    }

    private fun resetClientDelta(x : Float, start : Float) {
        val pos = (x - start)/TestGameCons.BAR_WIDTH
        Log.i(">>>>", "resetDelta() pos : $pos")
        if(pos > 0.9) {gameData.ballMoveX =7f; gameData.ballMoveY=3f}
        else if(pos > 0.8) {gameData.ballMoveX = 5.4f; gameData.ballMoveY = 5.4f}
        else if(pos > 0.7) {gameData.ballMoveX = 4.1f; gameData.ballMoveY = 6.4f}
        else if(pos > 0.5) {gameData.ballMoveX = 3f; gameData.ballMoveY = 7f}
        else if(pos > 0.3) {gameData.ballMoveX = -3f; gameData.ballMoveY = 7f}
        else if(pos > 0.2) {gameData.ballMoveX = -4.1f; gameData.ballMoveY = 6.4f}
        else if(pos > 0.1) {gameData.ballMoveX = -5.4f; gameData.ballMoveY = 5.4f}
        else {gameData.ballMoveX =-7f; gameData.ballMoveY= 3f}
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

    private fun processCollideWithObstacle(tempPoint: PointF){
        for (indexOfObstacle in gameData.obstacles.indices) {
            val obstacle = gameData.obstacles[indexOfObstacle]
            if (obstacle.getRect().contains(tempPoint.x, tempPoint.y)) {
                Log.i(">>>>", "collide with obstacle")
                // 반대 방향으로 진행 설정
                gameData.ballMoveY *= -1
                // 충돌한 것은 remove 후 for loop 에서 나감
                gameData.obstacles.removeAt(indexOfObstacle)
                gameData.obstacleRemnant = obstacle.getCurrentLocation() //display 후 null로
                break
            }
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