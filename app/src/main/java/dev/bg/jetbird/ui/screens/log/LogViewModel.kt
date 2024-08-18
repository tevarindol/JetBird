package dev.bg.jetbird.ui.screens.log

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bg.jetbird.repository.LogRepository
import dev.bg.jetbird.util.IoDispatcher
import dev.bg.jetbird.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val logRepository: LogRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
): ViewModel() {
    val logEntries = logRepository.logEntries

    suspend fun dumpNetbirdLog(): List<String> = withContext(ioDispatcher) {
        val logLines = mutableListOf<String>()
        Runtime.getRuntime().exec("logcat -d -s GoLog")
            .inputStream
            .bufferedReader()
            .useLines {
                logLines.addAll(it.map { l -> Log.getLogcatLineContent(l) })
            }
        return@withContext logLines
    }

    fun clearLog() = logRepository.clear()
}
