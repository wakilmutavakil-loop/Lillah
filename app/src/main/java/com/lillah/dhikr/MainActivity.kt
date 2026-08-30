package com.lillah.dhikr

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lillah.dhikr.core.di.AppContainer
import com.lillah.dhikr.ui.navigation.DhikrApp
import com.lillah.dhikr.ui.theme.DhikrTheme
import com.lillah.dhikr.ui.theme.ThemeMode
import com.lillah.dhikr.ui.vm.AppViewModel
import com.lillah.dhikr.ui.vm.LocalAppContainer

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer

    /** Mirrors the preference so the key handler can answer without touching a flow. */
    @Volatile
    private var volumeKeyCountingEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        container = (application as DhikrApplication).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                val appViewModel: AppViewModel = viewModel(
                    factory = remember {
                        viewModelFactory { initializer { AppViewModel(container) } }
                    },
                )
                val settings by appViewModel.settings.collectAsStateWithLifecycle()
                val systemDark = isSystemInDarkTheme()

                // Until preferences load, render with the defaults rather than a blank frame.
                val palette = settings?.palette ?: com.lillah.dhikr.ui.theme.ThemePalette.Default
                val mode = settings?.themeMode ?: ThemeMode.System
                val isDark = when (mode) {
                    ThemeMode.Light -> false
                    ThemeMode.Dark -> true
                    ThemeMode.System -> systemDark
                }

                volumeKeyCountingEnabled = settings?.countWithVolumeKeys == true

                // System bar icons follow the app's own theme, not the system's: choosing Dark
                // while the phone is in light mode must still give light status-bar icons.
                val view = LocalView.current
                SideEffect {
                    applyKeepScreenOn(settings?.keepScreenOn ?: true)
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !isDark
                        isAppearanceLightNavigationBars = !isDark
                    }
                }

                DhikrTheme(palette = palette, mode = mode) {
                    DhikrApp(isDark = isDark)
                }
            }
        }
    }

    private fun applyKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /**
     * Volume keys count when the user has asked them to, so a long sitting can be done with the
     * phone face-down. Consuming the event only while the preference is on leaves normal volume
     * control untouched otherwise, and key-up is swallowed too so the system UI never appears.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!volumeKeyCountingEnabled) return super.dispatchKeyEvent(event)

        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!isVolumeKey) return super.dispatchKeyEvent(event)

        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            container.emitHardwareCount(
                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) 1 else -1
            )
        }
        return true
    }
}
