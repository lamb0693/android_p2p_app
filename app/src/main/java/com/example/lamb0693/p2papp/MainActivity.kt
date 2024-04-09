package com.example.lamb0693.p2papp

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareNetworkInfo
import android.net.wifi.aware.WifiAwareNetworkSpecifier
import android.net.wifi.aware.WifiAwareSession
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.example.lamb0693.p2papp.databinding.ActivityMainBinding
import com.example.lamb0693.p2papp.fragment.HomeFragment
import com.example.lamb0693.p2papp.fragment.SettingFragment
import com.example.lamb0693.p2papp.fragment.TestFragment
import com.example.lamb0693.p2papp.interfaces.FragmentTransactionHandler
import com.example.lamb0693.p2papp.socket_thread.ClientSocketThread
import com.example.lamb0693.p2papp.socket_thread.ServerSocketThread
import java.net.InetSocketAddress

class MainActivity : AppCompatActivity() , FragmentTransactionHandler{

    private lateinit var bindMain : ActivityMainBinding
    private lateinit var viewModel : MainViewModel

    private val fragmentManager: FragmentManager = supportFragmentManager
    private val homeFragment = HomeFragment.newInstance("va1", "val2")

    // wifi aware connection variable
    private lateinit var connectivityManager : ConnectivityManager
    private lateinit var wifiAwareManager : WifiAwareManager
    private var wifiAwareReceiver: WifiAwareBroadcastReceiver? = null
    private lateinit var intentFilter : IntentFilter
    private var currentWifiAwareSession : WifiAwareSession? = null
    private var customAttachCallback = CustomAttachCallback(this)
    private var publishDiscoverySession : PublishDiscoverySession? = null
    private var subscribeDiscoverySession : SubscribeDiscoverySession? =null
    private var currentPeerHandle : PeerHandle? = null

    // connection var
    private var asServer : Boolean? =null

    //socketThread
    private lateinit var serverSocketAddress : InetSocketAddress
    private var serverSocketThread : ServerSocketThread? =  null
    private var clientSocketThread : ClientSocketThread? =  null

