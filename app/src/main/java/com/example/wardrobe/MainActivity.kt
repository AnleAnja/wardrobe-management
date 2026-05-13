package com.example.wardrobe

import MainAppContent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.wardrobe.ui.theme.WardrobeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WardrobeTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainAppContent()
                }
            }
        }
    }
}