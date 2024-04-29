package com.example.lamb0693.p2papp.socket_thread.bounce

import android.graphics.PointF
import com.google.gson.Gson

class BounceData() {

    val serverPaddle = ServerPaddle()
    val clientPaddle = ClientPaddle()

    val ball  = Ball()

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
        serverPaddle.resetPaddle()
        clientPaddle.resetPaddle()

        ball.resetBall()

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
        fun fromString(string: String): BounceData? {
            if (string.startsWith("GAME_DATA")) {
                val json = string.substringAfter("GAME_DATA")
                val gson = Gson()
                return gson.fromJson(json, BounceData::class.java)
            }
            return null
        }
    }
}