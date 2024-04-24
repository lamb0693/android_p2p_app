package com.example.lamb0693.p2papp.socket_thread.test

import android.graphics.PointF
import android.util.Log
import com.example.lamb0693.p2papp.R
import kotlin.math.abs

class ClientPaddle : Paddle(){

    init {
        y = 30f
        imageResource = R.drawable.paddle_client
        isServer = false
        collisionBorder = y + paddleHeight/2f // paddle의 아랫 면
        setPaddleState(0)
    }
    override fun passCollisionBorder(ball : Ball, currentPoint : PointF): Boolean {
        // passed upward 볼 위가  paddle의 아랫면을 지나갔는 지
        if(ball.pos.y - ball.radius <= collisionBorder ) return false // 출발점 체크, 패들보다 아래인지, 출발점이 접촉면이면 pass가 안됨
        if(currentPoint.y - ball.radius >  collisionBorder ) return false  //지난점 체크, 밖에서 출발 접촉선에 물리면 pass로 판정
        Log.i(">>>>", "prev ball, ballY, $ball")
        Log.i(">>>>", "temp ball $currentPoint")
        Log.i(">>>>", "passed through Line")
        return true
    }

    override fun moveBackToCollisionBorder(ball : Ball, currentPoint : PointF): Float {
        Log.i(">>>>", "moveBack")
        //Log.i(">>>>", "delta ${gameData.ballMoveX}, ${gameData.ballMoveY}")
        val ballUpper = currentPoint.y - ball.radius
        //Log.i(">>>>", "ballLower $ballLower")
        val toMoveY = collisionBorder - ballUpper// 양수, 이동할 방향
        //Log.i(">>>>", "toMoveY $toMoveY")
        val fractionProceeded = abs(toMoveY/ball.delta.y)
        val toMoveX = ball.delta.x * fractionProceeded // 원래 방향, currentMove 와 같은 바얗ㅇ
        //Log.i(">>>>", "toMoveX $toMoveX")
        currentPoint.x -= toMoveX // 같은 방향이니 빼 주어야 함
        currentPoint.y += toMoveY // 밑으로 가니 더해주어야 함
        Log.i(">>>>", "moved back to surface $currentPoint")

        Log.i(">>>>", "fractionProceeded = $fractionProceeded")
        return fractionProceeded
    }

}