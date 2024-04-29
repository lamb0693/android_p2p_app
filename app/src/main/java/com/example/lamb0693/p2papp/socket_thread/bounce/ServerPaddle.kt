package com.example.lamb0693.p2papp.socket_thread.bounce

import android.graphics.PointF
import android.util.Log
import com.example.lamb0693.p2papp.R
import kotlin.math.abs

class ServerPaddle : Paddle() {
    init {
        y = 470f
        imageResource = R.drawable.paddle_server
        isServer = true
        collisionBorder = y - paddleHeight/2f
        setPaddleState(0)
    }

    override fun passCollisionBorder(ball : Ball, currentPoint : PointF) : Boolean {
        // passed downward? 볼 엉덩이가 선을 지나갔는 지
        Log.i("passCollisionBorder", "checking pass collision border : $collisionBorder")
        if(ball.pos.y + ball.radius >= collisionBorder ) return false // 출발점이 접촉면이면 pass가 안됨
        if(currentPoint.y + ball.radius <  collisionBorder ) return false  // 밖에서 출발 접촉선에 물리면 pass로 판정
        Log.i("passCollisionBorder", "passed through Line")
        Log.i("passCollisionBorder", "prev ball.pos ${ball.pos}")
        Log.i("passCollisionBorder", "ball radius, ${ball.radius}")
        Log.i("passCollisionBorder", "temp.pos $currentPoint")
        return true
    }


    // currentPoint가 새로 설정됨, prevPoint은 변화가 없다
    // 돌아 나온 부분의 비율을 return 함
    override fun moveBackToCollisionBorder(ball : Ball, currentPoint : PointF): Float {
        //Log.i(">>>>", "moveBack")
        Log.i(">>>>", "ball delta info ${ball.delta}")
        val ballLower = currentPoint.y + ball.radius
        Log.i(">>>>", "ballLower $ballLower")
        val toMoveY = ballLower - collisionBorder // 양수, 원래 방향
        Log.i(">>>>", "toMoveY $toMoveY")
        val fractionProceeded = abs(toMoveY/ball.delta.y)
        val toMoveX = ball.delta.x * fractionProceeded // 원래 방향, currentMove 와 같은 바얗ㅇ
        //Log.i(">>>>", "toMoveX $toMoveX")
        currentPoint.x -= toMoveX // 같은 방향이니 빼 주어야 함
        currentPoint.y -= toMoveY // 위로 가니 빼주어야 함
        Log.i(">>>>", "moved back to surface $currentPoint")

        Log.i(">>>>", "fractionProceeded = $fractionProceeded")
        return fractionProceeded
    }

}