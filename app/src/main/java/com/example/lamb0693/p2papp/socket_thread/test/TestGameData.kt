package com.example.lamb0693.p2papp.socket_thread.test

import com.google.gson.Gson

class TestGameData() {
    var serverX : Float = 200.0f
    var clientX : Float = 200.0f
    val barWidth : Float = 80.0F
    val barHeight : Float = 10.0F

    val serverY : Float = 580f
    val clientY : Float = 20f

    var ballX : Float = 100.0F
    var ballY : Float = 100.0F
    var ballMoveX : Float = 5.0F
    var ballMoveY : Float = 5.0F
    val ballRadius : Float = 15.0F

    private fun toJson(): String {
        val gson = Gson()
        return gson.toJson(this)
    }

    fun getStringToSendViaSocket(): String {
        val json = toJson()
        return "GAME_DATA$json"
    }

    companion object {
        fun fromString(string: String): TestGameData? {
            if (string.startsWith("GAME_DATA")) {
                val json = string.substringAfter("GAME_DATA")
                val gson = Gson()
                return gson.fromJson(json, TestGameData::class.java)
            }
            return null
        }
    }
}