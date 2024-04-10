package com.example.lamb0693.p2papp.socket_thread.test

import com.example.lamb0693.p2papp.socket_thread.ClientSocketThread
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback
import java.net.InetSocketAddress

class TestClientSocketThread(private val host : InetSocketAddress,
                             private val messageCallback: ThreadMessageCallback
) : ClientSocketThread(host, messageCallback) {



}