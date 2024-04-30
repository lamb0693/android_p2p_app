package com.example.lamb0693.p2papp

import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
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
import com.example.lamb0693.p2papp.fragment.BounceFragment
import com.example.lamb0693.p2papp.fragment.LandingFragment
import com.example.lamb0693.p2papp.fragment.interfaces.FragmentTransactionHandler
import com.example.lamb0693.p2papp.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() , FragmentTransactionHandler {

    private lateinit var bindMain : ActivityMainBinding
    private lateinit var viewModel : MainViewModel

    private val fragmentManager: FragmentManager = supportFragmentManager

    private val homeFragment = HomeFragment.newInstance("va1", "val2")

    // wifi aware connection variable
    private lateinit var wifiAwareManager : WifiAwareManager
    private var wifiAwareReceiver: WifiAwareBroadcastReceiver? = null
    private lateinit var intentFilter : IntentFilter
    private var currentWifiAwareSession : WifiAwareSession? = null
    private var customAttachCallback = CustomAttachCallback(this)
    var publishDiscoverySession : PublishDiscoverySession? = null
    var subscribeDiscoverySession : SubscribeDiscoverySession? =null
    var currentPeerHandle : PeerHandle? = null

    // connection var
    var asServer : Boolean? =null

    private fun registerWifiAwareReceiver() {
        intentFilter = IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED)
        wifiAwareReceiver = WifiAwareBroadcastReceiver(this, wifiAwareManager, currentWifiAwareSession )
    }

    @SuppressLint("SetTextI18n")
    private fun initViewModel(){
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        viewModel.wifiAwareConnected.observe(this){
            if(it) {
                bindMain.imageConnectStatus.setImageResource(R.drawable.custom_onoff_wifi_on)
            }
            else {
                bindMain.imageConnectStatus.setImageResource(R.drawable.custom_onoff_wifi_off)
            }
            setButtonConnStateOfSettingFragment(it)
        }

        viewModel.roomName.observe(this){
            bindMain.tvRoomName.text = it
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

    private fun setHomeFragmentInMainActivity(){
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.frameLayoutContainer, homeFragment)
        fragmentTransaction.commit();
    }

    private fun initSystemService() {
        wifiAwareManager= getSystemService(Context.WIFI_AWARE_SERVICE) as WifiAwareManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindMain = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindMain.root)

        initViewModel()
        setHomeFragmentInMainActivity()
        initializeButton()
        initSystemService()
        registerWifiAwareReceiver()

        checkPermission()
    }

    // SessionCallback에서 실행 됨
    fun attach(){
        wifiAwareManager.attach(customAttachCallback, null)
    }

    @SuppressLint("MissingPermission")
    fun setWifiAwareSession(wifiAwareSession: WifiAwareSession?){
        removeCurrentWifiAwareSession()
        currentWifiAwareSession = wifiAwareSession

        Log.i(">>>>", "setting wifiAwareSession")
        if(currentWifiAwareSession == null) {
            Log.e(">>>>", "wifiAwareSession null")
            return
        }

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
                    if(receivedMessage.contains("PEER_HANDLE_IS_SET")){
                        currentPeerHandle = peerHandle
                        runOnUiThread {
                            viewModel.setWifiAwareConnected(true)
                        }
                        Log.i(">>>>",
                            "onMessageReceived...$peerHandle, $receivedMessage, Setting currentPeerHandle and sending server info")
                        Toast.makeText(this@MainActivity, "상대방과 연결되었습니다", Toast.LENGTH_LONG).show()
                        sendMessageViaSession("SEND_SERVER_INFO")
                    } else if (receivedMessage.contains("REFUSE_INVITATION")){
                        val bounceFragment : BounceFragment? = getBounceFragment()
                        if(bounceFragment == null) {
                            Log.e(">>>>", "bounceFragment null in onMessageReceived")
                        } else {
                            bounceFragment.cancelInitServerSocket()
                        }
                    } else {
                        homeFragment.addTextToChattingArea(receivedMessage, false)
                        Toast.makeText(this@MainActivity, receivedMessage, Toast.LENGTH_SHORT).show()
                    }
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
                    Toast.makeText(this@MainActivity, "상대방과 연결되었습니다", Toast.LENGTH_LONG).show()
                    val messageToSend = "PEER_HANDLE_IS_SET"
                    currentPeerHandle = peerHandle
                    sendMessageViaSession(messageToSend)

                    //connection 된 것으로 처리
                    runOnUiThread {
                        viewModel.setWifiAwareConnected(true)
                    }
                }
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
                    val receivedMessage = String(message, Charsets.UTF_8)
                    Log.i(">>>>", "onMessageReceived...$peerHandle, $receivedMessage")
                    if(receivedMessage.contains("INVITATION")) {
                        showInvitationAlertDialog()
                    } else if(receivedMessage.contains("SEND_SERVER_INFO")) {
                        Log.i(">>>>", "Recieved SEND_SERVER_INFO message")
                    } else {
                        homeFragment.addTextToChattingArea(receivedMessage, false)
                        Toast.makeText(this@MainActivity, receivedMessage, Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onSessionTerminated() {
                    removeCurrentWifiAwareSession()
                    Toast.makeText(this@MainActivity, "session terminated", Toast.LENGTH_SHORT).show()
                    removeCurrentWifiAwareSession()
                    super.onSessionTerminated()
                }
                override fun onServiceLost(peerHandle: PeerHandle, reason: Int) {
                    super.onServiceLost(peerHandle, reason)
                    Log.i(">>>>", "onServiceLost $peerHandle, $reason")
                    Toast.makeText(this@MainActivity, "session service lost", Toast.LENGTH_SHORT).show()
                    removeCurrentWifiAwareSession() // UIchange도 함수안에
                }
            }, null)
        }
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

        homeFragment.addTextToChattingArea("나 : $message", true)

        if(asServer!!) {
            publishDiscoverySession!!.sendMessage(
                currentPeerHandle!!,101, message.toByteArray(Charsets.UTF_8))
        } else {
            subscribeDiscoverySession!!.sendMessage(
                currentPeerHandle!!,101, message.toByteArray(Charsets.UTF_8))
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
        viewModel.setWifiAwareConnected(false)
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

        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayoutContainer, newFragment, tagName)
        //fragmentTransaction.addToBackStack(null) // Optional: add transaction to back stack
        fragmentTransaction.commit()

        if(newFragment is HomeFragment) bindMain.imageButtonSetting.isEnabled = true
        else bindMain.imageButtonSetting.isEnabled = false
    }

    override fun onConnectSessionButtonClicked(roomName : String) {
        if(asServer == null || roomName.isEmpty()) {
            SimpleConfirmDialog(this, R.string.allim, R.string.set_role_name_first).showDialog()
            return
        }

        if (!roomName.matches("[a-zA-Z][a-zA-Z0-9]*".toRegex())) {
            SimpleConfirmDialog(this, R.string.allim, R.string.roomname_prerequsite).showDialog()
            return
        }

        Log.i(">>>>", "ConnectSession Button is clicked")
        viewModel.setRoomName(roomName)
        attach()
    }

    // UI change도 removeCurrentWifiAwareSession() 안에서
    override fun onDisconnectSessionButtonClicked() {
        removeCurrentWifiAwareSession()
    }

    override fun onAsServerButtonClicked() {
        Log.i(">>>>", "AsServer Button is clicked")
        asServer = true
    }

    override fun onAsClientButtonClicked() {
        Log.i(">>>>", "AsClient Button is clicked")
        asServer = false
    }

