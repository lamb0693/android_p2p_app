package com.example.lamb0693.p2papp

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.lamb0693.p2papp.databinding.ActivityMainBinding
import com.example.lamb0693.p2papp.fragment.HomeFragment
import com.example.lamb0693.p2papp.fragment.SettingFragment
import com.example.lamb0693.p2papp.interfaces.FragmentTransactionHandler

class MainActivity : AppCompatActivity() , FragmentTransactionHandler{

    private lateinit var bindMain : ActivityMainBinding

    private val fragmentManager: FragmentManager = supportFragmentManager

    private val homeFragment = HomeFragment.newInstance("va1", "val2")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindMain = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindMain.root)

        initializeMainWithHomeFragment()
        initializeButton()

        checkPermission()
    }

    private fun initializeButton(){
        bindMain.imageButtonSetting.setOnClickListener{
            bindMain.imageButtonSetting.isEnabled = false
            SettingFragment.newInstance("val1", "val2").apply {
                setHomeFragment(homeFragment)
                onChangeFragment(this)
            }
        }
    }

    private fun initializeMainWithHomeFragment(){
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.frameLayoutContainer, homeFragment)
        fragmentTransaction.commit();
    }

    override fun onChangeFragment(newFragment: Fragment) {
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayoutContainer, newFragment)
        //fragmentTransaction.addToBackStack(null) // Optional: add transaction to back stack
        fragmentTransaction.commit()

        if(newFragment !is SettingFragment) bindMain.imageButtonSetting.isEnabled = true
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

}