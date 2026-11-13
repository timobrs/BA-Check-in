package de.jscoding.bacheckin.core.nfc

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.Ndef
import de.jscoding.bacheckin.shared.nfc.NfcSdmUrlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Ntag424DnaHandler(private val operationMode: Mode) {
    enum class Mode { DEMO, REAL }

    data class NtagScanData(
        val cardPublicId: String,
        val encryptedPiccDataHex: String,
        val sdmMacHex: String
    )

    suspend fun readTagScanData(tag: Tag): NtagScanData = withContext(Dispatchers.IO) {
        val discoveredTagUidBytes = tag.id ?: ByteArray(0)
        val discoveredTagUidHex = bytesToHex(discoveredTagUidBytes).uppercase()

        when (operationMode) {
            Mode.DEMO -> {
                NtagScanData(
                    cardPublicId = "123e4567-e89b-42d3-a456-426614174000",
                    encryptedPiccDataHex = discoveredTagUidHex.padEnd(32, '0').take(32),
                    sdmMacHex = "0011223344556677"
                )
            }

            Mode.REAL -> readSecureDynamicMessage(tag)
        }
    }

    private fun readSecureDynamicMessage(tag: Tag): NtagScanData {
        val ndefMessage = readNdefMessage(tag)
        val secureDynamicMessageUri = ndefMessage.records.singleOrNull()?.toUri()
            ?: error("Die NFC-Karte muss genau einen SDM-Link enthalten.")
        val parsed = NfcSdmUrlParser.parse(secureDynamicMessageUri.toString())
        return NtagScanData(
            parsed.cardPublicId,
            parsed.encryptedPiccDataHex,
            parsed.sdmMacHex
        )
    }

    private fun readNdefMessage(tag: Tag): NdefMessage {
        val ndefTechnologyConnection = Ndef.get(tag)
            ?: return readNdefFileDirectly(tag)
        val ndefMessage = try {
            ndefTechnologyConnection.connect()
            ndefTechnologyConnection.ndefMessage
        } finally {
            runCatching { ndefTechnologyConnection.close() }
        }
        return ndefMessage ?: readNdefFileDirectly(tag)
    }

    private fun readNdefFileDirectly(tag: Tag): NdefMessage {
        val connection = IsoDep.get(tag)
            ?: error("Die Karte wurde weder als NDEF noch als ISO-DEP erkannt. Eine personalisierte NTAG-424-DNA-Karte ist erforderlich.")
        return try {
            connection.connect()
            val messageBytes = Ntag424NdefFileReader(connection::transceive).readMessageBytes()
            NdefMessage(messageBytes)
        } finally {
            runCatching { connection.close() }
        }
    }
}
