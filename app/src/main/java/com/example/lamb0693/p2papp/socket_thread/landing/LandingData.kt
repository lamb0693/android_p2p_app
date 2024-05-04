package com.example.lamb0693.p2papp.socket_thread.landing

import com.google.gson.Gson

class LandingData {
    var serverLander = Lander(true)
    var clientLander = Lander(false)

    fun resetData() {
        serverLander.resetLanderData()
        clientLander.resetLanderData()
    }

    private fun toJson(): String {
        val gson = Gson()
        return gson.toJson(this)
    }

    fun getStringToSendViaSocket(): String {
        val json = toJson()
        return "GAME_DATA$json"
    }

    companion object {
        fun fromString(string: String): LandingData? {
            if (string.startsWith("GAME_DATA")) {
                val json = string.substringAfter("GAME_DATA")
                val gson = Gson()
                return gson.fromJson(json, LandingData::class.java)
            }
            return null
        }
    }
}