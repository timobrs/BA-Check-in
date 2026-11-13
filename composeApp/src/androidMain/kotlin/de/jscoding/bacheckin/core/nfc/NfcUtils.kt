package de.jscoding.bacheckin.core.nfc

fun bytesToHex(bytes: ByteArray): String = bytes.joinToString(separator = "") { "%02X".format(it) }

@Suppress("Unused")
fun hexToBytes(hex: String): ByteArray {
    val clean = hex.replace("[^0-9A-Fa-f]".toRegex(), "")
    return ByteArray(clean.length / 2) { i ->
        ((clean[i * 2].toString().toInt(16) shl 4) + clean[i * 2 + 1].toString().toInt(16)).toByte()
    }
}

