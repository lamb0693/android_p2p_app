package com.example.lamb0693.p2papp.fragment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
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
import android.view.MotionEvent
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
import com.example.lamb0693.p2papp.socket_thread.test.TestGameCons
import com.example.lamb0693.p2papp.socket_thread.test.TestGameData
import com.example.lamb0693.p2papp.socket_thread.test.TestServerSocketThread
import com.example.lamb0693.p2papp.viewmodel.GameState
import java.net.InetSocketAddress
import java.util.Timer
import java.util.TimerTask

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


    private var startStickX : Float = 0f // point to touch down
    private var currentPosition = PointF() // Store current position, judge whether isLeft or right
    // timer action of user periodically
    private var touchTimer: Timer? = null


    /***********************************
     * TestGameView
     **********************************/
    class TestGameView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {
        var serverWin = 0
        var clientWin = 0

        var gameData = TestGameData()  // class를 만드는 것은 의미 없음. 단지 만들어 놓음

        private val paint: Paint = Paint()

        private var backgroundBitmap : Bitmap
        private lateinit var offscreenBitmap: Bitmap
        private lateinit var offscreenCanvas: Canvas
        private lateinit var offscreenBitmapRect : Rect

        private var bitmapControllerInactive : Bitmap
        private var bitmapControllerNeutral : Bitmap
        private var bitmapControllerLeft : Bitmap
        private var bitmapControllerRight : Bitmap

        // 0 normal 1 large 2 small
        private var bitmapServerPaddle = BitmapFactory.decodeResource(resources, gameData.serverPaddle.imageResource)
        private var bitmapScaledServerPaddle = arrayOf(
            BitmapFactory.decodeResource(resources, gameData.clientPaddle.imageResource),
            BitmapFactory.decodeResource(resources, gameData.clientPaddle.imageResource),
            BitmapFactory.decodeResource(resources, gameData.clientPaddle.imageResource)
        )
        private var bitmapClientPaddle = BitmapFactory.decodeResource(resources, gameData.clientPaddle.imageResource)
        private var bitmapScaledClientPaddle = arrayOf(
            BitmapFactory.decodeResource(resources, gameData.clientPaddle.imageResource),
            BitmapFactory.decodeResource(resources, gameData.clientPaddle.imageResource),
            BitmapFactory.decodeResource(resources, gameData.clientPaddle.imageResource)
        )

        private var bitmapObstacles = arrayOf(
            BitmapFactory.decodeResource(resources, R.drawable.ball_green),
            BitmapFactory.decodeResource(resources, R.drawable.ball_brown),
            BitmapFactory.decodeResource(resources, R.drawable.ball_purple),
            BitmapFactory.decodeResource(resources, R.drawable.cube_cobalt),
            BitmapFactory.decodeResource(resources, R.drawable.cube_green),
            BitmapFactory.decodeResource(resources, R.drawable.cube_yellow),
            BitmapFactory.decodeResource(resources, R.drawable.drug_pink),
            BitmapFactory.decodeResource(resources, R.drawable.drug_purple),
            BitmapFactory.decodeResource(resources, R.drawable.drug_red)
        )

        private var bitmapRemnant : Bitmap

        var isUsingStick = false
        var isDraggingRight = false
        var isDraggingLeft = false

        var scaledControllerRect = RectF()

        private var scaleX: Float = 1.0f
        private var scaleY: Float = 1.0f

        init {
            paint.color = Color.BLUE // Change color as needed
            paint.style = Paint.Style.FILL

            //400X600 image
            backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
            //Controller(size of 75*60)
            bitmapControllerInactive = BitmapFactory.decodeResource(resources, R.drawable.neutral_inactive)
            bitmapControllerNeutral = BitmapFactory.decodeResource(resources, R.drawable.neutral)
            bitmapControllerLeft = BitmapFactory.decodeResource(resources, R.drawable.left)
            bitmapControllerRight = BitmapFactory.decodeResource(resources, R.drawable.right)

            bitmapRemnant = BitmapFactory.decodeResource(resources, R.drawable.remant)
        }

        /*****************************
         * onSizeChanged  - bitmap 및 offScreenBitmp을 resize
         * onDraw() 에서 drawGame을 call
         * drawGame() - rescale 된 bitmap에 rescale된 immge를 그림
         *****************************/
        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)

            // Calculate scaling factors to adjust bitmap size based on device screen size
            scaleX = w.toFloat() / TestGameCons.BITMAP_WIDTH // DESIRED_WIDTH is the width you want your game graphics to be
            scaleY = h.toFloat() / TestGameCons.BITMAP_HEIGHT // DESIRED_HEIGHT is the height you want your game graphics to be

            // Determine the scaled bitmap dimensions
            val scaledWidth = (TestGameCons.BITMAP_WIDTH * scaleX).toInt()
            val scaledHeight = (TestGameCons.BITMAP_HEIGHT * scaleY).toInt()

            // resize bitmaps of Controller
            bitmapControllerInactive = Bitmap.createScaledBitmap(bitmapControllerInactive,
                (TestGameCons.CONTROLLER_WIDTH * scaleX).toInt(),
                (TestGameCons.CONTROLLER_HEIGHT * scaleY).toInt(), true)
            bitmapControllerNeutral = Bitmap.createScaledBitmap(bitmapControllerNeutral,
                (TestGameCons.CONTROLLER_WIDTH * scaleX).toInt(),
                (TestGameCons.CONTROLLER_HEIGHT * scaleY).toInt(), true)
            bitmapControllerLeft = Bitmap.createScaledBitmap(bitmapControllerLeft,
                (TestGameCons.CONTROLLER_WIDTH * scaleX).toInt(),
                (TestGameCons.CONTROLLER_HEIGHT * scaleY).toInt(), true)
            bitmapControllerRight = Bitmap.createScaledBitmap(bitmapControllerRight,
                (TestGameCons.CONTROLLER_WIDTH * scaleX).toInt(),
                (TestGameCons.CONTROLLER_HEIGHT * scaleY).toInt(), true)

            bitmapRemnant = Bitmap.createScaledBitmap(bitmapRemnant,
                (60*scaleX).toInt(), (60*scaleY).toInt(), true)

            // Determine the scaledControllerRect = stick 이 위치할 장소
            scaledControllerRect.left = TestGameCons.CONTROLLER_RECT.left * scaleX
            scaledControllerRect.top = TestGameCons.CONTROLLER_RECT.top * scaleY
            scaledControllerRect.right = TestGameCons.CONTROLLER_RECT.right * scaleX
            scaledControllerRect.bottom = TestGameCons.CONTROLLER_RECT.bottom * scaleY

            // paddle bitmap resize  0 normla, 1 large  2 small
            Log.i(">>>>", "scaleX at the time of bitamp Scaling : ${scaleX}")
            bitmapScaledServerPaddle[0] = Bitmap.createScaledBitmap(bitmapServerPaddle,
                (80*scaleX).toInt(), (20*scaleY).toInt(), true)
            bitmapScaledServerPaddle[1] = Bitmap.createScaledBitmap(bitmapServerPaddle,
                (100*scaleX).toInt(), (20*scaleY).toInt(), true)
            bitmapScaledServerPaddle[2] = Bitmap.createScaledBitmap(bitmapServerPaddle,
                (60*scaleX).toInt(), (20*scaleY).toInt(), true)
            bitmapScaledClientPaddle[0] = Bitmap.createScaledBitmap(bitmapClientPaddle,
                (80*scaleX).toInt(), (20*scaleY).toInt(), true)
            bitmapScaledClientPaddle[1] = Bitmap.createScaledBitmap(bitmapClientPaddle,
                (100*scaleX).toInt(), (20*scaleY).toInt(), true)
            bitmapScaledClientPaddle[2] = Bitmap.createScaledBitmap(bitmapClientPaddle,
                (60*scaleX).toInt(), (20*scaleY).toInt(), true)

            // Initialize the offscreen bitmap and canvas with scaled dimensions
            offscreenBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
            offscreenBitmapRect = Rect(0, 0, offscreenBitmap.width, offscreenBitmap.height )
            offscreenCanvas = Canvas(offscreenBitmap)
        }

        private fun drawGame(canvas : Canvas){
            canvas.drawBitmap(backgroundBitmap, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), null)
            drawObstacleAndRemnant(canvas)
            drawPaddle(canvas)
            drawScore(canvas)
            drawWEffect(canvas)
            drawController(canvas)
        }
        private fun drawObstacleAndRemnant(canvas: Canvas){
            gameData.obstacles.forEach{
                canvas.drawBitmap(bitmapObstacles[it.type], null, it.getScaledRect(scaleX, scaleY), paint )
            }
            gameData.obstacleRemnant?.let{
                canvas.drawBitmap(bitmapRemnant, (it.x-30)*scaleX, (it.y-30)*scaleY ,paint )
                gameData.obstacleRemnant = null // 화면 출력후 업애준다
            }
        }
        private fun drawController(canvas : Canvas){
            if(!isUsingStick) canvas.drawBitmap(bitmapControllerInactive,
                scaledControllerRect.left, scaledControllerRect.top, paint)
            else if(!isDraggingRight && !isDraggingLeft) canvas.drawBitmap(
                bitmapControllerNeutral, scaledControllerRect.left, scaledControllerRect.top, paint)
            else if(isDraggingRight) canvas.drawBitmap(
                bitmapControllerRight, scaledControllerRect.left, scaledControllerRect.top, paint)
            else canvas.drawBitmap(bitmapControllerLeft,
                scaledControllerRect.left, scaledControllerRect.top, paint)
        }
        private fun drawPaddle(canvas : Canvas){
            val scaledBallX = gameData.ballX * scaleX
            val scaledBallY = gameData.ballY * scaleY
            val scaledBallRadius = gameData.ballRadius * scaleX // Assuming same scale factor for x and y

            // draw Paddle and ball
            var drawingPoint = gameData.serverPaddle.getDrawingPoint(scaleX,scaleY)
            canvas.drawBitmap(bitmapScaledServerPaddle[gameData.serverPaddle.getPaddleState()]
                , drawingPoint.x, drawingPoint.y, paint)
            drawingPoint = gameData.clientPaddle.getDrawingPoint(scaleX, scaleY)
            canvas.drawBitmap(bitmapScaledClientPaddle[gameData.clientPaddle.getPaddleState()]
                , drawingPoint.x, drawingPoint.y, paint)
            paint.color = Color.BLACK
            canvas.drawCircle(scaledBallX, scaledBallY, scaledBallRadius, paint)
            Log.i(">>>>", "paddleState : ${gameData.serverPaddle.getPaddleState()}. ${gameData.clientPaddle.getPaddleState()}")
        }
        private fun drawScore(canvas : Canvas){
            val serverScoreX = 20f * scaleX
            val clientScoreX = 340f * scaleX
            val scoreY = TestGameCons.PRINT_SCORE_BASELINE * scaleY
            paint.color = Color.BLUE
            paint.textSize = TestGameCons.SCORE_SIZE * scaleX
            canvas.drawText("$serverWin", serverScoreX, scoreY, paint)
            paint.color = Color.RED
            paint.textSize = TestGameCons.SCORE_SIZE * scaleX
            canvas.drawText("$clientWin", clientScoreX, scoreY, paint)
        }
        private fun drawWEffect(canvas : Canvas) {
            gameData.effectServer?.let{
                paint.color = Color.BLUE
                paint.textSize = TestGameCons.EFFECT_REMAIN_SIZE * scaleX
                canvas.drawText("${gameData.effectRemainServer}", 120f*scaleX, 580f*scaleY, paint)
                canvas.drawBitmap(bitmapObstacles[it], 120f*scaleX, 510f*scaleY, paint)
            }
            gameData.effectClient?.let{
                paint.color = Color.RED
                paint.textSize = TestGameCons.EFFECT_REMAIN_SIZE * scaleX
                canvas.drawText("${gameData.effectRemainClient}", 250f*scaleX, 580f*scaleY, paint)
                canvas.drawBitmap(bitmapObstacles[it], 250f*scaleX, 510f*scaleY, paint)
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            drawGame(offscreenCanvas)
            canvas.drawBitmap(offscreenBitmap, null, offscreenBitmapRect, null)
        }

        // With this code and view.performClick() in touch Listener is needed
        override fun performClick(): Boolean {
            super.performClick()
            // Handle the click action here if needed
            return true
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

    // Fragment의 Button 초기화 및 설정
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

    // View가 생성되고 난 뒤 Socket 연결 및 Controller를 초기화 함
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

    // Game Controller 초기화 및 설정
    private fun initGameInterfaceListener() {
        testGameView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Check if touch is inside the controller area
                    if (testGameView.scaledControllerRect.contains(event.x, event.y)) {
                        testGameView.isUsingStick = true
                        startStickX = event.x
                        currentPosition.x = event.x
                        currentPosition.y = event.y
                        true // Indicate that touch event is consumed
                    } else {
                        false // Indicate that touch event is not consumed
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    // Update current position if user is using the stick
                    if (testGameView.isUsingStick) {
                        currentPosition.x = event.x
                        currentPosition.y = event.y
                    }
                    true // Indicate that touch event is consumed
                }
                MotionEvent.ACTION_UP -> {
                    testGameView.isUsingStick = false
                    view.performClick() // Call performClick() on ACTION_UP
                    true // Indicate that touch event is consumed
                }
                else -> false // Return false for other touch event actions
            }
        }
    }

    // timer를 on or off  => Game Controller 의 상태를 주기적으로 Server로 보냄
    private fun startTouchEventTimer(activate : Boolean) {
        if (activate) {
            if (touchTimer == null) {
                touchTimer = Timer().apply {
                    scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {
                            testGameView.isDraggingRight = currentPosition.x > startStickX + TestGameCons.CONTROLLER_NEUTRAL_WIDTH
                            testGameView.isDraggingLeft = currentPosition.x < startStickX - TestGameCons.CONTROLLER_NEUTRAL_WIDTH

                            if (!testGameView.isUsingStick) return

                            if (mainActivity.asServer!!) {
                                if (testGameView.isDraggingRight) {
                                    serverSocketThread?.onGameDataFromServerFragment("ACTION:SERVER_RIGHT")
                                } else if (testGameView.isDraggingLeft) {
                                    serverSocketThread?.onGameDataFromServerFragment("ACTION:SERVER_LEFT")
                                }
                            } else {
                                if (testGameView.isDraggingRight) {
                                    clientSocketThread?.onMessageFromClientToServer("ACTION:CLIENT_RIGHT")
                                } else if (testGameView.isDraggingLeft) {
                                    clientSocketThread?.onMessageFromClientToServer("ACTION:CLIENT_LEFT")
                                }
                            }
                        }
                    }, 0, TestGameCons.TOUCH_EVENT_INTERVAL)
                }
            }
        } else {
            touchTimer?.cancel() // Stop the timer if it's running
            touchTimer = null // Reset the timer instance
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

    // InitServerSocket후 대기중 상대방 refuse 할때 사용
    // callback unregister가 다음 접속을 위해 중요
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
                //Toast.makeText(mainActivity, "Socket network available", Toast.LENGTH_SHORT).show()
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

                //Toast.makeText(mainActivity, "Got Server Address\nStarting client socket thread", Toast.LENGTH_SHORT).show()

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
            Toast.makeText(mainActivity, "상대방과 게임에 연결 되었습니다", Toast.LENGTH_SHORT).show()
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
     * 두 개 다 동일한 code 구현 or processGameStateChange에서 구현
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
                Toast.makeText(mainActivity, "game started", Toast.LENGTH_SHORT).show()
                testViewModel.setGameState(GameState.STARTED)
                startTouchEventTimer(true)
            }
            GameState.PAUSED-> {
                startTouchEventTimer(false)
                Toast.makeText(mainActivity, "game paused", Toast.LENGTH_SHORT).show()
                testViewModel.setGameState(GameState.PAUSED)
            }
            GameState.STOPPED-> {
                startTouchEventTimer(false)
                Toast.makeText(mainActivity, "game stopped", Toast.LENGTH_SHORT).show()
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
            //Log.i(">>>>", "received gameData in TestFragment : $gameData")
            processGameData(gameData)
        } else {
            Log.e(">>>>", "onGameDataReceivedFromThread to client")
        }
    }
    override fun onGameDataReceivedFromServerViaSocket(strGameData: String) {
        if(mainActivity.asServer!!) {
            Log.e(">>>>", "onGameDataReceivedFromServerViaSocket to server")
        } else {
            try{
                val gameData = TestGameData.fromString(strGameData)
                //Log.i(">>>>", "received gameData from server : $gameData")
                if (gameData is TestGameData) {
                    processGameData(gameData)
                } else {
                    Log.e(">>>>", "onGameDataReceivedFromServerViaSocket,  fail to Convert")
                }
            } catch(e : Exception) {
                Log.e(">>>>", "onGameDataReceivedFromServerViaSocket, ${e.message}")
            }
         }
    }

    private fun processGameData(gameData: TestGameData) {
        testGameView.gameData = gameData
        testGameView.invalidate()
    }

    /**********************************]
     * Winnder message 처리
     * Thread에서 실행 시킴 : runOnUiThread 필요
     * onGameWinnerFromServerViaSocket : client용
     * onGameWinnerFromThread : server용
     ***********************************/
    override fun onGameWinnerFromThread(isServerWin: Boolean) {
        mainActivity.runOnUiThread {
            if(mainActivity.asServer!!) {
                if(isServerWin) {
                    testGameView.serverWin ++
                    SimpleConfirmDialog(mainActivity,
                        "win", "You won this game").showDialog()
                } else {
                    testGameView.clientWin ++
                    SimpleConfirmDialog(mainActivity,
                        "lose", "You lost this game").showDialog()
                }
            }
            testGameView.invalidate()
        }
    }

    override fun onGameWinnerFromServerViaSocket(isServerWin: Boolean) {
        mainActivity.runOnUiThread{
            if(!mainActivity.asServer!!) {
                if(!isServerWin) {
                    testGameView.clientWin ++
                    SimpleConfirmDialog(mainActivity,
                        "win", "You won this game").showDialog()
                } else {
                    testGameView.serverWin++
                    SimpleConfirmDialog(mainActivity,
                        "lose", "You lost this game").showDialog()
                }
            }
            testGameView.invalidate()
        }
    }

    /*********************
     * Other Message From thread  :  client 만 해당
     * onOtherMessageReceivedFromServerViaSocket : Server 만 해당
     * onOtherMessageFromClientViaSocket
     * 현재는 HEART_BEAT 외에는 없어서 없어도 될 듯함
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