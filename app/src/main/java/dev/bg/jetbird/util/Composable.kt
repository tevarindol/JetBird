package dev.bg.jetbird.util

import android.content.res.Configuration
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import dev.bg.jetbird.data.LayoutType

@Composable
fun OnLifecycleEvent(
    onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit
) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { owner, event ->
            eventHandler.value(owner, event)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun OnLayoutChange(
    onEvent: (layoutType: LayoutType) -> Unit
) {
    val configuration = LocalConfiguration.current

    val windowSize = currentWindowAdaptiveInfo().windowSizeClass

    LaunchedEffect(configuration) {
        snapshotFlow { configuration.orientation }
            .collect { orientation ->
                when (orientation) {
                    Configuration.ORIENTATION_PORTRAIT -> {
                        onEvent(
                            when (windowSize.windowWidthSizeClass) {
                                WindowWidthSizeClass.COMPACT -> LayoutType.CompactPortrait
                                WindowWidthSizeClass.MEDIUM,
                                WindowWidthSizeClass.EXPANDED -> LayoutType.WidePortrait
                                else -> LayoutType.CompactPortrait
                            }
                        )
                    }
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        onEvent(
                            when {
                                windowSize.windowWidthSizeClass == WindowWidthSizeClass.COMPACT && windowSize.windowHeightSizeClass == WindowHeightSizeClass.COMPACT -> LayoutType.CompactLandscape
                                windowSize.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM -> LayoutType.WideLandscape
                                windowSize.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED -> LayoutType.PanelLandscape
                                else -> LayoutType.CompactLandscape
                            }
                        )
                    }
                    else -> onEvent(LayoutType.CompactLandscape)
                }
            }
    }
}
