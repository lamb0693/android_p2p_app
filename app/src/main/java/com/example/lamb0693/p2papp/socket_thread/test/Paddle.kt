package com.example.lamb0693.p2papp.socket_thread.test

import android.graphics.PointF
import com.example.lamb0693.p2papp.R

abstract class Paddle {
    var y : Float = 0f // 상속 class 에서 재정의
    var x : Float = 200f
    var imageResource : Int = 0
    var isServer : Boolean = true // 상속 class에서 재정의
    open val paddleHeight = 20f

    var collisionBorder : Float = 0f // 상속 class에서 재정의

    private var paddleState : Int = 0 // normal 0, Large 1, Small 0
    open var paddleWidth : Float = 80f //paddleState() 에서만 변경 가능

    open fun setPaddleState(state : Int) {
        paddleState = state
        paddleWidth = when(paddleState) {
            0 -> 80f
            1 -> 100f
            2 -> 60f
            else -> 0f
        }
        if(x < paddleWidth/2) x = paddleWidth/2
        if(x > (TestGameCons.BITMAP_WIDTH - paddleWidth/2f)) x=TestGameCons.BITMAP_WIDTH - paddleWidth/2f
    }

    fun getDrawingPoint(scaleX : Float, scaleY : Float) : PointF{
        return PointF( (x-paddleWidth/2)*scaleX, (y-paddleHeight/2)*scaleY)
    }


    fun getPaddleState() : Int {
        return this.paddleState
    }

    fun move(amount : Int) {
        x += amount
        if(x < paddleWidth/2) x = paddleWidth/2
        if(x > (TestGameCons.BITMAP_WIDTH - paddleWidth/2f)) x= TestGameCons.BITMAP_WIDTH - paddleWidth/2f
    }
    open fun getRect() {}
    open fun getCollisionBorder() {}
    abstract fun passCollisionBorder(prevPoint : PointF, currentPoint : PointF,
             ballRadius : Float) : Boolean
    //currentPoint is to be Changed
    abstract fun moveBackToCollisionBorder(prevPoint : PointF, currentPoint : PointF,
             ballRadius : Float , currentMoveX : Float, currentMoveY : Float) : Float

    // collision 한계를 -10 뭉 + 10pixel 넓게 잡음. 시각적 문제 교정
    fun isOnTheCollisionLine(ball : PointF) : Boolean {
        return ball.x > (x - paddleWidth/2f - 10) && ball.x < (x + paddleWidth/2f + 10)
    }

    // -0.1 정도 에서 + 1.1 정도 까지 나올 듯 -: left  + : right
    fun getPointOfCollisionLine(curX : Float) : Float{
        return ( curX - (x-paddleWidth/2f) )/paddleWidth
    }
    open fun reset() {
        x= 200f
        setPaddleState(0)
    }
}