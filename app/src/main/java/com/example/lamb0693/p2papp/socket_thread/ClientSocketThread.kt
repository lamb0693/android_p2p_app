package com.example.lamb0693.p2papp.socket_thread

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

class ClientSocketThread (val context: Context,
                          private val host : InetSocketAddress,
                          private val messageCallback: ThreadMessageCallback
) : Thread() {
    private lateinit var socket : Socket
    private var outputStream: OutputStream? = null
    private var inputStream : InputStream? = null
    private var isRunning = true
    override fun run() {
        Log.i(">>>>", "Client Thread Started")

        try {
            socket = Socket()
            socket.connect(host, 10000)
            Log.i(">>>>" , "client socket ; connected to server = $socket")

            outputStream = socket.getOutputStream()
            inputStream = socket.getInputStream()

            // main activity 에 thread 시작 알림
            messageCallback.onThreadStarted()

            // Send message to the server (group owner)
            sendMessageToSocket("hello from client through socket")

            while(isRunning){
                val buffer = ByteArray(1024)
                val bytesRead = inputStream?.read(buffer)
                if (bytesRead != null && bytesRead > 0) {
                    val receivedMessage = String(buffer, 0, bytesRead)
                    // Handle the received message
                    Log.i(">>>>",  "ReceivedMessage : $receivedMessage")

                    messageCallback.onMessageReceivedFromThread(receivedMessage)
                    if(receivedMessage == "quit") isRunning = false
                }
            }
        } catch (e: SocketTimeoutException) {
            // Handle timeout exception
            e.printStackTrace()
        } catch (e: Exception) {
            // Handle other exceptions
            e.printStackTrace()
        }finally {
            outputStream?.close()
            inputStream?.close()
            socket.close()
        }

        Log.i(">>>>", "Client Thread terminating...")
        // main activity 에 thread 종료 알림
        messageCallback.onThreadTerminated()
    }

    private fun sendMessageToSocket(message: String): Unit {
        try{
            //val strMessage = "client : $message << via socket"
            outputStream?.write(message.toByteArray())
        } catch(e:Exception) {
            Log.e(">>>>","sendMessage in socket thread : ${e.message}")
        }
    }

    fun sendMessageFromMainThread(message : String) {
        CoroutineScope(Dispatchers.IO).launch {
            sendMessageToSocket(message)
        }
    }
}