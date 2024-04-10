package com.example.lamb0693.p2papp.socket_thread

import android.util.Log
import com.example.lamb0693.p2papp.Constant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket


open class ServerSocketThread(private val messageCallback: ThreadMessageCallback) : Thread(){
    private var serverSocket: ServerSocket? = null
    private var connectedSocket : Socket? = null

    private var outputStream: OutputStream? = null
    private var inputStream : InputStream? = null
    open var isRunning = true

    override fun run() {
        Log.i(">>>>", "ServerSocketThread Thread Started")

        try {
            serverSocket = ServerSocket(Constant.PORT_NUMBER)

            serverSocket?.also { serverSoc ->
                connectedSocket = serverSoc.accept()
                Log.i(">>>>" , "server socket ; Accepted  clientSocket = $connectedSocket")
                connectedSocket?.also {
                    inputStream = it.getInputStream()
                    outputStream = it.getOutputStream()
                    messageCallback.onConnectionMade()

                    sendMessageToClientViaSocket("Hello.. from Server")

                    while(isRunning){
                        val buffer = ByteArray(1024)
                        val bytesRead = inputStream?.read(buffer)
                        if (bytesRead != null && bytesRead > 0) {
                            val receivedMessage = String(buffer, 0, bytesRead)
                            // Handle the received message
                            Log.i(">>>>",  "ServerThread ReceivedMessage : $receivedMessage")
                            if(receivedMessage.contains("ACTION")){
                                processGameDataInServer(receivedMessage)
                            } else if(receivedMessage == "QUIT_THREAD") {
                                isRunning = false
                            }
                            else {
                                messageCallback.onMessageReceivedFromThread(receivedMessage)
                            }

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

    open fun sendMessageToClientViaSocket(message: String): Unit {
        try{
            CoroutineScope(Dispatchers.IO).launch {
                outputStream?.write(message.toByteArray())
            }
        } catch(e:Exception) {
            Log.e(">>>>","sendMessageToClientViaSocket@serverSocketThread exception : ${e.message}")
        }
    }

    // server Fragment에서 client에게 simple message 보내는 용도
    fun onMessageFromFragmentToClient(message : String) {
        CoroutineScope(Dispatchers.IO).launch {
            sendMessageToClientViaSocket(message)
        }
    }

    fun onQuitMessageFromFragment() {
        CoroutineScope(Dispatchers.IO).launch {
            sendMessageToClientViaSocket("QUIT_THREAD")
        }
        isRunning = false
    }

    // server 역할의 Fragment 에서 server 에 GameData 전달
    fun onGameDataFromServerFragment(strAction: String) {
        processGameDataInServer(strAction)
    }

    // server 의 Game Data 를 모든 Fragement에 전달
    open fun sendGameDataToFragments(){ }


    // should Execute super function
    open fun processGameDataInServer(strAction : String) {
    }

}