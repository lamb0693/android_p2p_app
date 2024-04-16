package com.example.lamb0693.p2papp.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import com.example.lamb0693.p2papp.MainActivity
import com.example.lamb0693.p2papp.R
import com.example.lamb0693.p2papp.SimpleConfirmDialog
import com.example.lamb0693.p2papp.fragment.interfaces.FragmentTransactionHandler
import java.lang.Exception

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var isAlreadyConnected: String? = null
    private var param2: String? = null
    private var thisView : View? = null
    private var homeFragment: HomeFragment? = null
    private var fragmentTransactionHandler : FragmentTransactionHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isAlreadyConnected = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        thisView =  inflater.inflate(R.layout.fragment_setting, container, false)
        Log.e(">>>>", "fail to create View in onCreateView")

        initializeButton()

        return thisView
    }

    fun setHomeFragment(fragment: HomeFragment?) {
        if(fragment == null) {
            Log.e(">>>>", "homeFragment is null  in setHomeFragment()")
        }
        homeFragment = fragment
    }

    private fun initializeButton(){
        // 홈으로 돌아가기 버튼
        val buttonSettingCompleted = thisView?.findViewById<Button>(R.id.buttonSettingCompleted)
        if(buttonSettingCompleted == null) Log.e(">>>>", "buttonSettingCompleted null")
        buttonSettingCompleted?.setOnClickListener{
            homeFragment?.let { fragment ->
                fragmentTransactionHandler?.onChangeFragment(fragment, "HomeFragment")
            }
        }

        // WifiAware session 연결 버튼
        val buttonConnectSession = thisView?.findViewById<Button>(R.id.buttonConnectSession)
        if(buttonConnectSession == null) Log.e(">>>>", "buttonConnectSession null")
        buttonConnectSession?.isEnabled = ( isAlreadyConnected!!.contains("false") )
        buttonConnectSession?.setOnClickListener{
            val roomName : String
            thisView?.findViewById<EditText>(R.id.editRoomName).also{editText->
                roomName = editText?.text.toString().trim()
            }
            fragmentTransactionHandler?.onConnectSessionButtonClicked(roomName)
        }

        // WifiAware 연결 해제 버튼
        val buttonDisconnectSession = thisView?.findViewById<Button>(R.id.buttonDisconnectSession)
        if(buttonDisconnectSession == null) Log.e(">>>>", "buttonConnectSession null")
        buttonDisconnectSession?.isEnabled = ( isAlreadyConnected!!.contains("true") )
        buttonDisconnectSession?.setOnClickListener{
            fragmentTransactionHandler?.onDisconnectSessionButtonClicked()
        }

        // 서버 역할 선택
        val buttonAsServer = thisView?.findViewById<RadioButton>(R.id.rbServer)
        if(buttonAsServer == null) Log.e(">>>>", "buttonAsServer null")
        buttonAsServer?.setOnClickListener{
            fragmentTransactionHandler?.onAsServerButtonClicked()
        }

        // client 역할 선택
        val buttonAsClient = thisView?.findViewById<RadioButton>(R.id.rbClient)
        if(buttonAsClient == null) Log.e(">>>>", "buttonAsClient null")
        buttonAsClient?.setOnClickListener{
            fragmentTransactionHandler?.onAsClientButtonClicked()
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            fragmentTransactionHandler = context as FragmentTransactionHandler
        } catch (e : Exception){
            Log.e(">>>>", "onAttach ${e.message}")
            throw RuntimeException("$context must implement FragmentTransactionHandler")
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SettingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}