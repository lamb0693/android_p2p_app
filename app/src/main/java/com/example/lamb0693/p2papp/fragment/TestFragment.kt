package com.example.lamb0693.p2papp.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import com.example.lamb0693.p2papp.TestViewModel
import com.example.lamb0693.p2papp.interfaces.FragmentTransactionHandler
import com.example.lamb0693.p2papp.socket_thread.ClientSocketThread
import com.example.lamb0693.p2papp.socket_thread.ServerSocketThread
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback
import com.example.lamb0693.p2papp.socket_thread.test.TestGameData
import com.example.lamb0693.p2papp.socket_thread.test.TestServerSocketThread
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

    private lateinit var testViewModel : TestViewModel

    private lateinit var testView : View
    private var homeFragment: HomeFragment? = null
    private var fragmentTransactionHandler : FragmentTransactionHandler? = null

    lateinit var testGameView: TestGameView

    lateinit var mainActivity : MainActivity

    private lateinit var connectivityManager : ConnectivityManager
    //socketThread
    private lateinit var serverSocketAddress : InetSocketAddress
    private var serverSocketThread : TestServerSocketThread? =  null
    private var clientSocketThread : ClientSocketThread? =  null

    class TestGameView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {

        private val paint: Paint = Paint()

        var gameData = TestGameData(10.0F, 10.0F)

        init {
            paint.color = Color.BLUE // Change color as needed
            paint.style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            // Draw whatever you want here for your game view
            canvas.drawRect(gameData.charX, gameData.charY, gameData.charX+100F, gameData.charY+100F, paint)
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

        testViewModel = ViewModelProvider(this)[TestViewModel::class.java]

        testViewModel.socketConnected.observe(viewLifecycleOwner){
            val imageViewSocketConnected = testView.findViewById<ImageView>(R.id.imageSocketConnectStatus)
            if(it) imageViewSocketConnected.setImageResource(android.R.drawable.button_onoff_indicator_on)
            else imageViewSocketConnected.setImageResource(android.R.drawable.button_onoff_indicator_off)
        }

        val buttonToHome = testView.findViewById<Button>(R.id.buttonToHomeFromTest)
        if(buttonToHome == null) Log.e(">>>>", "buttonToHome null")
        buttonToHome?.setOnClickListener{
            if(mainActivity.asServer!!) serverSocketThread?.onQuitMessageFromFragment()
            else(clientSocketThread?.onQuitMessageFromFragment())

            // homeFragment로 돌아감
            homeFragment?.let { fragment ->
                fragmentTransactionHandler?.onChangeFragment(fragment, "HomeFragment")
            }
        }

        return testView
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(mainActivity.asServer!!) {
            initServerSocket()
            mainActivity.sendMessageViaSession("INVITATION")
        }
        else connectToServerSocket()

        testGameView.setOnClickListener{
            if(mainActivity.asServer!!){
                serverSocketThread?.onGameDataFromServerFragment("ACTION:LEFT")
            }else {
                clientSocketThread?.onMessageFromClientToServer("ACTION:RIGHT")
            }
        }
    }

    fun setHomeFragment(fragment: HomeFragment?) {
        if(fragment == null) {
            Log.e(">>>>", "homeFragment is null  in setHomeFragment()")
        }
        homeFragment = fragment
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            fragmentTransactionHandler = context as FragmentTransactionHandler
            mainActivity = context as MainActivity
        } catch (e : Exception){
            Log.e(">>>>", "onAttach ${e.message}")
            throw RuntimeException("$context must implement FragmentTransactionHandler and MainActivity")
        }
    }

    private fun removeCurrentSocketConnection(){
        clientSocketThread = null
        serverSocketThread = null
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

        // WifiAwareNetworkSpecifier 생성
        val networkSpecifier = WifiAwareNetworkSpecifier.Builder(
            mainActivity.publishDiscoverySession!!, mainActivity.currentPeerHandle!!)
            .setPskPassphrase("12340987").build()

        // WifiAware 를 이용 하는 NetworkRequest 생성
        val myNetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            .setNetworkSpecifier(networkSpecifier)
            .build()
        Log.i(">>>>", "networkRequest :  $myNetworkRequest in initServerSocket()")

        // 콜백 만들고 등록
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i(">>>>", "NetworkCallback onAvailable")
                Toast.makeText(mainActivity, "Socket network available", Toast.LENGTH_LONG).show()

                try{
                    if(serverSocketThread == null) {
                        serverSocketThread = TestServerSocketThread(this@TestFragment)
                        serverSocketThread?.also{
                            it.start()
                        }
                    }
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
                removeCurrentSocketConnection()
            }
        }

        connectivityManager.requestNetwork(myNetworkRequest, networkCallback)
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
            .setPskPassphrase("12340987")
            .build()

        Log.i(">>>>", "connecting to server socket $networkSpecifier")

        val myNetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
            .setNetworkSpecifier(networkSpecifier)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
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
                serverSocketAddress =InetSocketAddress(peerAwareInfo.peerIpv6Addr, Constant.PORT_NUMBER)

                if(clientSocketThread == null){
                    try{
                        clientSocketThread = ClientSocketThread(serverSocketAddress,this@TestFragment)
                        clientSocketThread?.also{
                            it.start()
                        }
                    } catch(e : Exception){
                        Log.e(">>>>", "clientSocket : ${e.message}")
                    }
                }
                Toast.makeText(mainActivity, "Got Server Address", Toast.LENGTH_LONG).show()
            }
            override fun onLost(network: Network) {
                super.onLost(network)
                Log.i(">>>>", "NetworkCallback onLost")
                removeCurrentSocketConnection()
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

    //both serverThread and client thread
    override fun onMessageReceivedFromThread(message: String) {
        Log.i(">>>>", "onMessageReceivedFromThread : $message")
    }

    override fun onThreadTerminated() {
        Log.i(">>>>", "from thread : terminating")
        mainActivity.runOnUiThread {
            Toast.makeText(mainActivity, " 상대방이 게임을 나갔습니다 ", Toast.LENGTH_LONG).show()
            testViewModel.setSocketConnected(false)
        }
    }

    override fun onThreadStarted() {
        Log.i(">>>>", "from thread : started")
    }

    // server인 경우에는 직접 받음
    override fun onGameDataReceivedFromThread(gameData: TestGameData) {
        if(mainActivity.asServer!!){
            Log.i(">>>>", "received gameData in TestFragment : $gameData")
            testGameView.gameData = gameData
            testGameView.invalidate()
        }
    }

    // server 로 부터 client 에 전달 되어 온 gameData
    override fun onGameDataReceivedFromServerViaSocket(strGameData: String) {
        if(!mainActivity.asServer!!){
            val gameData = TestGameData.fromString(strGameData)
            Log.i(">>>>", "received gameData from server : $gameData")
            if (gameData is TestGameData) {
                testGameView.gameData = gameData
                testGameView.invalidate()
            } else {
                Log.e(">>>>", "onGameDataReceivedFromServerViaSocket,  fail to Convert")
            }
        }
    }

    override fun onConnectionMade() {
        mainActivity.runOnUiThread{
            testViewModel.setSocketConnected(true)
        }
    }
}