//    @RequiresApi(Build.VERSION_CODES.Q)
//    override fun onGameBounceButtonClicked() {
//        if(isSocketConnectionPossible()) {
//            if(!asServer!!) {
//                SimpleConfirmDialog(this, R.string.allim ,R.string.condtion_open_room).showDialog()
//                return
//            }
//            BounceFragment.newInstance("val1", "val2").apply {
//                setHomeFragment(homeFragment)
//                onChangeFragment(this, "BounceFragment")
//            }
//        } else {
//            Toast.makeText(this, getString(R.string.wifiaware_first),
//                Toast.LENGTH_SHORT).show()
//        }
//    }

    override fun onGameButtonClicked(gameName : String) {
        if(isSocketConnectionPossible()) {
            if(!asServer!!) {
                SimpleConfirmDialog(this, R.string.allim ,R.string.condtion_open_room).showDialog()
                return
            }
            when(gameName){
                "Bounce" -> {
                    BounceFragment.newInstance("val1", "val2").apply {
                        setHomeFragment(homeFragment)
                        onChangeFragment(this, "BounceFragment")
                    }
                }
                "Landing" -> {
                    LandingFragment.newInstance("val1", "val2").apply {
                        setHomeFragment(homeFragment)
                        onChangeFragment(this, "LandFragment")
                    }
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.wifiaware_first),
                Toast.LENGTH_SHORT).show()
        }
    }

    override fun onEnableButtonSetting(bEnabled: Boolean) {
        bindMain.imageButtonSetting.isEnabled = bEnabled
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

    // client 만 해당
    // refuse하면 server에게 refuse message를 보냄
    private fun showInvitationAlertDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this@MainActivity)
        alertDialogBuilder.setTitle(R.string.invitaion)
        alertDialogBuilder.setMessage(R.string.invitaion_message)
        alertDialogBuilder.setPositiveButton(R.string.accept) { dialogInterface, _ ->
            dialogInterface.dismiss()
            // Handle invitation acceptance here
            // For example, start the BounceFragment
            BounceFragment.newInstance("val1", "val2").apply {
                setHomeFragment(homeFragment)
                onChangeFragment(this, "BounceFragment")
            }
        }
        alertDialogBuilder.setNegativeButton(R.string.decline) { dialogInterface, _ ->
            dialogInterface.dismiss()
            // server에 refuse message 보냄
            sendMessageViaSession("REFUSE_INVITATION")
        }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Create a confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage(R.string.confirm_exit)
            .setPositiveButton(R.string.yes) { _, _ ->
                // Call finish() to exit the app
                super.onBackPressed()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun getBounceFragment(): BounceFragment? {
        // Find the fragment by its tag
        return supportFragmentManager.findFragmentByTag("BounceFragment") as? BounceFragment
    }

    // viewModel.observer 에서만 실행
    private fun setButtonConnStateOfSettingFragment(connected : Boolean){
        val buttonConnect = findViewById<Button>(R.id.buttonConnectSession)
        buttonConnect?.isEnabled = !connected
        val buttonDisconnect = findViewById<Button>(R.id.buttonDisconnectSession)
        buttonDisconnect?.isEnabled = connected
    }
}