package com.example.lamb0693.p2papp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TestViewModel : ViewModel() {
    private val _socketConnected = MutableLiveData<Boolean>()
    val socketConnected : LiveData<Boolean> get() = _socketConnected

    init {
        _socketConnected.value = false
    }

    fun setSocketConnected(connected: Boolean){
        _socketConnected.value = connected
    }
}