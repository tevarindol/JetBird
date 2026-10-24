package dev.bg.jetbird.lib

import dev.bg.jetbird.data.model.ServiceEvent
import dev.bg.jetbird.data.model.VPNState
import io.netbird.android.URLOpener
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface EngineBinder {
    val state: StateFlow<VPNState>
    val events: SharedFlow<ServiceEvent>

    fun start(urlOpener: URLOpener?)
    fun stop()
    fun selectRoute(route: String)
    fun deselectRoute(route: String)
}
