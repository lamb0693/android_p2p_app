package com.example.lamb0693.p2papp.socket_thread.test

import android.graphics.Picture
import android.graphics.PointF
import android.graphics.RectF
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

        // 정상 진행 한다고 가정
        var tempPoint = gameData.ball.testMove()

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
            gameData.ball.delta.x *= (-1)
            // 팅겨나간 것으로 반영
            tempPoint.x *= -1
        } else if (tempPoint.x >= TestGameCons.BITMAP_WIDTH){
            //오른쪽 벽 밖에 대한 처리
            // x movement를 반대로 설정
            gameData.ball.delta.x *= (-1)
            //팅겨 나간 것으로 반영
            tempPoint.x = TestGameCons.BITMAP_WIDTH - 2 * (tempPoint.x - TestGameCons.BITMAP_WIDTH)
        }

        // obstacle 충돌 처리, tempPoint값이 함수 안에서 수정 됨
        if(tempPoint.y in 100.0..400.0) {
            processCollideWithObstacle(tempPoint)
        }

        // 패들 충돌 처리, tempPoint 값이 함수 안에서 수정 됨
        if(gameData.ball.pos.y > 420 && gameData.ball.delta.y >0) processCollideWithServerPaddle(tempPoint)
        if(gameData.ball.pos.y < 100 && gameData.ball.delta.y <0) processCollideWithClientPaddle(tempPoint)

        // 계산한 temp 값으로 새로 ball 위치 결정
        gameData.ball.pos = tempPoint

        // effectServer ,effectClient가 있으면 obstacle count 진행
        // 1보다 크면 0을 만들고, 뺀 결과 0이면 effect를 없앤다
        gameData.effectServer?.let{
            if(gameData.effectRemainServer > 0) {
                gameData.effectRemainServer--
                if(gameData.effectRemainServer ==0){
                    Log.i(">>>>", "timeout, set server paddle status to 0")
                    gameData.serverPaddle.setPaddleState(0)
                    gameData.effectServer = null
                }
            }
        }
        gameData.effectClient?.let{
            if(gameData.effectRemainClient > 0){
                gameData.effectRemainClient--
                if(gameData.effectRemainClient ==0){
                    Log.i(">>>>", "timeout, set server paddle status to 0")
                    gameData.clientPaddle.setPaddleState(0)
                    gameData.effectClient = null
                }
            }
        }

        // obstacle 생성
        if( count%TestGameCons.OBSTACLE_REGEN_INTERVAL == 0) {
            gameData.obstacles.add(Obstacle())
        }
    }

    private fun processCollideWithServerPaddle(tempPoint: PointF){
        Log.i(">>>>", "executing processCollideWithServerPaddle")

        // y 좌표가 collision line을 통과 햇는지 확인
        if(! gameData.serverPaddle.passCollisionBorder(gameData.ball, tempPoint) ) return

        // 표면 line으로 옮김
        val fraction = gameData.serverPaddle.moveBackToCollisionBorder(gameData.ball, tempPoint)

        if( gameData.serverPaddle.isOnTheCollisionLine(tempPoint, gameData.ball.radius)) {
            Log.i(">>>>", "Line 안이라 반사 처리")
            gameData.isServerPlaying = true // server가 play한 것으로 처리
            val passedPoint = gameData.serverPaddle.getPointOfCollisionLine(tempPoint.x)
            Log.i(">>>>", "Paddle의 $passedPoint 부분에 부딛힘")
            resetServerDelta(passedPoint) // delta 가 변한다
            tempPoint.x += (1-fraction) * gameData.ball.delta.x * gameData.ball.spped
            tempPoint.y += (1-fraction) * gameData.ball.delta.y * gameData.ball.spped
        } else {
            // tempPoint는 moveBack 된 temp point이니 원래 위치에다 그대로 이동
            Log.i(">>>>", "line 밖이라 그냥 진행한 것으로 처리")
            // 되돌린 후에는 그냥 돌아가려면 되돌림을 취소하고 그냥 진행한 것을 적용해 주어야 함
            // val이라 값만 재 설정
            tempPoint.x = gameData.ball.pos.x + gameData.ball.delta.x * gameData.ball.spped
            tempPoint.y = gameData.ball.pos.y + gameData.ball.delta.y * gameData.ball.spped
        }
    }

    // x : 부딛힌 높이 에서의 X좌표, start : bar의 start 지점의 x 좌표
    // 부딛힌 부위에 따라 반사각을 다르게 설정
    private fun resetServerDelta(passedPoint : Float) {
        if(passedPoint > 0.9) {gameData.ball.delta.x =7f; gameData.ball.delta.y=-3f}
        else if(passedPoint > 0.8) {gameData.ball.delta.x = 5.4f; gameData.ball.delta.y = -5.4f}
        else if(passedPoint > 0.7) {gameData.ball.delta.x = 4.1f; gameData.ball.delta.y = -6.4f}
        else if(passedPoint > 0.5) {gameData.ball.delta.x = 3f; gameData.ball.delta.y = -7f}
        else if(passedPoint > 0.3) {gameData.ball.delta.x = -3f; gameData.ball.delta.y = -7f}
        else if(passedPoint > 0.2) {gameData.ball.delta.x = -4.1f; gameData.ball.delta.y = -6.4f}
        else if(passedPoint > 0.1) {gameData.ball.delta.x = -5.4f; gameData.ball.delta.y = -5.4f}
        else {gameData.ball.delta.x =-7f; gameData.ball.delta.y=-3f}
    }

    private fun processCollideWithClientPaddle(tempPoint: PointF) {
        Log.i(">>>>", "executing processCollideWithClientPaddle")

        // y 좌표가 collision line을 통과 햇는지 확인
        if(! gameData.clientPaddle.passCollisionBorder(gameData.ball, tempPoint)) return

        // 표면 line으로 옮김
        val fraction = gameData.clientPaddle.moveBackToCollisionBorder(gameData.ball, tempPoint)

        if( gameData.clientPaddle.isOnTheCollisionLine(tempPoint, gameData.ball.radius)) {
            Log.i(">>>>", "Line 안이라 반사 처리")
            gameData.isServerPlaying = false // server가 play한 것으로 처리
            val passedPoint = gameData.clientPaddle.getPointOfCollisionLine(tempPoint.x)
            Log.i(">>>>", "Paddle의 $passedPoint 부분에 부딛힘")
            resetClientDelta(passedPoint)
            tempPoint.x += (1-fraction) * gameData.ball.delta.x * gameData.ball.spped
            tempPoint.y += (1-fraction) * gameData.ball.delta.y * gameData.ball.spped
        } else {
            Log.i(">>>>", "line 밖이라 그냥 진행한 것으로 처리")
            // 되돌린 후에는 그냥 돌아가려면 되돌림을 취소하고 그냥 진행한 것을 적용해 주어야 함
            tempPoint.x = gameData.ball.pos.x + gameData.ball.delta.x * gameData.ball.spped
            tempPoint.y = gameData.ball.pos.y + gameData.ball.delta.y * gameData.ball.spped
        }
    }

    // x : 부딛힌 높이 에서의 X좌표, start : bar의 start 지점의 x 좌표
    // 부딛힌 부위에 따라 반사각을 다르게 설정
    private fun resetClientDelta(passedPoint : Float) {
        if(passedPoint > 0.9) {gameData.ball.delta.x =7f; gameData.ball.delta.y=3f}
        else if(passedPoint > 0.8) {gameData.ball.delta.x = 5.4f; gameData.ball.delta.y = 5.4f}
        else if(passedPoint > 0.7) {gameData.ball.delta.x = 4.1f; gameData.ball.delta.y = 6.4f}
        else if(passedPoint > 0.5) {gameData.ball.delta.x = 3f; gameData.ball.delta.y = 7f}
        else if(passedPoint > 0.3) {gameData.ball.delta.x = -3f; gameData.ball.delta.y = 7f}
        else if(passedPoint > 0.2) {gameData.ball.delta.x = -4.1f; gameData.ball.delta.y = 6.4f}
        else if(passedPoint > 0.1) {gameData.ball.delta.x = -5.4f; gameData.ball.delta.y = 5.4f}
        else {gameData.ball.delta.x =-7f; gameData.ball.delta.y= 3f}
    }

    private fun processCollideWithObstacle(tempPoint: PointF){
        for (indexOfObstacle in gameData.obstacles.indices) {
            val obstacle = gameData.obstacles[indexOfObstacle]
            val checkRect = RectF(
                obstacle.getRect().left - gameData.ball.radius,
                obstacle.getRect().top - gameData.ball.radius,
                obstacle.getRect().right + gameData.ball.radius,
                obstacle.getRect().bottom + gameData.ball.radius,
            )
            if (checkRect.contains(tempPoint.x, tempPoint.y)) {
                Log.i(">>>>", "collide with obstacle")
                // obstacle effect 설정
                when(obstacle.type){
                    // ball speed 변화
                    0, 1, 2 -> {
                        gameData.ball.spped = 0.8f
                        gameData.ball.spped = 1.1f
                        gameData.ball.spped = 1.4f
                    }
                    // ball 크기 변화
                    3, 4, 5 -> {
                        gameData.ball.setSize(obstacle.type -3)
                    }
                    // paddle 정상화
                    6 -> {
                        gameData.effectServer = null
                        gameData.effectRemainServer = 0
                        gameData.effectRemainClient = 0
                        gameData.isServerPlaying?.let{
                            if(it) gameData.serverPaddle.setPaddleState(0)
                            else gameData.clientPaddle.setPaddleState(0)
                        }
                    }
                    // paddle size 변화
                    7 ,8 -> {
                        gameData.isServerPlaying?.let{
                            if(it) {
                                Log.i(">>>>", "collision, isSeverPalying = true set server Paddle to 1")
                                gameData.effectServer = obstacle.type
                                gameData.effectRemainServer= TestGameCons.EFFECT_DURATION
                                gameData.serverPaddle.setPaddleState(obstacle.type - 6)
                            }else {
                                Log.i(">>>>", "collision, isSeverPalying = false set client Paddle to 1")
                                gameData.effectClient = obstacle.type
                                gameData.effectRemainClient= TestGameCons.EFFECT_DURATION
                                gameData.clientPaddle.setPaddleState(obstacle.type - 6)
                            }
                        }
                    }
                    else -> {
                        Log.e(">>>>", "ball type ${obstacle.type} not exist")
                    }
                }

                // 반대 방향으로 진행 설정
                gameData.ball.delta.y *= -1
                // 충돌한 것은 remove 후 for loop 에서 나감
                gameData.obstacles.removeAt(indexOfObstacle)
                gameData.obstacleRemnant = obstacle.getCurrentLocation() //display 후 null로
                break
            }
        }
    }

    override fun processGameDataInServer(strAction : String, manualRedraw : Boolean) {
        Log.i(">>>>", "processGameDataInServer() $strAction")

        if(isPaused) return

        if(strAction.startsWith("ACTION:")){
            val action = strAction.split(":")[1]
            when(action){
                "CLIENT_LEFT" -> gameData.clientPaddle.move(-10)
                "CLIENT_RIGHT" -> gameData.clientPaddle.move(10)
                "SERVER_LEFT" -> gameData.serverPaddle.move(-10)
                "SERVER_RIGHT" -> gameData.serverPaddle.move(10)
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