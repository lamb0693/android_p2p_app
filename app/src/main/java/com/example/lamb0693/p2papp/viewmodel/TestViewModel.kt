package com.example.lamb0693.p2papp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TestViewModel : ViewModel() {
    private val _socketConnected = MutableLiveData<Boolean>()
    val socketConnected : LiveData<Boolean> get() = _socketConnected

    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> get() = _gameState

    init {
        _socketConnected.value = false
        _gameState.value = GameState.STOPPED
    }

    fun setSocketConnected(connected: Boolean){
        _socketConnected.value = connected
    }

    fun setGameState(state: GameState) {
        _gameState.value = state
    }
}

enum class GameState {
    STOPPED,
    STARTED,
    PAUSED
}