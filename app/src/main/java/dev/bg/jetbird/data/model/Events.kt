package dev.bg.jetbird.data.model

sealed class BaseEvent {
    abstract val event: String
}

data class ServiceEvent(
    override val event: String
): BaseEvent()
