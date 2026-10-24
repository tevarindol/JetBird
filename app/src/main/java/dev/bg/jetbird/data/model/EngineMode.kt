package dev.bg.jetbird.data.model

enum class EngineMode {
    VPN,
    PROXY
}

object ActiveEngine {
    @Volatile private var mode: EngineMode? = null
    @Volatile private var stop: (() -> Unit)? = null

    @Synchronized
    fun claim(newMode: EngineMode, stopEngine: () -> Unit) {
        if (mode != null && mode != newMode) {
            stop?.invoke()
        }
        mode = newMode
        stop = stopEngine
    }

    @Synchronized
    fun release(released: EngineMode) {
        if (mode == released) {
            mode = null
            stop = null
        }
    }
}
