package com.example.lamb0693.p2papp

import android.net.wifi.aware.WifiAwareSession
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _wifiAwareConnected = MutableLiveData<Boolean>()
    val wifiAwareConnected : LiveData<Boolean> get() = _wifiAwareConnected
    private val _roomName = MutableLiveData<String>("")
    val roomName : LiveData<String> = _roomName

    init {
        _wifiAwareConnected.value = false
    }

    fun setWifiAwareConnected(connected: Boolean){
        _wifiAwareConnected.value = connected
    }

    fun setRoomName(roomName : String){
        _roomName.value = roomName
    }
}