package com.example.lamb0693.p2papp.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import com.example.lamb0693.p2papp.MainActivity
import com.example.lamb0693.p2papp.R
import com.example.lamb0693.p2papp.interfaces.FragmentTransactionHandler
import com.example.lamb0693.p2papp.socket_thread.ThreadMessageCallback

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TestFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TestFragment : Fragment(), ThreadMessageCallback {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var testView : View
    private var homeFragment: HomeFragment? = null
    private var fragmentTransactionHandler : FragmentTransactionHandler? = null

    lateinit var testGameView: TestGameView

    class TestGameView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {

        private val paint: Paint = Paint()
        private val mainActivity = context as MainActivity

        var charX : Float = 100.0F
        var charY : Float = 100.0F

        init {
            paint.color = Color.BLUE // Change color as needed
            paint.style = Paint.Style.FILL

            setOnClickListener {
                // fragmentTransactionHandler 를 구하면 될텐데
                mainActivity.onGameData("CLICKED")

                Log.i(">>>>", "TestGameView clicked")
                invalidate()
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            // Draw whatever you want here for your game view
            canvas.drawRect(charX, charY, charX+100F, charY+100F, paint)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        testView = inflater.inflate(R.layout.fragment_test, container, false)
        testGameView = testView.findViewById(R.id.viewTestGame)

        val buttonToHome = testView.findViewById<Button>(R.id.buttonToHomeFromTest)
        if(buttonToHome == null) Log.e(">>>>", "buttonToHome null")
        buttonToHome?.setOnClickListener{
            Log.i(">>>>", "homeFragement : $homeFragment")
            // Socket 정리는 MainActivity 의 onChangeFragment 에서
            homeFragment?.let { fragment ->
                fragmentTransactionHandler?.onChangeFragment(fragment, "HomeFragment")
            }
        }

        return testView
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    fun setHomeFragment(fragment: HomeFragment?) {
        if(fragment == null) {
            Log.e(">>>>", "homeFragment is null  in setHomeFragment()")
        }
        homeFragment = fragment
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
         * @return A new instance of fragment TestFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic fun newInstance(param1: String, param2: String) =
                TestFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }

    override fun onMessageReceivedFromThread(message: String) {
        Log.i(">>>>", "onMessageReceivedFromThread : $message")
        if(message.contains("charx:")) {
            try{
                var newX = message.split(":")[1].toFloat()
                Log.i(">>>>","newX : $newX")
                testGameView.charX = newX
                testGameView.invalidate()
            } catch(e : Exception) {
                Log.e(">>>>", "toFloat  : ${e.message}")
            }
        }
    }

    override fun onThreadTerminated() {
        Log.i(">>>>", "from thread : terminating")
    }

    override fun onThreadStarted() {
        Log.i(">>>>", "from thread : started")
    }

}