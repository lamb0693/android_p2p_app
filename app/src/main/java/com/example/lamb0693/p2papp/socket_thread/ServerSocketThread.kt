package com.example.lamb0693.p2papp.socket_thread

import android.content.Context
import android.util.Log
import com.example.lamb0693.p2papp.Constant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class ServerSocketThread (private val context: Context, private val messageCallback: ThreadMessageCallback) : Thread(){
    private var serverSocket: ServerSocket? = null
    private var connectedSocket : Socket? = null

    private var outputStream: OutputStream? = null
    private var inputStream : InputStream? = null
    private var isRunning = true

    private var charX = 100
    override fun run() {
        Log.i(">>>>", "ServerSocketThread Thread Started")

        try {
            serverSocket = ServerSocket(Constant.PORT_NUMBER)

            serverSocket?.also { serverSocket1 ->
                connectedSocket = serverSocket1.accept()
                Log.i(">>>>" , "server socket ; Accepted  clientSocket = $connectedSocket")
                connectedSocket?.also {
                    inputStream = it.getInputStream()
                    outputStream = it.getOutputStream()

                    sendMessageToSocket("hello from server through socket")

                    while(isRunning){
                        val buffer = ByteArray(1024)
                        val bytesRead = inputStream?.read(buffer)
                        if (bytesRead != null && bytesRead > 0) {
                            val receivedMessage = String(buffer, 0, bytesRead)
                            // Handle the received message
                            Log.i(">>>>",  "ReceivedMessage : $receivedMessage")
                            if(receivedMessage == "CLICKED"){
                                charX +=10
                                sendGameDataToFragments()
                            } else {
                                messageCallback.onMessageReceivedFromThread(receivedMessage)
                            }
                            if(receivedMessage == "quit") isRunning = false
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            outputStream?.close()
            inputStream?.close()
            connectedSocket?.close()
            serverSocket?.close()
        }
        Log.i(">>>>", "Server Thread terminating...")
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

    // main Thread에서 client에게 message 보내는 용도
    fun sendMessageFromMainThread(message : String) {
        CoroutineScope(Dispatchers.IO).launch {
            sendMessageToSocket(message)
        }
    }

    // server역할의 main Thread에서 server 에 data 전달
    fun onMessageFromMain(message : String) {
        if(message == "CLICKED"){
            charX +=10
            sendGameDataToFragments()
        }
    }

    private fun sendGameDataToFragments(){
        synchronized(this){
            messageCallback.onMessageReceivedFromThread("charx:$charX")
            sendMessageToSocket("charx:10")
        }
    }

}