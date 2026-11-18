package com.okuyanstudio.gecepostasi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    private lateinit var sound: Sound

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )
        super.onCreate(savedInstanceState)
        sound = Sound(applicationContext)
        val save = SaveData(applicationContext).also { it.load() }
        val langs = Lang.loadAll(applicationContext)
        setContent {
            val scope = rememberCoroutineScope()
            val st = remember { AppState(save, langs, sound, scope) }
            LaunchedEffect(Unit) { st.showStart() }
            GameApp(st)
        }
    }

    override fun onStart() { super.onStart(); sound.resume() }
    override fun onStop() { super.onStop(); sound.pause() }
    override fun onDestroy() { super.onDestroy(); if (isFinishing) sound.release() }
}
