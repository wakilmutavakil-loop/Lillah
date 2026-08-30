package com.lillah.dhikr.ui.vm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lillah.dhikr.core.di.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided. Wrap the content in MainActivity's provider.")
}

/**
 * Builds a ViewModel from the app container without an annotation processor.
 *
 * Route arguments are captured by the lambda, so a screen scoped to an id gets a ViewModel scoped
 * to the same id by passing that id as [key].
 */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = LocalAppContainer.current
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(container) } },
    )
}
