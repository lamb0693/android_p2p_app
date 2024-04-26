package com.example.lamb0693.p2papp.socket_thread.test

import android.graphics.PointF
import android.graphics.RectF
import com.google.gson.Gson
import kotlin.random.Random

class Obstacle {
    var row : Int = Random.nextInt(0, 7)// 0-6
    var curPosX : Float

    var type : Int = Random.nextInt(0, 9)// 0-9
    val positive = Random.nextBoolean()
    var speed: Float = if (positive) {
        5 * Random.nextFloat() + 5 // Range (5, 10)
    } else {
        -5 * Random.nextFloat() - 5 // Range (-10, -5)
    }

    init {
        curPosX = if(speed > 0 ) 0f
        else TestGameCons.BITMAP_WIDTH.toFloat()
    }

    fun toJson(): String {
        val gson = Gson()
        return gson.toJson(this)
    }

    companion object {
        fun fromJson(json: String): Obstacle {
            val gson = Gson()
            return gson.fromJson(json, Obstacle::class.java)
        }
    }

    fun getRect() : RectF {
        return RectF(curPosX-15, row*50f+100-15, curPosX+15, row*50f+100+15)
    }

    fun getScaledRect(scaleX : Float, scaleY : Float) : RectF{
        return RectF((curPosX-15) * scaleX, (row*50f + 100 -15)*scaleY,
            (curPosX+15)*scaleX, (row*50f + 100 +15)*scaleY)
    }

    fun getScaledDrawingPoint(scaleX : Float, scaleY : Float) : PointF {
        return  PointF(  (curPosX-15)* scaleX, (row*50f + 100 -15)*scaleY )
    }

    fun getUpperBorderY() : Float {
        return (row*50f + 100 -15)
    }

    fun getLowerBorderY() : Float{
        return (row*50f + 100 + 15)
    }

    fun getCurrentLocation() : PointF {
        return PointF(curPosX, row*50f + 100)
    }

    fun move() {
        curPosX += speed
    }

}