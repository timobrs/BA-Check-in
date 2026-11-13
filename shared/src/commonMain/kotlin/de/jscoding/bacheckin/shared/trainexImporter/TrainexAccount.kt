package de.jscoding.bacheckin.shared.trainexImporter

import de.jscoding.bacheckin.shared.Secrets
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TrainexAccount(val username: String, val password: String)

@Serializable
data class SecretsFile(
    @SerialName("trainex-accounts") val trainexAccounts: List<TrainexAccount>
)

private val secretsParser = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

fun getTrainexAccounts(): List<TrainexAccount> =
    secretsParser.decodeFromString<SecretsFile>(Secrets.accountsJson).trainexAccounts
