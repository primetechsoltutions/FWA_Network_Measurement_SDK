package com.ptsl.networksdk_event.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ptsl.networksdk_event.ui.theme.FWASDKTheme

class SupportFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                FWASDKTheme {
                    PlaceholderScreen("Support", onBack = { parentFragmentManager.popBackStack() })
                }
            }
        }
    }
}
