package com.example.wardrobe

import android.app.Application
import com.example.wardrobe.database.AppDatabase
import com.example.wardrobe.json_parser.WardrobeImporter
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@HiltAndroidApp
class WardrobeApplication : Application()