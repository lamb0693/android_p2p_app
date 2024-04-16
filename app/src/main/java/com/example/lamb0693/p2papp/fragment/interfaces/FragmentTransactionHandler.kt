package com.example.lamb0693.p2papp.fragment.interfaces

import androidx.fragment.app.Fragment

 /******
 * Fragment 에서 MainActivity 의 기능을 사용 하기 위해 사용
  *
  * MainActivity : FragmentTransactionHandler{
  *     Fragment(this)
  * }
  *
  * Fragment(){
  *     fragmentTransactionHandler : FragmentTransactionHandler
  *     onAttach(context : Context){
  *         fragmentTransactionHandler = context as FragmentTransactionHandler
  *     }
  * }
  */

interface FragmentTransactionHandler {
    // from multiple Fragment
    fun onChangeFragment(newFragment : Fragment, tagName : String)
    fun onEnableButtonSetting(bEnabled: Boolean)

    // from SettingFragment
    fun onConnectSessionButtonClicked(roomName : String)
    fun onDisconnectSessionButtonClicked()
    fun onAsServerButtonClicked()
    fun onAsClientButtonClicked()

    //from HomeFragment
    fun onGame1ButtonClicked()

}