package com.example.lamb0693.p2papp.socket_thread.landing

import android.graphics.PointF
import android.graphics.RectF

object LandingCons {
    const val REDRAW_INTERVAL = 70L
    // 사용자 인풋 refresh interval
    const val TOUCH_EVENT_INTERVAL = 40L

    val LANDER_SIZE = PointF(40f, 40f)
    val FLAME_SIZE = PointF(30f, 30f)
    val SIDE_FLAME_SIZE = PointF(20f, 20f)

    // buttonRect
    val LEFT_BUTTON_RECT = RectF(90f, 520f, 150f, 580f)
    val RIGHT_BUTTON_RECT = RectF(160f, 520f, 220f, 580f)
    val UP_BUTTON_RECT = RectF(260f, 520f, 330f, 580f)
}