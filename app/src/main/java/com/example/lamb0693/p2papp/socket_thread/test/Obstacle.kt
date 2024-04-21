package com.example.lamb0693.p2papp.socket_thread.test

import android.graphics.RectF
import com.google.gson.Gson
import kotlin.random.Random

class Obstacle {
    var row : Int = Random.nextInt(0, 7)// 0-6
    var curPosX : Float = 0f

    var type : Int = Random.nextInt(0, 9)// 0-9
    var speed : Float = 5 * Random.nextFloat() + 5

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

    fun getScaledRect(scaleX : Float, scaleY : Float) : RectF{
        return RectF((curPosX-15) * scaleX, (row*50f + 100 -15)*scaleY,
            (curPosX+15)*scaleX, (row*50f + 100 +15)*scaleY)
    }

    fun move() {
        curPosX += speed
    }

}