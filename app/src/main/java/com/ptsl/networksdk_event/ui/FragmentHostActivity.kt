package com.ptsl.networksdk_event.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ptsl.networksdk_event.R

class FragmentHostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // We'll use a simple container for the fragment
        val containerId = android.R.id.content
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(containerId, NetworkAssessmentFragment())
                .commit()
        }
    }
}
