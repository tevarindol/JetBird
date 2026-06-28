package dev.bg.jetbird.ui.screens.home

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bg.jetbird.repository.LogRepository
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.util.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val logRepository: LogRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
): ViewModel() {
    fun hasAuthed() = preferencesRepository.hasAuthed

    fun getSetupKey() = preferencesRepository.setupKey

    fun updateHasAuthed() {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.hasAuthed = true
            preferencesRepository.updateState()
        }
    }

    fun generateQrCode(uri: String?): Bitmap? {
        if (uri == null) return null
        return try {
            val matrix = QRCodeWriter().encode(
                uri,
                BarcodeFormat.QR_CODE,
                512,
                512,
                mapOf(
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                    EncodeHintType.MARGIN to 2
                )
            )
            val bitmap = createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            for (x in 0 until 512) {
                for (y in 0 until 512) {
                    bitmap[x, y] = if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            bitmap
        } catch (e: WriterException) {
            logRepository.log("Failed to write QR code: $e")
            null
        }
    }
}
