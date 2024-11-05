package com.your.user

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.your.user.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGoToAudioActivity.setOnClickListener {
            val intent = Intent(this, AudioActivity::class.java)
            startActivity(intent)
        }

    }
}