package com.example.lamb0693.p2papp.fragment

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.example.lamb0693.p2papp.R
import com.example.lamb0693.p2papp.fragment.interfaces.FragmentTransactionHandler
import java.lang.Exception

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var fragmentTransactionHandler : FragmentTransactionHandler? = null

    protected var thisView : View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        thisView = inflater.inflate(R.layout.fragment_home, container, false)

        val buttonBounce = thisView?.findViewById<ImageButton>(R.id.imageButtonBounce)
        if(buttonBounce == null) Log.e(">>>>", "buttonAsServer null")
        buttonBounce?.setOnClickListener{
            fragmentTransactionHandler?.onGameBounceButtonClicked()
        }

        return thisView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            fragmentTransactionHandler = context as FragmentTransactionHandler
        } catch (e : Exception){
            Log.e(">>>>", "onAttach ${e.message}")
        }
    }

    fun addTextToChattingArea(message: String, fromMainActivity: Boolean) {
        if(message.contains("PEER_HANDLE_IS_SET") || message.contains("SEND_SERVER_INFO") ) return

        val tvChatting = thisView?.findViewById<TextView>(R.id.tvChattingArea)
        if(tvChatting == null) Log.e(">>>>", "tvChatting null")

        val spannable = SpannableString(message)

        // Set the color for the specific part of the text
        val color = if (fromMainActivity) {
            Color.BLUE // Change this to the desired color
        } else {
            Color.RED // Change this to the desired color
        }
        spannable.setSpan(ForegroundColorSpan(color), 0, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Append the SpannableString to the TextView
        tvChatting?.append("\n")
        tvChatting?.append(spannable)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

}