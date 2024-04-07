package com.example.lamb0693.p2papp.socket_thread

interface ThreadMessageCallback {
    fun onMessageReceivedFromThread(message : String)
    fun onThreadTerminated()
    fun onThreadStarted()
}