package com.example.lamb0693.p2papp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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
    private val settingFragment = SettingFragment.newInstance("val1", "val2")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindMain = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindMain.root)

        initializeMainWithHomeFragment()
        initializeButton()
    }

    private fun initializeButton(){
        bindMain.imageButtonSetting.setOnClickListener{
            bindMain.imageButtonSetting.isEnabled = false
            val settingFragment = SettingFragment.newInstance("val1", "val2")
            settingFragment?.let{
                it.setHomeFragment(homeFragment)
                onChangeFragment(it)
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
}