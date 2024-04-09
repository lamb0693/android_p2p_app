package com.example.lamb0693.p2papp

import android.content.Context
import androidx.appcompat.app.AlertDialog

class SimpleConfirmDialog (context : Context, title : String, message : String) : AlertDialog.Builder(context){
    init{
        setTitle(title)
        setMessage(message)
        setPositiveButton("OK") { dialog, _ ->
            // Handle the OK button click here
            dialog.dismiss()
        }
    }

    fun showDialog(){
        this.create().show()
    }
}