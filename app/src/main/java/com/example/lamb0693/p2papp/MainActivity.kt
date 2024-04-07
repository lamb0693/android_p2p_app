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
import com.example.lamb0693.p2papp.interfaces.FragmentTransactionHandler
import com.example.lamb0693.p2papp.socket_thread.ClientSocketThread
import com.example.lamb0693.p2papp.socket_thread.ServerSocketThread
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback
import java.net.InetSocketAddress

class MainActivity : AppCompatActivity() , FragmentTransactionHandler, ThreadMessageCallback{

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

            // session이 있으면 연결 버튼 disable
            //bindMain.btnConnect.isEnabled = (it==null)
            //bindMain.btnDisconnect.isEnabled = (it!=null)
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
                    if(receivedMessage.contains("PEER_HANDLE_IS_SET")){
                        currentPeerHandle = peerHandle
                        viewModel.setWifiAwareConnected(true)
                        setButtonConnection(true)
                        sendMessageViaSession("SEND_SERVER_INFO")
                        // do network connection
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
                    viewModel.setWifiAwareConnected(true)
                    setButtonConnection(true)

                    // send Message
                    sendMessageViaSession(messageToSend)
                }
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                    val receivedMessage = String(message, Charsets.UTF_8)
                    Log.i(">>>>", "onMessageReceived...$peerHandle, $receivedMessage")
                    if(receivedMessage.contains("SEND_SERVER_INFO")) {
                        currentPeerHandle = peerHandle
                        // do Network Connection
                    }
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
        if(asServer == null || asServer==false) return
        Log.e(">>>>", "asServer ==null || asServer==false in initServerSocket()")

        if(publishDiscoverySession == null || currentPeerHandle == null) return
        Log.e(">>>>", "publishDiscoverySession ==null || peerHandle == null in initServerSocket()")

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
                        serverSocketThread = ServerSocketThread(this@MainActivity, this@MainActivity)
                        serverSocketThread?.also{
                            it.start()
                            runOnUiThread{
                                bindMain.buttonSendMessage.isEnabled = true
                            }
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
            }
        }

        connectivityManager.requestNetwork(myNetworkRequest, networkCallback)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToServerSocket() {
        if(asServer == null || asServer==true) return
        Log.e(">>>>", "asServer ==null || asServer==true in connectToServerSocket()")

        if(subscribeDiscoverySession == null || currentPeerHandle == null) return
        Log.e(">>>>", "subscribeDiscoverySession ==null || peerHandle == null in connectToServerSocket()")

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
                val peerIpv6 = peerAwareInfo.peerIpv6Addr

                if(clientSocketThread == null){
                    try{
                        clientSocketThread = ClientSocketThread(this@MainActivity,
                            InetSocketAddress(peerIpv6, 8888), this@MainActivity)
                        clientSocketThread?.also{
                            it.start()
                            runOnUiThread{
                                bindMain.buttonSendMessage.isEnabled = true
                            }
                        }
                    } catch(e : Exception){
                        Log.e(">>>>", "clientSocket : ${e.message}")
                    }
                }

//                networkCallback?.let{
//                    connectivityManager.unregisterNetworkCallback(it)
//                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.i(">>>>", "NetworkCallback onLost")
                removeCurrentWifiAwareSession()
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

//        val strDisplay = bindMain.tvChattingArea.text.toString() + "\n" + message
//        bindMain.tvChattingArea.text = strDisplay
    }

    private fun sendMessageViaSocket(message : String){
        if(serverSocketThread == null && clientSocketThread == null) {
            Log.e(">>>>", "SocketThread null in sendMessageViaSocket()")
            return
        }

        if(asServer!!) {
            serverSocketThread!!.sendMessageFromMainThread("server : $message")
        } else {
            clientSocketThread!!.sendMessageFromMainThread("client : $message")
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
        setButtonConnection(false)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        registerReceiver(wifiAwareReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        super.onResume()
    }

    override fun onChangeFragment(newFragment: Fragment, tagName : String) {
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

    override fun onMessageReceivedFromThread(message: String) {
        Log.i(">>>>", "from thread : $message")
        // 일단 homeFragment의 TextView에 추가하고

        // homeFragment가 아니면 Toast에

    }

    override fun onThreadTerminated() {
        Log.i(">>>>", "from thread : terminating")
        bindMain.imageConnectStatus.setImageResource(android.R.drawable.button_onoff_indicator_off)
    }

    override fun onThreadStarted() {
        Log.i(">>>>", "from thread : started")
        bindMain.imageConnectStatus.setImageResource(android.R.drawable.button_onoff_indicator_on)
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