package com.example.lamb0693.p2papp.fragment.interfaces

import androidx.fragment.app.Fragment

interface FragmentTransactionHandler {
    fun onChangeFragment(newFragment : Fragment, tagName : String)
    fun onConnectSessionButtonClicked(roomName : String)
    fun onDisconnectSessionButtonClicked()
    fun onAsServerButtonClicked()
    fun onAsClientButtonClicked()
    fun onGame1ButtonClicked()
    fun setEnableButtonSetting(bEnabled: Boolean)
}