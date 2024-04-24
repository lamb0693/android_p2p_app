package com.example.lamb0693.p2papp.socket_thread.test

import android.graphics.PointF
import com.google.gson.Gson

class TestGameData() {

    val serverPaddle = ServerPaddle()
    val clientPaddle = ClientPaddle()

    var ballX : Float = 100.0F
    var ballY : Float = 100.0F
    var ballMoveX : Float = 5.4F
    var ballMoveY : Float = 5.4F
    var ballRadius : Float = 15.0F

    var obstacles =  mutableListOf<Obstacle>()
    var obstacleRemnant : PointF? = null // 1000 not exist

    // player가 get 한 effect 및 남은 시간
    var effectServer : Int? = null
    var effectRemainServer  = 0
    var effectClient : Int? = null
    var effectRemainClient = 0
    var isServerPlaying : Boolean? = null

    init {
        // normal paddle로 설정
        serverPaddle.setPaddleState(0)
        clientPaddle.setPaddleState(0)
    }
    fun resetData() {
        serverPaddle.reset()
        clientPaddle.reset()

        ballX = 100.0F
        ballY = 100.0F

        ballMoveX = 5.0F
        ballMoveY = 5.0F

        effectServer = null
        effectRemainServer = 0
        effectClient = null
        effectRemainClient = 0
        isServerPlaying = null
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