    private fun registerWifiAwareReceiver() {
        intentFilter = IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED)
        wifiAwareReceiver = WifiAwareBroadcastReceiver(this, wifiAwareManager, currentWifiAwareSession )
    }

    @SuppressLint("SetTextI18n")
    private fun initViewModel(){
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        viewModel.wifiAwareConnected.observe(this){
            if(it) bindMain.imageConnectStatus.setImageResource(android.R.drawable.button_onoff_indicator_on)
            else {
                bindMain.imageConnectStatus.setImageResource(android.R.drawable.button_onoff_indicator_off)
                // 나중에 상대방 device 이름 으로 대체
                //bindMain.tvConnectedDevice.text = "connected"
            }
        }

        viewModel.roomName.observe(this){
            bindMain.tvConnectedDevice.text = it
        }
    }

    private fun initializeButton(){
        bindMain.imageButtonSetting.setOnClickListener{
            bindMain.imageButtonSetting.isEnabled = false
            SettingFragment.newInstance(viewModel.wifiAwareConnected.value.toString(), "val2").apply {
                setHomeFragment(homeFragment)
                onChangeFragment(this, "SettingFragment")
            }
        }

        bindMain.buttonSendMessage.setOnClickListener {
            if(bindMain.editMessage.toString().trim().isEmpty()) return@setOnClickListener
            sendMessageViaSession(bindMain.editMessage.text.toString())
            bindMain.editMessage.setText("")
        }
    }

    private fun initializeMainWithHomeFragment(){
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.frameLayoutContainer, homeFragment)
        fragmentTransaction.commit();
    }

    private fun initSystemService() {
        connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        wifiAwareManager= getSystemService(Context.WIFI_AWARE_SERVICE) as WifiAwareManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindMain = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindMain.root)

        initViewModel()
        initializeMainWithHomeFragment()
        initializeButton()
        initSystemService()
        registerWifiAwareReceiver()

        checkPermission()
    }

    fun attach(){
        wifiAwareManager.attach(customAttachCallback, null)
    }

    @SuppressLint("MissingPermission")
    fun setWifiAwareSession(wifiAwareSession: WifiAwareSession?){
        removeCurrentWifiAwareSession()
        currentWifiAwareSession = wifiAwareSession

        Log.i(">>>>", "setting wifiAwareSession")
        if(currentWifiAwareSession == null) Log.e(">>>>", "wifiAwareSession null")

        if(asServer == null || viewModel.roomName.value!!.isEmpty()) {
            Log.e(">>>>", "asServer null or roomName Empty")
            return
        }

        if(asServer!!) {
            val config: PublishConfig = PublishConfig.Builder()
                .setServiceName(viewModel.roomName.value!!)
                .setTtlSec(0)
                .build()
            currentWifiAwareSession?.publish(config, object : DiscoverySessionCallback() {
                override fun onPublishStarted(session: PublishDiscoverySession) {
                    Log.i(">>>>", "onPublishStarted... $session")
                    publishDiscoverySession = session
                }
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                    val receivedMessage = String(message, Charsets.UTF_8)
                    Log.i(">>>>", "onMessageReceived...$peerHandle, $receivedMessage, Setting currentPeerHandle")
                    currentPeerHandle = peerHandle
                    if(receivedMessage.contains("PEER_HANDLE_IS_SET")){
//                        initServerSocket()
                        sendMessageViaSession("SEND_SERVER_INFO")
                    }

                    //connection 된 것으로 처리
                    runOnUiThread {
                        viewModel.setWifiAwareConnected(true)
                        setButtonConnection(true)
                    }

                    Toast.makeText(this@MainActivity, receivedMessage, Toast.LENGTH_SHORT).show()
                }
                override fun onSessionTerminated() {
                    Log.i(">>>>", "onSessionTerminated")
                    removeCurrentWifiAwareSession()
                    Toast.makeText(this@MainActivity, "session terminated", Toast.LENGTH_SHORT).show()
                    super.onSessionTerminated()
                }
                override fun onServiceLost(peerHandle: PeerHandle, reason: Int) {
                    super.onServiceLost(peerHandle, reason)
                    Log.i(">>>>", "onServiceLost $peerHandle, $reason")
                    Toast.makeText(this@MainActivity, "session service lost", Toast.LENGTH_SHORT).show()
                    removeCurrentWifiAwareSession()
                }
            }, null)
        } else {
            val config: SubscribeConfig = SubscribeConfig.Builder()
                .setServiceName(viewModel.roomName.value!!)
                .setTtlSec(0)
                .build()
            currentWifiAwareSession?.subscribe(config, object : DiscoverySessionCallback() {
                override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                    Log.i(">>>>", "onSubscribeStarted... $session")
                    subscribeDiscoverySession = session
                }
                override fun onServiceDiscovered(
                    peerHandle: PeerHandle,
                    serviceSpecificInfo: ByteArray,
                    matchFilter: List<ByteArray>
                ) {
                    Log.i(">>>>", "onServiceDiscovered... $peerHandle, $serviceSpecificInfo")
                    Toast.makeText(this@MainActivity, "Connected to server", Toast.LENGTH_SHORT).show()
                    val messageToSend = "PEER_HANDLE_IS_SET"
                    currentPeerHandle = peerHandle

                    //connection 된 것으로 처리
                    runOnUiThread {
                        viewModel.setWifiAwareConnected(true)
                        setButtonConnection(true)
                    }

                    // send Message
                    sendMessageViaSession(messageToSend)
                }
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                    val receivedMessage = String(message, Charsets.UTF_8)
                    Log.i(">>>>", "onMessageReceived...$peerHandle, $receivedMessage")
                    currentPeerHandle = peerHandle
//                    if(receivedMessage.contains("SEND_SERVER_INFO")) {
//                        connectToServerSocket()
//                    }
                    Toast.makeText(this@MainActivity, receivedMessage, Toast.LENGTH_SHORT).show()
                }
                override fun onSessionTerminated() {
                    removeCurrentWifiAwareSession()
                    Toast.makeText(this@MainActivity, "session terminated", Toast.LENGTH_SHORT).show()
                    super.onSessionTerminated()
                }
                override fun onServiceLost(peerHandle: PeerHandle, reason: Int) {
                    super.onServiceLost(peerHandle, reason)
                    Log.i(">>>>", "onServiceLost $peerHandle, $reason")
                    Toast.makeText(this@MainActivity, "session service lost", Toast.LENGTH_SHORT).show()
                    removeCurrentWifiAwareSession()
                }
            }, null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun initServerSocket(){
        if(asServer == null || asServer==false) {
            Log.e(">>>>", "asServer ==null || asServer==false in initServerSocket()")
            return
        }

        if(publishDiscoverySession == null || currentPeerHandle == null){
            Log.e(">>>>", "publishDiscoverySession ==null || peerHandle == null in initServerSocket()")
            return
        }

        Log.i(">>>>", "init serverSocket")
        // WifiAwareNetworkSpecifier 생성
        val networkSpecifier = WifiAwareNetworkSpecifier.Builder(publishDiscoverySession!!, currentPeerHandle!!)
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
                Toast.makeText(this@MainActivity, "Socket network available", Toast.LENGTH_LONG).show()

                // ServerSocketThread가 만들어 져 있지 않으면
                // ServerSocketThread를 만들고 시작시킴
                try{
                    if(serverSocketThread == null) {
                        serverSocketThread = ServerSocketThread(this@MainActivity,
                            supportFragmentManager.findFragmentById(R.id.frameLayoutContainer) as TestFragment)
                        serverSocketThread?.also{
                            it.start()
//                            runOnUiThread{
//                                bindMain.btnSendViaSocket.isEnabled = true
//                            }
                        }
                    }
                } catch ( e : Exception){
                    Log.e(">>>>", "starting socket thread exception : ${e.message}")
                }
                // connection 된 것으로 처리
//                runOnUiThread {
//                    viewModel.setWifiAwareConnected(true)
//                    setButtonConnection(true)
//                }
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
//                runOnUiThread {
//                    viewModel.setWifiAwareConnected(false)
//                    setButtonConnection(false)
//                }
            }
        }

        connectivityManager.requestNetwork(myNetworkRequest, networkCallback)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToServerSocket() {
        if(asServer == null || asServer==true){
            Log.e(">>>>", "asServer ==null || asServer==true in connectToServerSocket()")
            return
        }

        if(subscribeDiscoverySession == null || currentPeerHandle == null) {
            Log.e(">>>>", "subscribeDiscoverySession ==null || peerHandle == null in connectToServerSocket()")
            return
        }

        Log.i(">>>>", "init connectToServerSocket")

        val networkSpecifier = WifiAwareNetworkSpecifier.Builder(subscribeDiscoverySession!!, currentPeerHandle!!)
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
                Toast.makeText(this@MainActivity, "Socket network available", Toast.LENGTH_LONG)
                    .show()
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
                        clientSocketThread = ClientSocketThread(this@MainActivity,
                            serverSocketAddress,
                            supportFragmentManager.findFragmentById(R.id.frameLayoutContainer) as TestFragment)
                        clientSocketThread?.also{
                            it.start()
//                            runOnUiThread{
//                                bindMain.btnSendViaSocket.isEnabled = true
//                            }
                        }
                    } catch(e : Exception){
                        Log.e(">>>>", "clientSocket : ${e.message}")
                    }
                }
                // connection 된 것으로 처리
