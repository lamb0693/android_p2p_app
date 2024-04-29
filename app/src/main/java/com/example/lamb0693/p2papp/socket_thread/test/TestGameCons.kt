package com.example.lamb0693.p2papp.socket_thread.test

import android.graphics.RectF

object TestGameCons {
    // 화면 refreshing interval
    const val TEST_GAME_INTERVAL = 100L

    // 사용자 인풋 refresh interval
    const val TOUCH_EVENT_INTERVAL = 80L

    const val OBSTACLE_REGEN_INTERVAL = 15
    //effect 관련
    const val EFFECT_DURATION = 200
    const val EFFECT_REMAIN_SIZE = 40

    // game original bitmap widh and height
    const val BITMAP_WIDTH = 400
    const val BITMAP_HEIGHT = 600

    const val BALL_SPEED_LOW = 0.9f
    const val BALL_SPEED_NORMAL = 1.15f
    const val BALL_SPEED_HIGH = 1.4f
    const val BALL_SPEED_INITIAL = 1.0f

    const val BALL_SIZE_SMALL = 10f
    const val BALL_SIZE_NORMAL = 15f
    const val BALL_SIZE_LARGE = 20f

    // Original controller
    const val CONTROLLER_WIDTH = 75
    const val CONTROLLER_HEIGHT = 60
    val CONTROLLER_RECT = RectF(162f, 520f, 237f, 580f)
    // screen 에서 controller 가 neutral 로 판정 받는 범위
    const val CONTROLLER_NEUTRAL_WIDTH = 10f

    const val SCORE_SIZE = 70
    const val PRINT_SCORE_BASELINE = 575
}