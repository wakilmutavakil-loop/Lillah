package com.lillah.dhikr.ui.vm

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

/**
 * The hosting Activity, which both sign-in SDKs require: Credential Manager shows a system sheet
 * over it, and Facebook Login needs its ActivityResultRegistry.
 */
object LocalActivity {
    val current: Activity?
        @Composable
        @ReadOnlyComposable
        get() = LocalContext.current.findActivity()
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
