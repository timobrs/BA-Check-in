package de.jscoding.bacheckin.shared.nfc

data class NfcSdmPayload(
    val cardPublicId: String,
    val encryptedPiccDataHex: String,
    val sdmMacHex: String
)

object NfcSdmUrlParser {
    private const val PREFIX = "https://bacheckin.brettspielrunde.com/nfc/scan?"
    private val uuid = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
    private val piccData = Regex("^[0-9a-fA-F]{32}$")
    private val cmac = Regex("^[0-9a-fA-F]{16}$")

    @Throws(IllegalArgumentException::class)
    fun parse(url: String): NfcSdmPayload {
        require(url.startsWith(PREFIX) && !url.contains('#')) { "Der NFC-Link hat einen ungültigen Host oder Pfad." }
        val parameters = url.removePrefix(PREFIX).split('&').map { part ->
            val separator = part.indexOf('=')
            require(separator > 0 && separator == part.lastIndexOf('=')) { "Der NFC-Link enthält ungültige Parameter." }
            part.substring(0, separator) to part.substring(separator + 1)
        }
        require(parameters.size == 3 && parameters.map { it.first }.toSet() == setOf("id", "picc_data", "cmac")) {
            "Der NFC-Link muss exakt id, picc_data und cmac enthalten."
        }
        val values = parameters.toMap()
        val id = values.getValue("id")
        val encrypted = values.getValue("picc_data")
        val mac = values.getValue("cmac")
        require(uuid.matches(id)) { "Die öffentliche Karten-ID ist ungültig." }
        require(piccData.matches(encrypted)) { "PICCData muss aus 32 Hex-Zeichen bestehen." }
        require(cmac.matches(mac)) { "Der SDM-CMAC muss aus 16 Hex-Zeichen bestehen." }
        return NfcSdmPayload(id.lowercase(), encrypted.uppercase(), mac.uppercase())
    }
}
