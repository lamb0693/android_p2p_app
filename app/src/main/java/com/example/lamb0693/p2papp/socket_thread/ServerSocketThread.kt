package com.example.lamb0693.p2papp.socket_thread

import android.util.Log
import com.example.lamb0693.p2papp.Constant
import com.example.lamb0693.p2papp.viewmodel.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException


open class ServerSocketThread(
    private val messageCallback: ThreadMessageCallback,
) : Thread(){
    // timerInterval 은 Extended class에서 값을 재 정이
    open var timerInterval : Long = 0L // timer가 0이면 수동

    private var serverSocket : ServerSocket? = null
    private var connectedSocket : Socket? = null

    private var outputStream: OutputStream? = null
    private var inputStream : InputStream? = null

    @Volatile open var isPaused = true
    @Volatile open var isRunning = true
    private  var portNo : Int = -1

    private var isServerPrepared = false
    private var isClientPrepared = false

    init {
        serverSocket = ServerSocket(0)
        portNo = serverSocket!!.localPort
    }

    override fun run() {
        Log.i(">>>>", "ServerSocketThread Thread Started")

        try {
            serverSocket?.also { serverSoc ->
                connectedSocket = serverSoc.accept()
                Log.i(">>>>" , "server socket ; Accepted  clientSocket = $connectedSocket")
                connectedSocket?.also {
                    inputStream = it.getInputStream()
                    outputStream = it.getOutputStream()
                    messageCallback.onConnectionMade()

                    sendMessageToClientViaSocket("Hello.. from Server")

                    // 자동 갱신이면
                    if(timerInterval > 0) {
                        CoroutineScope(Dispatchers.IO).launch {
                            while (isRunning) {
                                proceedGame()
                                sendGameDataToFragments()
                                delay(timerInterval)
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.IO).launch {
                            while (isRunning) {
                                sendMessageToClientViaSocket("HEARTBEAT")
                                delay(Constant.HEART_BEAT_INTERVAL)
                            }
                        }
                    }

                    connectedSocket?.soTimeout = 100  //inputStream.read에서 block 되는 것을 막아줌
                    val buffer = ByteArray(1024)
                    while(isRunning){
                        try{
                            val bytesRead = inputStream?.read(buffer)
                            if (bytesRead != null && bytesRead > 0) {
                                val receivedMessage = String(buffer, 0, bytesRead)
                                // Handle the received message
                                Log.i(">>>>",  "ServerThread ReceivedMessage : $receivedMessage")
                                if(receivedMessage.startsWith("ACTION")){
                                    processGameDataInServer(receivedMessage, (timerInterval==0L))  //true 이면 수동
                                } else {
                                    when(receivedMessage) {
                                        "CLIENT_PREPARED_GAME" -> {
                                            isClientPrepared = true
                                            if (isServerPrepared) startGameRoutine()
                                        }
                                        "CLIENT_PAUSED_GAME" -> {
                                            isPaused = true
                                            messageCallback.onGameStateMessageFromThread(GameState.PAUSED)
                                            sendMessageToClientViaSocket("SERVER_PAUSED_GAME")
                                        }
                                        "CLIENT_RESTART_GAME" -> {
                                            isPaused = false
                                            messageCallback.onGameStateMessageFromThread(GameState.STARTED)
                                            sendMessageToClientViaSocket("SERVER_RESTARTED_GAME")
                                        }
                                        else -> messageCallback.onOtherMessageFromClientViaSocket(receivedMessage)
                                    }
                                }
                            } else {
                                isRunning = false
                            }
                        }  catch (e : SocketTimeoutException) {
                            //Log.e(">>>>", "socket Timeout ${e.message}")
                            continue
                        } catch (e : Exception) {
                            Log.e(">>>>", "Excetpion in while ${e.message}")
                            isRunning = false
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

    fun getLocalPort() : Int {
        return this.portNo
    }

    fun sendMessageToClientViaSocket(message: String): Unit {
        try{
            if (connectedSocket != null && connectedSocket?.isConnected == true) {
                CoroutineScope(Dispatchers.IO).launch {
                    outputStream?.write(message.toByteArray())
                }
            }
        } catch(e:Exception) {
            Log.e(">>>>","sendMessageToClientViaSocket@serverSocketThread exception : ${e.message}")
        }
    }

    //fragment에서 직접 실행시킴
    fun quitServerThread() {
        isRunning = false
    }

    // framgemnt에서 직접 실행 시킴
    fun setServerPrepared(){
        isServerPrepared = true
        if(isClientPrepared) startGameRoutine()
    }

    private fun startGameRoutine() {
        isPaused = false
        sendMessageToClientViaSocket("SERVER_STARTED_GAME")  // client에는 message로 전달
        messageCallback.onGameStateMessageFromThread(GameState.STARTED) // server에는 직접 전달
        isServerPrepared = false
        isClientPrepared = false
    }

    // framgemnt에서 직접 실행 시킴
    fun setPauseGameRoutine(pause : Boolean){
        isPaused = pause
        if(isPaused) {
            sendMessageToClientViaSocket("SERVER_PAUSED_GAME")
            messageCallback.onGameStateMessageFromThread(GameState.PAUSED)
        }
        else {
            sendMessageToClientViaSocket("SERVER_RESTARTED_GAME")
            messageCallback.onGameStateMessageFromThread(GameState.STARTED)
        }
    }

    // server 역할의 Fragment 에서 server 에 GameData 전달
    fun onGameDataFromServerFragment(strAction: String) {
        processGameDataInServer(strAction, (timerInterval==0L))
    }

    open fun proceedGame() {
        //Extended class에서 override 해서 사용
    }

    // server 의 Game Data 를 모든 Fragement에 전달
    open fun sendGameDataToFragments(){
        //Extended class에서 override 해서 사용
    }

    // should Execute super function
    open fun processGameDataInServer(strAction : String, manualRedraw : Boolean) {
        //Extended class에서 override 해서 사용
    }

}