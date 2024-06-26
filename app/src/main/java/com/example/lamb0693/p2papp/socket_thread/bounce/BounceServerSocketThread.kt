package com.example.lamb0693.p2papp.socket_thread.bounce

import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.example.lamb0693.p2papp.socket_thread.ServerSocketThread
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback
import com.example.lamb0693.p2papp.viewmodel.GameState
import kotlin.math.*


class BounceServerSocketThread (
    private val messageCallback: ThreadMessageCallback,
) : ServerSocketThread(messageCallback){

    override var timerInterval : Long = BounceCons.TEST_GAME_INTERVAL
    private var gameData = BounceData()

    private var count : Int = 0

    override fun proceedGame() {
        super.proceedGame()
        if(isPaused) return

        count ++
        // 게임 진행

        // 정상 진행 한다고 가정
        val tempPoint = gameData.ball.testMove()

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
        } else if (tempPoint.x >= BounceCons.BITMAP_WIDTH){
            //오른쪽 벽 밖에 대한 처리
            // x movement를 반대로 설정
            gameData.ball.delta.x *= (-1)
            //팅겨 나간 것으로 반영
            tempPoint.x = BounceCons.BITMAP_WIDTH - 2 * (tempPoint.x - BounceCons.BITMAP_WIDTH)
        }

        // obstacle 이동 및 범위 초과 하면 remove
        gameData.obstacles.forEach{it.move() }
        gameData.obstacles.removeIf {
            it.curPosX > BounceCons.BITMAP_WIDTH || it.curPosX < 0
        }
        // obstacle 충돌 처리, tempPoint 값이 함수 안에서 수정 됨
        if(tempPoint.y in 100.0..400.0) {
            processCollideWithObstacle(tempPoint)
        }

        // 패들 충돌 처리, tempPoint 값이 함수 안에서 수정 됨
        if(gameData.ball.pos.y > 420 && gameData.ball.delta.y >0) processCollideWithServerPaddle(tempPoint)
        if(gameData.ball.pos.y < 100 && gameData.ball.delta.y <0) processCollideWithClientPaddle(tempPoint)

        // 계산한 temp 값으로 새로 ball 위치 결정
        gameData.ball.pos = tempPoint

        // effectServer ,effectClient 가 있으면 obstacle count 진행
        // 1보다 크면 0을 만들고, 뺀 결과 0이면 effect를 없앤다
        gameData.effectServer?.let{
            if(gameData.effectRemainServer > 0) gameData.effectRemainServer--
            if(gameData.effectRemainServer == 0){
                Log.i("proceedGame", "timeout, set server paddle status to 0")
                gameData.serverPaddle.setPaddleState(0)
                gameData.effectServer = null
            }
        }
        gameData.effectClient?.let{
            if(gameData.effectRemainClient > 0)  gameData.effectRemainClient--
            // frame 빠짐 대비
            if(gameData.effectRemainClient ==0){
                Log.i("proceedGame", "timeout, set client paddle status to 0")
                gameData.clientPaddle.setPaddleState(0)
                gameData.effectClient = null
            }
        }

        // obstacle 생성
        if( count%BounceCons.OBSTACLE_REGEN_INTERVAL == 0) {
            gameData.obstacles.add(Obstacle())
        }
    }

    private fun processCollideWithServerPaddle(tempPoint: PointF){
        synchronized(gameData.serverPaddle){
            Log.i("processCollideWithServerPaddle", "executing processCollideWithServerPaddle")

            // y 좌표가 collision line 을 통과 햇는지 확인
            if(! gameData.serverPaddle.passCollisionBorder(gameData.ball, tempPoint) ) return
            val savedTempPoint = PointF(tempPoint.x, tempPoint.y) // 원래 이동 해야 할 위치 저장
            Log.i("processCollideWithServerPaddle", "server paddle 충돌면 지나감")

            // ball 을 충돌 line 으로 옮김
            val fraction = gameData.serverPaddle.moveBackToCollisionBorder(gameData.ball, tempPoint)

            // 현재 tempPoint 는 충돌선 으로 이동 상태
            if(gameData.serverPaddle.isOnTheCollisionLine(tempPoint, gameData.ball.radius + 15)) {
                //back
                Log.i("processCollideWithServerPaddle", "Line 안이라 반사 처리")
                gameData.isServerPlaying = true // server 가 play 한 것으로 처리
                val passedPoint = gameData.serverPaddle.getPointOfCollisionLine(tempPoint.x)
                Log.i("processCollideWithServerPaddle", "Paddle 의 $passedPoint 부분에 부딛힘")
                resetServerDelta(passedPoint) // delta 가 변한다
                // 물러선 fraction 만큼 진행
                tempPoint.x += fraction * gameData.ball.delta.x * gameData.ball.spped
                tempPoint.y += fraction * gameData.ball.delta.y * gameData.ball.spped
            } else {
                // 가까운 범위에서는 옆으로 튀고(방향만 바꾸어 놓음), 먼 곳에서는 그냥 진행
                if(gameData.serverPaddle.isOnTheCollisionLine(savedTempPoint,
                        gameData.ball.radius + abs(gameData.ball.delta.x*6)))
                        { gameData.ball.delta.x *= -1 }
                tempPoint.x = savedTempPoint.x
                tempPoint.y = savedTempPoint.y
            }
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
        synchronized(gameData.clientPaddle) {
            Log.i("processCollideWithClientPaddle", "executing processCollideWithClientPaddle")

            // y 좌표가 collision line 을 통과 햇는지 확인
            if(! gameData.clientPaddle.passCollisionBorder(gameData.ball, tempPoint) ) return
            val savedTempPoint = PointF(tempPoint.x, tempPoint.y) // 원래 이동 해야 할 위치 저장
            Log.i("processCollideWithClientPaddle", "client paddle 충돌면 지나감")

            // ball 을 충돌 line 으로 옮김
            val fraction = gameData.clientPaddle.moveBackToCollisionBorder(gameData.ball, tempPoint)

            // 현재 tempPoint 는 충돌선 으로 이동 상태
            if(gameData.clientPaddle.isOnTheCollisionLine(tempPoint, gameData.ball.radius + 15)) {
                //back
                Log.i("processCollideWithClientPaddle", "Line 안이라 반사 처리")
                gameData.isServerPlaying = false // client 가 play 한 것으로 처리
                val passedPoint = gameData.clientPaddle.getPointOfCollisionLine(tempPoint.x)
                Log.i("processCollideWithClientPaddle", "client Paddle 의 $passedPoint 부분에 부딛힘")
                resetClientDelta(passedPoint) // delta 가 변한다
                // 물러선 fraction 만큼 진행
                tempPoint.x += fraction * gameData.ball.delta.x * gameData.ball.spped
                tempPoint.y += fraction * gameData.ball.delta.y * gameData.ball.spped
            } else {
                // 가까운 범위에서는 옆으로 튀고, 먼 곳에서는 그냥 진행
                if(gameData.clientPaddle.isOnTheCollisionLine(savedTempPoint,
                        gameData.ball.radius + abs(gameData.ball.delta.x*6)))
                                { gameData.ball.delta.x *= -1 }
                tempPoint.x = savedTempPoint.x
                tempPoint.y = savedTempPoint.y
            }
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
                Log.i("processCollideWithObstacle", "collide with obstacle ${obstacle.type} at $tempPoint")
                // obstacle effect 설정
                when(obstacle.type){
                    // ball speed 변화
                    0 -> gameData.ball.spped = BounceCons.BALL_SPEED_LOW
                    1 -> gameData.ball.spped = BounceCons.BALL_SPEED_NORMAL
                    2 -> gameData.ball.spped = BounceCons.BALL_SPEED_HIGH
                    // ball 크기 변화
                    3, 4, 5 -> {
                        gameData.ball.setSizeIndex(obstacle.type -3)
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
                        //7-6=1 large paddle,  8-6=2 small paddle
                        gameData.isServerPlaying?.let{
                            if(it) {
                                Log.i("processCollideWithObstacle", "collision, isSeverPalying = true set server Paddle to 1")
                                gameData.effectServer = obstacle.type
                                gameData.effectRemainServer= BounceCons.EFFECT_DURATION
                                gameData.serverPaddle.setPaddleState(obstacle.type - 6)
                            }else {
                                Log.i("processCollideWithObstacle", "collision, isSeverPalying = false set client Paddle to 1")
                                gameData.effectClient = obstacle.type
                                gameData.effectRemainClient= BounceCons.EFFECT_DURATION
                                gameData.clientPaddle.setPaddleState(obstacle.type - 6)
                            }
                        }
                    }
                    else -> {
                        Log.e("processCollideWithObstacle", "ball type ${obstacle.type} not exist")
                    }
                }

                // 반대 방향으로 진행 설정
                // 지났으면 옆으로 아니면 아래위로
                if(gameData.ball.delta.y >0  && tempPoint.y>obstacle.row*50+100
                    || gameData.ball.delta.y < 0  && tempPoint.y<obstacle.row*50+100) {
                    gameData.ball.delta.x *= -1
                } else {
                    gameData.ball.delta.y *= -1
                }

                // 충돌한 것은 remove 후 for loop 에서 나감
                gameData.obstacles.removeAt(indexOfObstacle)
                gameData.obstacleRemnant = obstacle.getCurrentLocation() //display 후 null로
                break
            }
        }
    }

    override fun processGameDataInServer(strAction : String, manualRedraw : Boolean) {
        Log.i("processGameDataInServer", "processGameDataInServer() $strAction")

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