//                runOnUiThread {
//                    viewModel.setWifiAwareConnected(true)
//                    setButtonConnection(true)
//                }
                Toast.makeText(this@MainActivity, "Got Server Address", Toast.LENGTH_LONG).show()
            }
            override fun onLost(network: Network) {
                super.onLost(network)
                Log.i(">>>>", "NetworkCallback onLost")
                removeCurrentSocketConnection()
//                runOnUiThread {
//                    viewModel.setWifiAwareConnected(false)
//                    setButtonConnection(false)
//                }
            }
        }
        connectivityManager.requestNetwork(myNetworkRequest,
            networkCallback as ConnectivityManager.NetworkCallback
        )
    }

    fun sendMessageViaSession(message : String){
        if(publishDiscoverySession == null && subscribeDiscoverySession == null) {
            Log.e(">>>>", "DiscoverySession null in sendMessageViaSession()")
            return
        }
        if(currentPeerHandle == null) {
            Log.e(">>>>", "peerHandle null in sendMessageViaSession()")
            return
        }

        if(asServer!!) {
            val strToSend = "server : $message"
            publishDiscoverySession!!.sendMessage(
                currentPeerHandle!!,101, strToSend.toByteArray(Charsets.UTF_8))
        } else {
            val strToSend = "client : $message"
            subscribeDiscoverySession!!.sendMessage(
                currentPeerHandle!!,101, strToSend.toByteArray(Charsets.UTF_8))
        }

    }

    private fun sendMessageViaSocket(message : String){
        if(serverSocketThread == null && clientSocketThread == null) {
            Log.e(">>>>", "SocketThread null in sendMessageViaSocket()")
            return
        }

        if(asServer!!) {
            serverSocketThread!!.sendMessageFromMainThread(message)
        } else {
            clientSocketThread!!.sendMessageFromMainThread(message)
        }
    }

    fun removeCurrentWifiAwareSession(){
        try{
            publishDiscoverySession?.close()
            publishDiscoverySession = null
            subscribeDiscoverySession?.close()
            subscribeDiscoverySession = null
            currentWifiAwareSession?.close()
            currentWifiAwareSession = null
            currentPeerHandle = null
        } catch (e: Exception) {
            Log.e(">>>>", "removeWifiAwareSession : ${e.message}")
        }
        // button in SettingFragment
        runOnUiThread{
            setButtonConnection(false)
            viewModel.setWifiAwareConnected(false)
        }
    }

    private fun removeCurrentSocketConnection(){
        clientSocketThread = null
        serverSocketThread = null
    }

    private fun isSocketConnectionPossible() : Boolean {
        if (asServer == null) return false
        if (publishDiscoverySession==null && subscribeDiscoverySession==null) return false
        if (currentPeerHandle == null) return false
        return true
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        registerReceiver(wifiAwareReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        super.onResume()
    }

    // Fragment 에 따라 Socket connection 을 정리
    override fun onChangeFragment(newFragment: Fragment, tagName : String) {
        Log.i(">>>>", "onChangeFragment")

        //현재 Fragment 가 Socket 을 사용 중이면 초기화
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frameLayoutContainer)
        if(currentFragment is TestFragment){
            removeCurrentSocketConnection()
        }

        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayoutContainer, newFragment, tagName)
        //fragmentTransaction.addToBackStack(null) // Optional: add transaction to back stack
        fragmentTransaction.commit()

        if(newFragment !is SettingFragment) bindMain.imageButtonSetting.isEnabled = true
    }

    override fun onConnectSessionButtonClicked(roomName : String) {
        Log.i(">>>>", "ConnectSession Button is clicked")
        viewModel.setRoomName(roomName)
        attach()
    }

    override fun onDisconnectSessionButtonClicked() {
        removeCurrentWifiAwareSession()
        viewModel.setWifiAwareConnected(false)
        viewModel.setRoomName("")
    }

    override fun onAsServerButtonClicked() {
        Log.i(">>>>", "AsServer Button is clicked")
        asServer = true
    }

    override fun onAsClientButtonClicked() {
        Log.i(">>>>", "AsClient Button is clicked")
        asServer = false
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onGame1ButtonClicked() {
        if(isSocketConnectionPossible()) {
            if(asServer!!) initServerSocket()
            else connectToServerSocket()

            TestFragment.newInstance("val1", "val2").apply {
                setHomeFragment(homeFragment)
                onChangeFragment(this, "TestFragment")
            }
        } else {
            Toast.makeText(this, "connect wifi aware network first",
                Toast.LENGTH_SHORT).show()
        }
    }

    fun onGameData(data : String){
        if(asServer == null) return

        if(asServer!!) {
            serverSocketThread!!.onMessageFromMain(data)
        } else {
            sendMessageViaSocket(data)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
            permissions -> val granted = permissions.entries.all {
        it.value
    }
        if(granted) Log.i(">>>>", "all permission granted in permission Launcher")
        else {
            Log.e(">>>>", "not all of permission granted in permission Launcher ")
        }
    }

    private fun checkPermission(){
        val statusCoarseLocation = ContextCompat.checkSelfPermission(this,
            "android.permission.ACCESS_COARSE_LOCATION")
        val statusFineLocation = ContextCompat.checkSelfPermission(this,
            "android.permission.ACCESS_FINE_LOCATION")

        val shouldRequestPermission = statusCoarseLocation != PackageManager.PERMISSION_GRANTED
                || statusFineLocation != PackageManager.PERMISSION_GRANTED

        if (shouldRequestPermission) {
            Log.d(">>>>", "One or more Permission Denied, Starting permission Launcher")
            permissionLauncher.launch(
                arrayOf(
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.ACCESS_FINE_LOCATION",
                )
            )
        } else {
            Log.i(">>>>", "All Permission Permitted, No need to start permission Launcher")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Create a confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                // Call finish() to exit the app
                super.onBackPressed()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun getSettingFragment(): SettingFragment? {
        // Find the fragment by its tag
        return supportFragmentManager.findFragmentByTag("SettingFragment") as? SettingFragment
    }

    private fun setButtonConnection(connected : Boolean){
        val buttonConnect = findViewById<Button>(R.id.buttonConnectSession)
        buttonConnect?.isEnabled = !connected
        val buttonDisconnect = findViewById<Button>(R.id.buttonDisconnectSession)
        buttonDisconnect?.isEnabled = connected
    }

}