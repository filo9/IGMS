package com.your.user

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.Button

class LightControlDialog(context: Context, light: String, private val commandCallback: (String) -> Unit) : Dialog(context) {

    private val lightName = light

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_light_control)

        val turnButton = findViewById<Button>(R.id.btn_turn)
        val increaseButton = findViewById<Button>(R.id.btn_increase_brightness)
        val decreaseButton = findViewById<Button>(R.id.btn_decrease_brightness)
        val backButton = findViewById<Button>(R.id.btn_back)

        turnButton.setOnClickListener {
            commandCallback("TURN")
            dismiss()
        }
        increaseButton.setOnClickListener {
            commandCallback("UP")
            dismiss()
        }
        decreaseButton.setOnClickListener {
            commandCallback("DOWN")
            dismiss()
        }
        backButton.setOnClickListener {
            dismiss()
        }
    }
}