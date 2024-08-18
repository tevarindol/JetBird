package dev.bg.jetbird.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

interface LogRepository {
    val logEntries: StateFlow<List<String>>
    fun log(message: String)
    fun clear()
}

class LogRepositoryImpl @Inject constructor(): LogRepository {

    private val _logEntries: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    override val logEntries: StateFlow<List<String>>
        get() = _logEntries.asStateFlow()

    override fun log(message: String) {
        _logEntries.update {
            it.toMutableList().apply {
                add(message)
            }
        }
    }

    override fun clear() {
        _logEntries.update { emptyList() }
    }

}
