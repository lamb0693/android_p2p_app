package com.example.lamb0693.p2papp.socket_thread.landing

import android.graphics.PointF
import android.util.Log
import kotlin.math.abs

class Lander(server : Boolean) {
    val pos = PointF(0f, 0f)
    val delta = PointF(0f, 0f)
    private val isServer : Boolean

    private val naturalDownForce = 0.2f
    private val resistance = 0.2f
    private var downForce = 0f
    private var sideForce = 0f

    private val maxDelta = PointF(3f, 7f)

    var isLeftFlame = false
    var isRightFlame = false
    var isLowerFlame = false

    init {
        this.isServer = server
        if(isServer) pos.x = 100f
        else pos.x = 300f
    }

    fun resetLanderData() {
        if(isServer) pos.x = 100f
        else pos.x = 300f
        pos.y = 20f

        delta.x = 0f
        delta.y = 0f

        downForce = 0.0f
        sideForce = 0f
    }

    fun move() {
        pos.x += delta.x
        pos.y += delta.y

        // side force 값에 따라 delta.x 변경
        delta.x += sideForce
        // downForce값에 따라 delta.y 변경
        delta.y += downForce
        Log.i("move", "sideForce : $sideForce")
        Log.i("move", "downForce : $downForce")
        Log.i("move", "delta : $delta")

        // delta 값에 한계를 둠
        delta.x = delta.x.coerceIn(-maxDelta.x, maxDelta.x)
        delta.y = delta.y.coerceIn(-maxDelta.y, maxDelta.y)
    }

    fun changeForce(left : Boolean, right : Boolean, upward : Boolean){
        if(left) sideForce -= 0.5f
        if(right) sideForce += 0.5f
        if(sideForce > resistance) sideForce -= resistance
        else if(sideForce < -resistance) sideForce += resistance
        else sideForce = 0f

        if(upward) downForce = -0.2f
        else downForce = naturalDownForce

        isRightFlame = left
        isLeftFlame = right
        isLowerFlame = upward
    }

    fun testMove() : PointF {
        return PointF(pos.x+delta.x, pos.y+delta.y)
    }

    fun getScaledDrawPosition(scaleX : Float, scaleY : Float) : PointF {
        return PointF((pos.x-LandingCons.LANDER_SIZE.x/2f)*scaleX,
            (pos.y-LandingCons.LANDER_SIZE.y/2f)*scaleY)
    }

    fun getScaledLowerFlameDrawPosition(scaleX: Float, scaleY: Float): PointF {
        return PointF((pos.x-LandingCons.FLAME_SIZE.x/2f)*scaleX,
            (pos.y+ LandingCons.LANDER_SIZE.y/2f)*scaleY)
    }
    fun getScaledLeftFlameDrawPosition(scaleX: Float, scaleY: Float): PointF {
        return PointF((pos.x-LandingCons.LANDER_SIZE.x/2f - LandingCons.SIDE_FLAME_SIZE.x)*scaleX,
            (pos.y - LandingCons.SIDE_FLAME_SIZE.y/2f)*scaleY)
    }

    fun getScaledRightFlameDrawPosition(scaleX: Float, scaleY: Float): PointF {
        return PointF((pos.x +LandingCons.LANDER_SIZE.x/2f )*scaleX,
            (pos.y - LandingCons.SIDE_FLAME_SIZE.y/2f)*scaleY)
    }

}