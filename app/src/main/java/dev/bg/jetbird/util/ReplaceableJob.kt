package dev.bg.jetbird.util

import kotlinx.coroutines.Job

class ReplaceableJob {
    private var job: Job? = null

    val isActive get() = job?.isActive ?: false

    @Synchronized
    operator fun plusAssign(newJob: Job) {
        cancel()
        job = newJob
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
