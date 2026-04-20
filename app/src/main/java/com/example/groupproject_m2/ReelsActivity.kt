package com.example.groupproject_m2

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class ReelsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate start")
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_reels)
            Log.d(TAG, "setContentView OK")

            if (savedInstanceState == null) {
                val spotName = intent.getStringExtra(EXTRA_SPOT_NAME) ?: ""
                val spotLocation = intent.getStringExtra(EXTRA_SPOT_LOCATION) ?: ""
                Log.d(TAG, "Starting ReelsFragment — spotName='$spotName' spotLocation='$spotLocation'")

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, ReelsFragment.newInstance(spotName, spotLocation))
                    .commit()
                Log.d(TAG, "Fragment transaction committed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Crash in onCreate", e)
        }
    }

    companion object {
        private const val TAG = "ReelsActivity"
        const val EXTRA_SPOT_NAME = "spot_name"
        const val EXTRA_SPOT_LOCATION = "spot_location"

        fun start(context: Context, spotName: String, spotLocation: String) {
            context.startActivity(
                Intent(context, ReelsActivity::class.java).apply {
                    putExtra(EXTRA_SPOT_NAME, spotName)
                    putExtra(EXTRA_SPOT_LOCATION, spotLocation)
                }
            )
        }
    }
}
