package com.example.lamb0693.p2papp.socket_thread

import android.content.Context
import android.util.Log
import com.example.lamb0693.p2papp.Constant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

class ServerSocketThread (private val context: Context, private val messageCallback: ThreadMessageCallback) : Thread(){
    private var serverSocket: ServerSocket? = null
    private var clientSocket : Socket? = null

    private var outputStream: OutputStream? = null
    private var inputStream : InputStream? = null
    private var isRunning = true
    override fun run() {
        Log.i(">>>>", "ServerSocketThread Thread Started")

        try {
            serverSocket = ServerSocket(Constant.PORT_NUMBER)

            serverSocket?.also { serverSocket1 ->
                clientSocket = serverSocket1.accept()
                Log.i(">>>>" , "server socket ; Accepted  clientSocket = $clientSocket")
                clientSocket?.also {
                    inputStream = it.getInputStream()
                    outputStream = it.getOutputStream()

                    sendMessage("hello from server through socket")

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
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            outputStream?.close()
            inputStream?.close()
            clientSocket?.close()
            serverSocket?.close()
        }
        Log.i(">>>>", "Server Thread terminating...")
        messageCallback.onThreadTerminated()
    }

    private fun sendMessage(message: String): Unit {
        try{
            val strMessage = "server : $message << via socket"
            outputStream?.write(strMessage.toByteArray())
        } catch(e:Exception) {
            Log.e(">>>>","sendMessage in socket thread : ${e.message}")
        }

    }

    fun sendMessageFromMainThread(message : String) {
        CoroutineScope(Dispatchers.IO).launch {
            sendMessage(message)
        }
    }
}