package com.example.lamb0693.p2papp.socket_thread

import android.content.Context
import android.util.Log
import com.example.lamb0693.p2papp.Constant
import com.example.lamb0693.p2papp.viewmodel.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

open class ClientSocketThread (private val host : InetSocketAddress,
                               private val messageCallback: ThreadMessageCallback
) : Thread() {
    private var socket : Socket? =null
    private var outputStream: OutputStream? = null
    private var inputStream : InputStream? = null
    @Volatile private var isRunning = true
    override fun run() {
        Log.i(">>>>", "Client Thread Started")

        try {
            socket = Socket()
            socket?.connect(host, 10000)
            if(socket == null) {
                Log.e(">>>>" , "client : fail to connect server")
                return
            }

            outputStream = socket?.getOutputStream()
            inputStream = socket?.getInputStream()

            messageCallback.onThreadStarted()
            messageCallback.onConnectionMade()

            // Send message to the server (group owner)
            sendMessageToServerViaSocket("Hello.. from client")

            CoroutineScope(Dispatchers.IO).launch {
                while (isRunning) {
                    sendMessageToServerViaSocket("HEARTBEAT")
                    delay(Constant.HEART_BEAT_INTERVAL)
                }
            }

            socket?.soTimeout = 100  //inputStream.read에서 block 되는 것을 막아줌
            val buffer = ByteArray(1024)
            while(isRunning){
                try{
                    val bytesRead = inputStream?.read(buffer)
                    if (bytesRead != null && bytesRead > 0) {
                        val receivedMessage = String(buffer, 0, bytesRead)

                        val messages = receivedMessage.split("\n")
                        for (msg in messages){
                            if(msg.isNotBlank()){
                                if(msg.startsWith("GAME_DATA")){
                                    messageCallback.onGameDataReceivedFromServerViaSocket(msg)
                                } else {
                                    Log.i(">>>>",  "ClientThread ReceivedMessage : $msg")
                                    when(msg) {
                                        "SERVER_STARTED_GAME" -> messageCallback.onGameStateFromServerViaSocket(GameState.STARTED)
                                        "SERVER_PAUSED_GAME" -> messageCallback.onGameStateFromServerViaSocket(GameState.PAUSED)
                                        "SERVER_RESTARTED_GAME" -> messageCallback.onGameStateFromServerViaSocket(GameState.STARTED)
                                        "SERVER_STOPPED_GAME" -> messageCallback.onGameStateFromServerViaSocket(GameState.STOPPED)
                                        "SERVER_WIN" -> messageCallback.onGameWinnerFromServerViaSocket(true)
                                        "CLIENT_WIN" -> messageCallback.onGameWinnerFromServerViaSocket(false)
                                        else -> messageCallback.onOtherMessageReceivedFromServerViaSocket(msg)
                                    }
                                }
                            }
                        }
                    } else {
                        isRunning = false
                    }
                } catch (e : SocketTimeoutException) {
                    //Log.e(">>>>", "socket Timeout ${e.message}")
                    continue
                } catch (e : Exception) {
                    Log.e(">>>>", "Exception in while ${e.message}")
                    isRunning = false
                }
            }
        } catch (e: Exception) {
            Log.e(">>>>", "client socket thread : ${e.message}")
        }finally {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        }

        Log.i(">>>>", "Client Thread terminating...")
        // main activity 에 thread 종료 알림
        messageCallback.onThreadTerminated()
    }

    // Thread 에서 ServerThread로 message 보냄
    private fun sendMessageToServerViaSocket(message: String): Unit {
        try{
            if (socket != null && socket!!.isConnected) {
                CoroutineScope(Dispatchers.IO).launch {
                    outputStream?.write(message.toByteArray())
                    //Log.i(">>>>", "sendMessageToServerViaSocket@ClientSocketThred sended message $message to ClientSocket")
                }
            }
        } catch(e:Exception) {
            Log.e(">>>>","sendMessage in socket thread : ${e.message}")
        }
    }

    // Client 용 Fragment 에서 Server 로 보내는 메시지 가 옴
    fun onMessageFromClientToServer(message : String) {
        Log.i(">>>>","onMessageFromClientToServer@clintSocketThread : $message")
        CoroutineScope(Dispatchers.IO).launch {
            sendMessageToServerViaSocket(message)
        }
    }

    fun onQuitMessageFromFragment() {
        isRunning = false
        Log.i(">>>>", "onQuitMessageFromFragment, set isRunning false $isRunning")
    }
}