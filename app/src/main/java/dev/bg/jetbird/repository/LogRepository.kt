package dev.bg.jetbird.repository

import android.annotation.SuppressLint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject

interface LogRepository {
    val logEntries: StateFlow<List<String>>
    fun log(
        message: String,
        timestamp: Boolean = true
    )
    fun clear()
}

@SuppressLint("SimpleDateFormat")
class LogRepositoryImpl @Inject constructor(): LogRepository {

    private val _logEntries: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    override val logEntries: StateFlow<List<String>>
        get() = _logEntries.asStateFlow()

    override fun log(
        message: String,
        timestamp: Boolean
    ) {
        _logEntries.update {
            it.toMutableList().apply {
                if (timestamp) {
                    add("[${SimpleDateFormat("HH:mm:ss.SSS").format(Date(System.currentTimeMillis()))}] $message")
                } else {
                    add(message)
                }
            }
        }
    }

    override fun clear() {
        _logEntries.update { emptyList() }
    }

}
