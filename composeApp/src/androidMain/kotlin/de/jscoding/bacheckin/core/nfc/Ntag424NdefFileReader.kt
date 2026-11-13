package de.jscoding.bacheckin.core.nfc

internal class Ntag424NdefFileReader(private val transceive: (ByteArray) -> ByteArray) {
    fun readMessageBytes(): ByteArray {
        exchange(
            byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x0C, 0x07,
                0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01),
            "NDEF-Anwendung auswählen"
        )
        exchange(
            byteArrayOf(0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0xE1.toByte(), 0x04),
            "NDEF-Datei E104 auswählen"
        )

        val file = exchange(byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x00), "NDEF-Datei lesen")
        check(file.size in 2..256) { "Die NDEF-Datei hat eine ungültige Länge." }
        val messageLength = ((file[0].toInt() and 0xFF) shl 8) or (file[1].toInt() and 0xFF)
        check(messageLength > 0) { "Die NDEF-Datei ist leer. Die Karte muss personalisiert werden." }
        check(messageLength <= file.size - 2) { "Die NDEF-Nachricht ist unvollständig oder ihre Länge ist ungültig." }
        return file.copyOfRange(2, 2 + messageLength)
    }

    private fun exchange(command: ByteArray, operation: String): ByteArray {
        val response = transceive(command)
        check(response.size >= 2) { "$operation: Die Karte hat keine gültige Antwort geliefert." }
        val status = ((response[response.size - 2].toInt() and 0xFF) shl 8) or
            (response.last().toInt() and 0xFF)
        val statusHex = status.toString(16).uppercase().padStart(4, '0')
        check(status == 0x9000) {
            when (status) {
                0x6A82 -> "$operation: NDEF-Anwendung oder Datei nicht gefunden (SW=$statusHex)."
                0x6982 -> "$operation: Die Karte verweigert den Zugriff; Leserechte und SDM-Zähler prüfen (SW=$statusHex)."
                else -> "$operation: Kartenfehler (SW=$statusHex)."
            }
        }
        return response.copyOfRange(0, response.size - 2)
    }
}
