package com.example.lamb0693.p2papp.fragment

import android.app.AlertDialog
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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider
import com.example.lamb0693.p2papp.Constant
import com.example.lamb0693.p2papp.MainActivity
import com.example.lamb0693.p2papp.R
import com.example.lamb0693.p2papp.SimpleConfirmDialog
import com.example.lamb0693.p2papp.fragment.interfaces.FragmentTransactionHandler
import com.example.lamb0693.p2papp.socket_thread.ClientSocketThread
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback
import com.example.lamb0693.p2papp.socket_thread.bounce.BounceCons
import com.example.lamb0693.p2papp.socket_thread.bounce.BounceData
import com.example.lamb0693.p2papp.socket_thread.bounce.BounceServerSocketThread
import com.example.lamb0693.p2papp.socket_thread.landing.LandingData
import com.example.lamb0693.p2papp.socket_thread.landing.LandingServerSocketThread
import com.example.lamb0693.p2papp.viewmodel.BounceViewModel
import com.example.lamb0693.p2papp.viewmodel.GameState
import com.example.lamb0693.p2papp.viewmodel.LandingViewModel
import java.net.InetSocketAddress
import java.util.Objects
import java.util.Timer
import java.util.TimerTask

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LandingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LandingFragment : Fragment(), ThreadMessageCallback {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    // socketConnected 와, GameStatus 를 위한 ViewModel
    private lateinit var landingViewModel : LandingViewModel

    // Fragment 를 Activity 에 전달 하기 위한 View
    private lateinit var thisFragment : View

    // homeFragment 는 항상 Activity 에 남아 있음
    private var homeFragment: HomeFragment? = null

    // MainActivity 의 함수 실행용
    private var fragmentTransactionHandler : FragmentTransactionHandler? = null

    // Game 화면을 그리기 위한 View
    private lateinit var landingGameView: LandingFragment.LandingGameView

    private var buttonStart : Button?= null
    private var buttonToHome : ImageButton? = null

    lateinit var mainActivity : MainActivity

    private lateinit var connectivityManager : ConnectivityManager

    //socketThread
    private lateinit var serverSocketAddress : InetSocketAddress
    private var serverSocketThread : LandingServerSocketThread? =  null
    private var clientSocketThread : ClientSocketThread? =  null

    private var networkCallback : ConnectivityManager.NetworkCallback? =null

    // timer action of user periodically
    private var touchTimer: Timer? = null

    /***********************************
     * Nested class LandingGameView
     **********************************/
    class LandingGameView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {
        var serverWin = 0
        var clientWin = 0

        var gameData = LandingData()  // class 를 만드는 것은 의미 없음. 단지 만들어 놓음

        private val paint: Paint = Paint()

        // 400 * 600 background
        private var backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
        private lateinit var offscreenBitmap: Bitmap
        private lateinit var offscreenCanvas: Canvas
        private lateinit var offscreenBitmapRect : Rect

        private var scaleX: Float = 1.0f
        private var scaleY: Float = 1.0f

        /*****************************
         * onSizeChanged  - bitmap 및 offScreenBitmap 을 resize
         * onDraw() 에서 drawGame 을 call
         * drawGame() - rescale 된 bitmap 에 rescale 된 image 를 그림
         *****************************/
        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)

            // Calculate scaling factors to adjust bitmap size based on device screen size
            scaleX = w.toFloat() / BounceCons.BITMAP_WIDTH // DESIRED_WIDTH is the width you want your game graphics to be
            scaleY = h.toFloat() / BounceCons.BITMAP_HEIGHT // DESIRED_HEIGHT is the height you want your game graphics to be

            backgroundBitmap = Bitmap.createScaledBitmap(backgroundBitmap, w, h,true)

            // Initialize the offscreen bitmap and canvas with scaled dimensions
            offscreenBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            offscreenBitmapRect = Rect(0, 0, offscreenBitmap.width, offscreenBitmap.height )
            offscreenCanvas = Canvas(offscreenBitmap)
        }

        private fun drawGame(canvas : Canvas){
            canvas.drawBitmap(backgroundBitmap, 0f, 0f, paint)
            drawScore(canvas)
        }

        private fun drawScore(canvas : Canvas){
            val serverScoreX = 20f * scaleX
            val clientScoreX = 340f * scaleX
            val scoreY = BounceCons.PRINT_SCORE_BASELINE * scaleY
            paint.color = Color.BLUE
            paint.textSize = BounceCons.SCORE_SIZE * scaleX
            canvas.drawText("$serverWin", serverScoreX, scoreY, paint)
            paint.color = Color.RED
            paint.textSize = BounceCons.SCORE_SIZE * scaleX
            canvas.drawText("$clientWin", clientScoreX, scoreY, paint)
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
     * Code for LandingFragment
     **********************************/

    private fun initViewModel() {
        landingViewModel = ViewModelProvider(this)[LandingViewModel::class.java]

        landingViewModel.socketConnected.observe(viewLifecycleOwner){
            val imageViewSocketConnected = thisFragment.findViewById<ImageView>(R.id.ivSocketConnectStatusLanding)
            if(it) imageViewSocketConnected.setImageResource(R.drawable.custom_socket_connection_on)
            else imageViewSocketConnected.setImageResource(R.drawable.custom_socket_connection_off)
        }

        landingViewModel.gameState.observe(viewLifecycleOwner) {
            when(it) {
                GameState.STOPPED -> {
                    buttonStart?.text = getString(R.string.start)
                    // network 연결 될 때 혹은 winner dialog 클릭 시 풀어 주어야 함
                    buttonStart?.isEnabled = false
                }
                GameState.STARTED -> buttonStart?.text = getString(R.string.pause)
                GameState.PAUSED  -> buttonStart?.text = getString(R.string.restart)
                null -> return@observe
            }
        }
    }

    // Fragment의 Button 초기화 및 설정
    private fun initTestViewButtonAndListener(){
        buttonToHome = thisFragment.findViewById<ImageButton>(R.id.buttonToHomeFromLanding)
        if(buttonToHome == null) Log.e(">>>>", "buttonToHome null")

        buttonStart = thisFragment.findViewById<Button>(R.id.btnStarLandingGame)
        if(buttonStart == null) Log.e(">>>>", "buttonStart null")
        buttonStart?.isEnabled = false

        buttonToHome?.setOnClickListener{
            showConfirmationDialogForButtonToHome()
        }

        // button click 하면 sever에 message 보내기만,  ui 처리는 server에서 돌아오면 한다.
        buttonStart?.setOnClickListener{
            buttonStart?.isEnabled = false
            if(landingViewModel.gameState.value == GameState.STOPPED) {
                if(mainActivity.asServer!!) serverSocketThread?.setServerPrepared()
                else clientSocketThread?.onMessageFromClientToServer("CLIENT_PREPARED_GAME")
            } else if(landingViewModel.gameState.value == GameState.STARTED) {
                if (mainActivity.asServer!!) serverSocketThread?.setPauseGameRoutine(true)
                else clientSocketThread?.onMessageFromClientToServer("CLIENT_PAUSED_GAME")
            } else if(landingViewModel.gameState.value == GameState.PAUSED) {
                if (mainActivity.asServer!!) serverSocketThread?.setPauseGameRoutine(false)
                else clientSocketThread?.onMessageFromClientToServer("CLIENT_RESTART_GAME")
            }
        }
    }

    private fun showConfirmationDialogForButtonToHome() {
        AlertDialog.Builder(context)
            .setMessage(R.string.return_home_fragment) //"Are you sure you want to go main page?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                navigateToHome()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false) // Disallows dismiss by clicking outside
            .show()
    }

    private fun navigateToHome() {
        if(landingViewModel.socketConnected.value == true){
            if(mainActivity.asServer!!) serverSocketThread?.quitServerThread()
            else clientSocketThread?.onQuitMessageFromFragment()
        } else{
            homeFragment?.let { fragment ->
                fragmentTransactionHandler?.onChangeFragment(fragment, "HomeFragment")
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        thisFragment =  inflater.inflate(R.layout.fragment_landing, container, false)
        landingGameView = thisFragment.findViewById(R.id.viewTestGame)

        connectivityManager = mainActivity.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager

        initViewModel()
        initTestViewButtonAndListener()

        return thisFragment
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // asServer -> initServerSocket else connectToServerSocket
        if(mainActivity.asServer!!) {
            initServerSocket()  // accept()에서 block됨
            mainActivity.sendMessageViaSession("INVITATION:LANDING")
        }
        else connectToServerSocket()

        // Game Control용 Listerner
        initGameInterfaceListener()
    }

    private fun initGameInterfaceListener() {

    }

    // timer를 on or off  => Game Controller 의 상태를 주기적으로 Server로 보냄
    private fun startTouchEventTimer(activate : Boolean) {
        if (activate) {
            if (touchTimer == null) {
                touchTimer = Timer().apply {
                    scheduleAtFixedRate(object : TimerTask() {
                        override fun run() {

                        }
                    }, 0, BounceCons.TOUCH_EVENT_INTERVAL)
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
            Log.e("onAttach", "onAttach ${e.message}")
            throw RuntimeException("$context must implement FragmentTransactionHandler and MainActivity")
        }
    }

    // Detach()될때 NetworkCallback 을 꼭 제거해야 됨 - 아니면 다음에 다른 callback이 안 붙음
    override fun onDetach() {
        super.onDetach()
        networkCallback?.let{
            Log.i("onDetach", "unregistering network Callback")
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
            Log.e("cancelInitServerSocket", "error to cancelInitServerSocket() : ${e.message}")
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun initServerSocket(){
        if(mainActivity.asServer == null || mainActivity.asServer==false) {
            Log.e("initServerSocket", "asServer ==null || asServer==false in initServerSocket()")
            return
        }

        if(mainActivity.publishDiscoverySession == null || mainActivity.currentPeerHandle == null){
            Log.e("initServerSocket", "publishDiscoverySession ==null || peerHandle == null in initServerSocket()")
            return
        }

        Log.i("initServerSocket", "init serverSocket")

        serverSocketThread = LandingServerSocketThread(this@LandingFragment)

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
        Log.i("initServerSocket", "networkRequest :  $myNetworkRequest in initServerSocket()")

        // 콜백 만들고 등록
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i("initServerSocket", "NetworkCallback onAvailable")
                //Toast.makeText(mainActivity, "Socket network available", Toast.LENGTH_SHORT).show()
                try{
                    serverSocketThread?.start()
                } catch ( e : Exception){
                    Log.e("initServerSocket", "starting socket thread exception : ${e.message}")
                }
            }
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Log.i("initServerSocket", "NetworkCallback onCapabilitiesChanged network : $network")
            }
            override fun onLost(network: Network) {
                super.onLost(network)
                Log.i("initServerSocket", "NetworkCallback onLost")
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
            Log.e("connectToServerSocket", "asServer ==null || asServer==true in connectToServerSocket()")
            return
        }

        if(mainActivity.subscribeDiscoverySession == null || mainActivity.currentPeerHandle == null) {
            Log.e("connectToServerSocket", "subscribeDiscoverySession ==null || peerHandle == null in connectToServerSocket()")
            return
        }

        Log.i("connectToServerSocket", "init connectToServerSocket")

        val networkSpecifier = WifiAwareNetworkSpecifier.Builder(
            mainActivity.subscribeDiscoverySession!!, mainActivity.currentPeerHandle!!)
            .setPskPassphrase(Constant.PSK_PARAPHRASE)
            .build()

        Log.i("connectToServerSocket", "connecting to server socket $networkSpecifier")

        val myNetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            .setNetworkSpecifier(networkSpecifier)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i("connectToServerSocket", "NetworkCallback onAvailable")
            }
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Log.i("connectToServerSocket", "NetworkCallback onCapabilitiesChanged network : $network")

                val peerAwareInfo = networkCapabilities.transportInfo as WifiAwareNetworkInfo
                serverSocketAddress =InetSocketAddress(peerAwareInfo.peerIpv6Addr, peerAwareInfo.port)

                //Toast.makeText(mainActivity, "Got Server Address\nStarting client socket thread", Toast.LENGTH_SHORT).show()

                if(clientSocketThread == null){
                    try{
                        clientSocketThread = ClientSocketThread(serverSocketAddress,this@LandingFragment)
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
                    landingViewModel.setSocketConnected(false)
                }
            }
        }
        connectivityManager.requestNetwork(myNetworkRequest,
            networkCallback as ConnectivityManager.NetworkCallback
        )
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LandingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LandingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    /**********************************]
     * 기본적인 ThreadMessageCallback overload
     * Thread 에서 실행 시킴 : runOnUiThread 필요
     ***********************************/
    override fun onConnectionMade() {
        mainActivity.runOnUiThread{
            landingViewModel.setSocketConnected(true)
            Toast.makeText(mainActivity, "상대방과 게임에 연결 되었습니다", Toast.LENGTH_SHORT).show()
            buttonStart?.isEnabled = true
        }
    }
    override fun onThreadStarted() {
        Log.i("onThreadStarted", "from thread : started")
    }
    override fun onThreadTerminated() {
        Log.i("onThreadTerminated", "from thread : terminating")
        mainActivity.runOnUiThread {
            landingViewModel.setSocketConnected(false)
            SimpleConfirmDialog(mainActivity, R.string.allim,
                R.string.connection_lost_message).showDialog()

            homeFragment?.let { fragment ->
                fragmentTransactionHandler?.onChangeFragment(fragment, "HomeFragment")
            }
        }
    }

    /***************
     * ThreadMessageCallback overload  : GameStateMessage 처리
     * onGameStateMessageFromThread :  from server thread server 만 해당
     * onGameStateFromServerViaSocket : from client thread  client 만 해당
     * processGameStateChange
     * 두 개 다 동일한 code 구현 or processGameStateChange 에서 구현
     */
    override fun onGameStateMessageFromThread(gameState: GameState) {
        if(mainActivity.asServer!!){
            Log.i("onGameStateMessageFromThread", "received GameState message from server thread")
            mainActivity.runOnUiThread{
                processGameStateChange(gameState)
            }
        }
    }

    //client는 server -> client thread통해 받고
    override fun onGameStateFromServerViaSocket(gameState: GameState) {
        if(!mainActivity.asServer!!){
            Log.i("onGameStateFromServerViaSocket", "received GameState message from client thread")
            mainActivity.runOnUiThread{
                processGameStateChange(gameState)
            }
        }
    }

    private fun processGameStateChange(gameState: GameState) {
        when(gameState){
            GameState.STARTED-> {
                Toast.makeText(mainActivity, "game started", Toast.LENGTH_SHORT).show()
                landingViewModel.setGameState(GameState.STARTED)
                startTouchEventTimer(true)
            }
            GameState.PAUSED-> {
                startTouchEventTimer(false)
                Toast.makeText(mainActivity, "game paused", Toast.LENGTH_SHORT).show()
                landingViewModel.setGameState(GameState.PAUSED)
            }
            GameState.STOPPED-> {
                startTouchEventTimer(false)
                Toast.makeText(mainActivity, "game stopped", Toast.LENGTH_SHORT).show()
                landingViewModel.setGameState(GameState.STOPPED)
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
    override fun onGameDataReceivedFromThread(gameData:Any) {
        if(gameData is LandingData) {
            if(mainActivity.asServer!!){
                //Log.i(">>>>", "received gameData in BounceFragment : $gameData")
                processGameData(gameData)
            } else {
                Log.e(">>>>", "onGameDataReceivedFromThread to client")
            }
        }
    }

    override fun onGameDataReceivedFromServerViaSocket(strGameData: String) {
        if(mainActivity.asServer!!) {
            Log.e(">>>>", "onGameDataReceivedFromServerViaSocket to server")
        } else {
            try{
                val gameData = LandingData.fromString(strGameData)
                //Log.i(">>>>", "received gameData from server : $gameData")
                if (gameData is LandingData) {
                    processGameData(gameData)
                } else {
                    Log.e(">>>>", "onGameDataReceivedFromServerViaSocket,  fail to Convert")
                }
            } catch(e : Exception) {
                Log.e(">>>>", "onGameDataReceivedFromServerViaSocket, ${e.message}")
            }
        }
    }

    private fun processGameData(gameData: LandingData) {
        landingGameView.gameData = gameData
        landingGameView.invalidate()
    }

    /**********************************]
     * Winner message 처리
     * Thread 에서 실행 시킴 : runOnUiThread 필요
     * onGameWinnerFromServerViaSocket : client 용
     * onGameWinnerFromThread : server 용
     ***********************************/
    override fun onGameWinnerFromThread(isServerWin: Boolean) {
        mainActivity.runOnUiThread {
            if(mainActivity.asServer!!) {
                if(isServerWin) {
                    landingGameView.serverWin ++
                    SimpleConfirmDialog(mainActivity,
                        R.string.win, R.string.win_message).showDialog()
                } else {
                    landingGameView.clientWin ++
                    SimpleConfirmDialog(mainActivity,
                        R.string.lose, R.string.lose_message).showDialog()
                }
            }
            landingGameView.invalidate()
            buttonStart?.isEnabled = true
        }
    }

    override fun onGameWinnerFromServerViaSocket(isServerWin: Boolean) {
        mainActivity.runOnUiThread{
            if(!mainActivity.asServer!!) {
                if(!isServerWin) {
                    landingGameView.clientWin ++
                    SimpleConfirmDialog(mainActivity,
                        R.string.win, R.string.win_message).showDialog()
                } else {
                    landingGameView.serverWin++
                    SimpleConfirmDialog(mainActivity,
                        R.string.lose, R.string.lose_message).showDialog()
                }
            }
            landingGameView.invalidate()
            buttonStart?.isEnabled = true
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
}