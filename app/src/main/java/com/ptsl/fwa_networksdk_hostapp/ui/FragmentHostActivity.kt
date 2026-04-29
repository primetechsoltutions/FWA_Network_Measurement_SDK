package com.ptsl.fwa_networksdk_hostapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ptsl.fwa_networksdk_hostapp.R

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

