package com.example.lamb0693.p2papp.fragment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.aware.WifiAwareNetworkInfo
import android.net.wifi.aware.WifiAwareNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import com.example.lamb0693.p2papp.Constant
import com.example.lamb0693.p2papp.MainActivity
import com.example.lamb0693.p2papp.R
import com.example.lamb0693.p2papp.SimpleConfirmDialog
import com.example.lamb0693.p2papp.viewmodel.TestViewModel
import com.example.lamb0693.p2papp.fragment.interfaces.FragmentTransactionHandler
import com.example.lamb0693.p2papp.socket_thread.ClientSocketThread
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback
import com.example.lamb0693.p2papp.socket_thread.test.TestGameData
import com.example.lamb0693.p2papp.socket_thread.test.TestServerSocketThread
import com.example.lamb0693.p2papp.viewmodel.GameState
import java.net.InetSocketAddress

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TestFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TestFragment : Fragment(), ThreadMessageCallback {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    // socketConnected와, GameStatus를 위한 ViewModel
    private lateinit var testViewModel : TestViewModel

    //Fragment를 Activity에 전달하기 위한 View
    private lateinit var testView : View

    // homeFragment는 항상 Activity에 남아 있음
    private var homeFragment: HomeFragment? = null
    // MainActiviy의 함수 실행용
    private var fragmentTransactionHandler : FragmentTransactionHandler? = null

    // Game 화면을 그리기 위한 View
    private lateinit var testGameView: TestGameView

    private var buttonStart : Button?= null
    private var buttonToHome : Button? = null

    lateinit var mainActivity : MainActivity

    private lateinit var connectivityManager : ConnectivityManager

    //socketThread
    private lateinit var serverSocketAddress : InetSocketAddress
    private var serverSocketThread : TestServerSocketThread? =  null
    private var clientSocketThread : ClientSocketThread? =  null

    private var networkCallback : ConnectivityManager.NetworkCallback? =null

    /***********************************
     * TestGameView
     **********************************/
    class TestGameView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {

        private val paint: Paint = Paint()

        private lateinit var backgroundBitmap: Bitmap
        private lateinit var offscreenBitmap: Bitmap
        private lateinit var offscreenCanvas: Canvas
        private lateinit var offscreenBitmapRect : Rect
        companion object{
            private const val BITMAP_WIDTH = 400
            private const val BITMAP_HEIGHT = 600
            private const val WALL_THICKNESS = 5
        }

        private var scaleX: Float = 1.0f
        private var scaleY: Float = 1.0f

        var gameData = TestGameData()

        init {
            paint.color = Color.BLUE // Change color as needed
            paint.style = Paint.Style.FILL

            //400X600 image
            backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)

            // Calculate scaling factors to adjust bitmap size based on device screen size
            scaleX = w.toFloat() / BITMAP_WIDTH // DESIRED_WIDTH is the width you want your game graphics to be
            scaleY = h.toFloat() / BITMAP_HEIGHT // DESIRED_HEIGHT is the height you want your game graphics to be

            // Determine the scaled bitmap dimensions
            val scaledWidth = (BITMAP_WIDTH * scaleX).toInt()
            val scaledHeight = (BITMAP_HEIGHT * scaleY).toInt()

            // Initialize the offscreen bitmap and canvas with scaled dimensions
            offscreenBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
            offscreenBitmapRect = Rect(0, 0, offscreenBitmap.width, offscreenBitmap.height )
            offscreenCanvas = Canvas(offscreenBitmap)
        }

        private fun drawGame(canvas : Canvas){
            canvas.drawBitmap(backgroundBitmap, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), null)
            // Draw whatever you want here for your game view
            // Make sure to scale your graphics based on the scaleX and scaleY factors
            val scaledServerX = (gameData.serverX - gameData.barWidth/2) * scaleX
            val scaledServerY = (gameData.serverY - gameData.barHeight/2) * scaleY
            val scaledClientX = (gameData.clientX - gameData.barWidth/2) * scaleX
            val scaledClientY = (gameData.clientY - gameData.barHeight/2) * scaleY
            val scaledBallX = gameData.ballX * scaleX
            val scaledBallY = gameData.ballY * scaleY
            val scaledBallRadius = gameData.ballRadius * scaleX // Assuming same scale factor for x and y

            paint.color = Color.BLUE
            canvas.drawRect(scaledServerX, scaledServerY,
                scaledServerX + gameData.barWidth * scaleX, scaledServerY + gameData.barHeight * scaleY, paint)
            paint.color = Color.RED
            canvas.drawRect(scaledClientX, scaledClientY,
                scaledClientX + gameData.barWidth * scaleX, scaledClientY + gameData.barHeight * scaleY, paint)
            paint.color = Color.BLACK
            canvas.drawCircle(scaledBallX, scaledBallY, scaledBallRadius, paint)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            offscreenCanvas.drawColor(Color.WHITE) // Clear the offscreen canvas
            drawGame(offscreenCanvas)

            canvas.drawBitmap(offscreenBitmap, null, offscreenBitmapRect, null)
        }
    }

    /***********************************
     * TestFragment
     **********************************/
    private fun initViewModel() {
        testViewModel = ViewModelProvider(this)[TestViewModel::class.java]

        testViewModel.socketConnected.observe(viewLifecycleOwner){
            val imageViewSocketConnected = testView.findViewById<ImageView>(R.id.imageSocketConnectStatus)
            if(it) imageViewSocketConnected.setImageResource(R.drawable.custom_socket_connection_on)
            else imageViewSocketConnected.setImageResource(R.drawable.custom_socket_connection_off)
        }

        testViewModel.gameState.observe(viewLifecycleOwner) {
            when(it) {
                GameState.STOPPED -> buttonStart?.text = getString(R.string.start)
                GameState.STARTED -> buttonStart?.text = getString(R.string.pause)
                GameState.PAUSED  -> buttonStart?.text = getString(R.string.restart)
                null -> return@observe
            }
        }
    }
    private fun initTestViewButtonAndListener(){
        buttonToHome = testView.findViewById<Button>(R.id.buttonToHomeFromTest)
        if(buttonToHome == null) Log.e(">>>>", "buttonToHome null")

        buttonStart = testView.findViewById<Button>(R.id.buttonStartTestGame)
        if(buttonStart == null) Log.e(">>>>", "buttonStart null")
        buttonStart?.isEnabled = false

        buttonToHome?.setOnClickListener{
            if(testViewModel.socketConnected.value == true){
                if(mainActivity.asServer!!) serverSocketThread?.quitServerThread()
                else clientSocketThread?.onQuitMessageFromFragment()
            } else{
                homeFragment?.let { fragment ->
                    fragmentTransactionHandler?.onChangeFragment(fragment, "HomeFragment")
                }
            }
        }

        // button click 하면 sever에 message 보내기만,  ui 처리는 server에서 돌아오면 한다.
        buttonStart?.setOnClickListener{
            buttonStart?.isEnabled = false
            if(testViewModel.gameState.value == GameState.STOPPED) {
                if(mainActivity.asServer!!) serverSocketThread?.setServerPrepared()
                else clientSocketThread?.onMessageFromClientToServer("CLIENT_PREPARED_GAME")
            } else if(testViewModel.gameState.value == GameState.STARTED) {
                if (mainActivity.asServer!!) serverSocketThread?.setPauseGameRoutine(true)
                else clientSocketThread?.onMessageFromClientToServer("CLIENT_PAUSED_GAME")
            } else if(testViewModel.gameState.value == GameState.PAUSED) {
                if (mainActivity.asServer!!) serverSocketThread?.setPauseGameRoutine(false)
                else clientSocketThread?.onMessageFromClientToServer("CLIENT_RESTART_GAME")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        testView = inflater.inflate(R.layout.fragment_test, container, false)
        testGameView = testView.findViewById(R.id.viewTestGame)

        connectivityManager = mainActivity.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager

        initViewModel()
        initTestViewButtonAndListener()

        return testView
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // asServer -> initServerSocket else connectToServerSocket
        if(mainActivity.asServer!!) {
            initServerSocket()  // accept()에서 block됨
            mainActivity.sendMessageViaSession("INVITATION")
        }
        else connectToServerSocket()

        // Game Control용 Listerner
        initGameInterfaceListener()
    }

    private fun initGameInterfaceListener() {
        testGameView.setOnClickListener{
            if(mainActivity.asServer!!){
                serverSocketThread?.onGameDataFromServerFragment("ACTION:SERVER_RIGHT")
            }else {
                clientSocketThread?.onMessageFromClientToServer("ACTION:CLIENT_RIGHT")
            }
        }
    }

    fun setHomeFragment(fragment: HomeFragment?) {
        if(fragment == null) {
            Log.e(">>>>", "homeFragment is null  in setHomeFragment()")
        }
        homeFragment = fragment
    }

    // Attach 될때 MainActivity관련 변수 설정
    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            fragmentTransactionHandler = context as FragmentTransactionHandler
            fragmentTransactionHandler?.onEnableButtonSetting(false)
            mainActivity = context as MainActivity
        } catch (e : Exception){
            Log.e(">>>>", "onAttach ${e.message}")
            throw RuntimeException("$context must implement FragmentTransactionHandler and MainActivity")
        }
    }

    // Detach()될때 NetworkCallback 을 꼭 제거해야 됨 - 아니면 다음에 다른 callback이 안 붙음
    override fun onDetach() {
        super.onDetach()
        networkCallback?.let{
            Log.i(">>>>", "unregistering network Callback")
            connectivityManager.unregisterNetworkCallback(it)
        }
    }

    fun cancelInitServerSocket(){
        try{
            // network 초기화 를 reset --> detach()에서
            homeFragment?.let { fragment ->
                fragmentTransactionHandler?.onChangeFragment(fragment, "HomeFragment")
            }
        } catch (e : Exception) {
            Log.e(">>>>", "error to cancelInitServerSocket() : ${e.message}")
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun initServerSocket(){
        if(mainActivity.asServer == null || mainActivity.asServer==false) {
            Log.e(">>>>", "asServer ==null || asServer==false in initServerSocket()")
            return
        }

        if(mainActivity.publishDiscoverySession == null || mainActivity.currentPeerHandle == null){
            Log.e(">>>>", "publishDiscoverySession ==null || peerHandle == null in initServerSocket()")
            return
        }

        Log.i(">>>>", "init serverSocket")

        serverSocketThread = TestServerSocketThread(this@TestFragment)

        // WifiAwareNetworkSpecifier 생성
        val networkSpecifier = WifiAwareNetworkSpecifier.Builder(
            mainActivity.publishDiscoverySession!!, mainActivity.currentPeerHandle!!)
            .setPort(serverSocketThread!!.getLocalPort())
            .setPskPassphrase(Constant.PSK_PARAPHRASE).build()

        // WifiAware 를 이용 하는 NetworkRequest 생성
        val myNetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            .setNetworkSpecifier(networkSpecifier)
            .build()
        Log.i(">>>>", "networkRequest :  $myNetworkRequest in initServerSocket()")

        // 콜백 만들고 등록
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i(">>>>", "NetworkCallback onAvailable")
                //Toast.makeText(mainActivity, "Socket network available", Toast.LENGTH_LONG).show()
                try{
                    serverSocketThread?.start()
                } catch ( e : Exception){
                    Log.e(">>>>", "starting socket thread exception : ${e.message}")
                }
            }
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Log.i(">>>>", "NetworkCallback onCapabilitiesChanged network : $network")
            }
            override fun onLost(network: Network) {
                super.onLost(network)
                Log.i(">>>>", "NetworkCallback onLost")
                // 필요 없을 듯 removeCurrentSocketConnection()
            }
        }

        networkCallback?.let{
            connectivityManager.requestNetwork(myNetworkRequest, it)
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToServerSocket() {
        if(mainActivity.asServer == null || mainActivity.asServer==true){
            Log.e(">>>>", "asServer ==null || asServer==true in connectToServerSocket()")
            return
        }

        if(mainActivity.subscribeDiscoverySession == null || mainActivity.currentPeerHandle == null) {
            Log.e(">>>>", "subscribeDiscoverySession ==null || peerHandle == null in connectToServerSocket()")
            return
        }

        Log.i(">>>>", "init connectToServerSocket")

        val networkSpecifier = WifiAwareNetworkSpecifier.Builder(
            mainActivity.subscribeDiscoverySession!!, mainActivity.currentPeerHandle!!)
            .setPskPassphrase(Constant.PSK_PARAPHRASE)
            .build()

        Log.i(">>>>", "connecting to server socket $networkSpecifier")

        val myNetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            .setNetworkSpecifier(networkSpecifier)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i(">>>>", "NetworkCallback onAvailable")
            }
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Log.i(">>>>", "NetworkCallback onCapabilitiesChanged network : $network")

                val peerAwareInfo = networkCapabilities.transportInfo as WifiAwareNetworkInfo
                serverSocketAddress =InetSocketAddress(peerAwareInfo.peerIpv6Addr, peerAwareInfo.port)

                //Toast.makeText(mainActivity, "Got Server Address\nStarting client socket thread", Toast.LENGTH_LONG).show()

                if(clientSocketThread == null){
                    try{
                        clientSocketThread = ClientSocketThread(serverSocketAddress,this@TestFragment)
                        clientSocketThread?.start()
                    } catch(e : Exception){
                        Log.e(">>>>", "clientSocket : ${e.message}")
                    }
                }
            }
            override fun onLost(network: Network) {
                super.onLost(network)
                Log.i(">>>>", "NetworkCallback onLost")
                // 필요 없을 듯 removeCurrentSocketConnection()
                mainActivity.runOnUiThread{
                    testViewModel.setSocketConnected(false)
                }
            }
        }
        connectivityManager.requestNetwork(myNetworkRequest,
            networkCallback as ConnectivityManager.NetworkCallback
        )
    }

    /**********************************]
     * 기본적인 ThreadMessageCallback overload
     * Thread에서 실행 시킴 : runOnUiThread 필요
     ***********************************/
    override fun onConnectionMade() {
        mainActivity.runOnUiThread{
            testViewModel.setSocketConnected(true)
            Toast.makeText(mainActivity, "상대방과 게임에 연결 되었습니다", Toast.LENGTH_LONG).show()
            buttonStart?.isEnabled = true
        }
    }
    override fun onThreadStarted() {
        Log.i(">>>>", "from thread : started")
    }
    override fun onThreadTerminated() {
        Log.i(">>>>", "from thread : terminating")
        mainActivity.runOnUiThread {
            testViewModel.setSocketConnected(false)
            SimpleConfirmDialog(mainActivity, "알림",
                "Connection Lost, Return to Home").showDialog()

            homeFragment?.let { fragment ->
                fragmentTransactionHandler?.onChangeFragment(fragment, "HomeFragment")
            }
        }
    }


    /***************
     * ThreadMessageCallback overload  : GameStateMessage 처리
     * onGameStateMessageFromThread :  from server thread server만 해당
     * onGameStateFromServerViaSocket : from client thread  client만 해당
     * processGameStateChange
     */
    override fun onGameStateMessageFromThread(gameState: GameState) {
        if(mainActivity.asServer!!){
            Log.i(">>>>", "received GameState message from server thread")
            mainActivity.runOnUiThread{
                processGameStateChange(gameState)
            }

        }
    }

    //client는 server -> client thread통해 받고
    override fun onGameStateFromServerViaSocket(gameState: GameState) {
        if(!mainActivity.asServer!!){
            Log.i(">>>>", "received GameState message from client thread")
            mainActivity.runOnUiThread{
                processGameStateChange(gameState)
            }
        }
    }

    private fun processGameStateChange(gameState: GameState) {
        when(gameState){
            GameState.STARTED-> {
                Toast.makeText(mainActivity, "game started", Toast.LENGTH_LONG).show()
                testViewModel.setGameState(GameState.STARTED)
            }
            GameState.PAUSED-> {
                Toast.makeText(mainActivity, "game paused", Toast.LENGTH_LONG).show()
                testViewModel.setGameState(GameState.PAUSED)
            }
            GameState.STOPPED-> {
                Toast.makeText(mainActivity, "game stopped", Toast.LENGTH_LONG).show()
                testViewModel.setGameState(GameState.STOPPED)
            }
        }
        buttonStart?.isEnabled = true
    }

    /***************
     * ThreadMessageCallback overload : GameData 처리
     * onGameDataReceivedFromThread :  from server thread server only
     * onGameDataReceivedFromServerViaSocket : from client thread, client only
     * processGameData
     */
    override fun onGameDataReceivedFromThread(gameData: TestGameData) {
        if(mainActivity.asServer!!){
            Log.i(">>>>", "received gameData in TestFragment : $gameData")
            processGameData(gameData)
        } else {
            Log.e(">>>>", "onGameDataReceivedFromThread to client")
        }
    }
    override fun onGameDataReceivedFromServerViaSocket(strGameData: String) {
        if(mainActivity.asServer!!) {
            Log.e(">>>>", "onGameDataReceivedFromServerViaSocket to server")
        } else {
            val gameData = TestGameData.fromString(strGameData)
            Log.i(">>>>", "received gameData from server : $gameData")
            if (gameData is TestGameData) {
                processGameData(gameData)
            } else {
                Log.e(">>>>", "onGameDataReceivedFromServerViaSocket,  fail to Convert")
            }
        }
    }
    private fun processGameData(gameData: TestGameData) {
        testGameView.gameData = gameData
        testGameView.invalidate()
    }

    /*********************
     * Other Message From thread  :  client 만 해당
     * onOtherMessageReceivedFromServerViaSocket : Server 만 해당
     * onOtherMessageFromClientViaSocket
     **************************/
    override fun onOtherMessageReceivedFromServerViaSocket(receivedMessage: String) {
        if(mainActivity.asServer!!) return
        if(receivedMessage != "HEARTBEAT") Log.i(">>>>", "onOtherMessageReceivedFromServerViaSocket : $receivedMessage")
    }
    override fun onOtherMessageFromClientViaSocket(receivedMessage: String) {
        if(!mainActivity.asServer!!) return
        if(receivedMessage != "HEARTBEAT") Log.i(">>>>", "onOtherMessageFromClientViaSocket : $receivedMessage")
    }

    /**********************************
     * newInstance : companion object
     *********************************/
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TestFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic fun newInstance(param1: String, param2: String) =
            TestFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}