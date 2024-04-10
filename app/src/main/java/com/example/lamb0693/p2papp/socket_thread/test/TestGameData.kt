package com.example.lamb0693.p2papp.socket_thread.test

import com.google.gson.Gson

data class TestGameData(
    var charX : Float,
    var charY : Float
) {
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