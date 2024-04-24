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
    var radius : Float = 15f  // 15, 20
    var spped : Float = 1f  // 0.8  1.2

    fun move() {
        pos.x += delta.x
        pos.y += delta.y
    }

    // 가상으로 이동한 좌표만 return
    fun testMove() : PointF{
        return PointF(pos.x+delta.x, pos.y+delta.y)
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
        radius  = 15f  // 15, 20
        spped = 1f  // 0.8  1.2
    }

    fun getSize() : Int{
        return when(radius) {
            10f -> 0
            15f -> 1
            20f -> 2
            else -> {
                Log.e(">>>>", "wrong parameter  getting ball size")
                1
            }
        }
    }

    fun setSize(size : Int) {
        when(size) {
            0 -> radius = 10f
            1 -> radius = 15f
            2 -> radius = 20f
            else -> {
                Log.e(">>>>", "error setting ball size")
            }
        }
    }
}