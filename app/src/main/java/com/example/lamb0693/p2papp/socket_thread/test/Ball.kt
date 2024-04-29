package com.example.lamb0693.p2papp.socket_thread.test

import android.graphics.PointF
import android.util.Log
import kotlin.random.Random

class Ball {
    var pos : PointF = PointF(
        Random.nextInt(100, 301).toFloat(), // Random x within range [100, 300]
        Random.nextInt(100, 401).toFloat() // Random y within range [100, 400]
    )
    var delta : PointF = PointF(
        if (Random.nextBoolean()) 5.4f else -5.4f, // Randomly choose between -5.4 and 5.4 for x
        if (Random.nextBoolean()) 5.4f else -5.4f  // Randomly choose between -5.4 and 5.4 for y
    )
    var radius : Float = TestGameCons.BALL_SIZE_NORMAL // 15, 20
    var spped : Float = TestGameCons.BALL_SPEED_INITIAL // 0.8  1.2

    fun move() {
        pos.x += delta.x * spped
        pos.y += delta.y * spped
    }

    // 가상으로 이동한 좌표만 return
    fun testMove() : PointF{
        return PointF(pos.x+ delta.x*spped, pos.y+delta.y*spped)
    }

    // bitmap 그릴때 그리기 시작하는 Point
    fun getDrawPoint(scaleX : Float, scaleY : Float) : PointF {
        return PointF( (pos.x-radius) * scaleX, (pos.y-radius) * scaleY )
    }

    fun resetBall() {
        pos.x =  Random.nextInt(100, 301).toFloat()
        pos.y =  Random.nextInt(200, 301).toFloat()
        delta.x = if (Random.nextBoolean()) 5.4f else -5.4f
        delta.y = if (Random.nextBoolean()) 5.4f else -5.4f
        radius  = TestGameCons.BALL_SIZE_NORMAL // 15
        spped = 1f
    }

    fun getSizeIndex() : Int{
        return when(radius) {
            TestGameCons.BALL_SIZE_SMALL -> 0
            TestGameCons.BALL_SIZE_NORMAL -> 1
            TestGameCons.BALL_SIZE_LARGE -> 2
            else -> {
                Log.e(">>>>", "wrong parameter  getting ball size")
                1
            }
        }
    }

    fun setSizeIndex(size : Int) {
        when(size) {
            0 -> radius = TestGameCons.BALL_SIZE_SMALL
            1 -> radius = TestGameCons.BALL_SIZE_NORMAL
            2 -> radius = TestGameCons.BALL_SIZE_LARGE
            else -> {
                Log.e(">>>>", "error setting ball size")
            }
        }
    }
}