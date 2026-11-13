package de.jscoding.bacheckin.core.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class NfcReader(private val activity: FragmentActivity) {
    private var readerCallback: NfcAdapter.ReaderCallback? = null
    private var enabled = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val processingTag = AtomicBoolean(false)

    fun start(onTagDiscovered: suspend (Tag) -> Unit, onError: suspend (Throwable) -> Unit = {}): Boolean {
        if (enabled) return true
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity) ?: return false
        if (!nfcAdapter.isEnabled) return false

        readerCallback = NfcAdapter.ReaderCallback { discoveredTag ->
            if (!processingTag.compareAndSet(false, true)) return@ReaderCallback
            scope.launch {
                try {
                    onTagDiscovered(discoveredTag)
                } catch (error: Exception) {
                    onError(error)
                }
            }
        }

        val readerModeFlags = NfcAdapter.FLAG_READER_NFC_A
        activity.runOnUiThread {
            nfcAdapter.enableReaderMode(activity, readerCallback, readerModeFlags, null)
            enabled = true
        }
        return true
    }

    fun stop() {
        if (!enabled) return
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        activity.runOnUiThread {
            nfcAdapter.disableReaderMode(activity)
            enabled = false
            processingTag.set(false)
        }
    }
}
