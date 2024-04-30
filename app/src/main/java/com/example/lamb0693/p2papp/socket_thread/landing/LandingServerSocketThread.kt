package com.example.lamb0693.p2papp.socket_thread.landing

import com.example.lamb0693.p2papp.socket_thread.ServerSocketThread
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback
import com.example.lamb0693.p2papp.socket_thread.bounce.BounceCons

class LandingServerSocketThread (
    private val messageCallback: ThreadMessageCallback,
) : ServerSocketThread(messageCallback){

    override var timerInterval : Long = LandingCons.REDRAW_INTERVAL

    override fun proceedGame() {
        super.proceedGame()
    }

    override fun processGameDataInServer(strAction: String, manualRedraw: Boolean) {
        super.processGameDataInServer(strAction, manualRedraw)
    }

    override fun sendGameDataToFragments() {
        super.sendGameDataToFragments()
    }
}