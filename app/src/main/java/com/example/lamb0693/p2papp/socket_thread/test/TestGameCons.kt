package com.example.lamb0693.p2papp.socket_thread.test

import android.graphics.RectF

object TestGameCons {
    const val BAR_NORMAL = 0
    const val BAR_LARGE = 1
    const val BAR_SMALL = 2
    //const val BAR_HEIGHT : Float = 16.0F

    // 화면 refreshing interval
    const val TEST_GAME_INTERVAL = 100L

    // game original bitmap widh and height
    const val BITMAP_WIDTH = 400
    const val BITMAP_HEIGHT = 600

    // Original controller
    const val CONTROLLER_WIDTH = 75
    const val CONTROLLER_HEIGHT = 60
    val CONTROLLER_RECT = RectF(162f, 520f, 237f, 580f)
    // screen 에서 controller 가 neutral 로 판정 받는 범위
    const val CONTROLLER_NEUTRAL_WIDTH = 10f

    // 사용자 인풋 refresh interval
    const val TOUCH_EVENT_INTERVAL = 80L

    const val SCORE_SIZE = 70
    const val PRINT_SCORE_BASELINE = 575

    const val OBSTACLE_REGEN_INTERVAL = 15
    //effect 관련
    const val EFFECT_DURATION = 200
    const val EFFECT_REMAIN_SIZE = 40
}