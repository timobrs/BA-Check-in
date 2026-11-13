package de.jscoding.bacheckin.shared.trainexImporter

import co.touchlab.kermit.Logger
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters

class TrainexImporter(
    trainexUrl: String,
    private val clientFactory: () -> HttpClient = { createTrainexHttpClient() },
) {
    private val baseUrl = trainexUrl.trim().let { if (it.endsWith('/')) it else "$it/" }

    suspend fun downloadCalendars(accounts: List<TrainexAccount>): List<String> {
        require(accounts.isNotEmpty()) { "Keine Trainex-Konten konfiguriert." }

        return accounts.mapIndexed { index, account ->
            require(account.username.isNotBlank() && account.password.isNotBlank()) {
                "Trainex-Konto ${index + 1} ist unvollständig konfiguriert."
            }

            val client = clientFactory()
            try {
                downloadCalendar(client, account.username, account.password)
            } catch (error: TrainexImporterException) {
                throw TrainexImporterException("Trainex-Konto ${index + 1}: ${error.message}", error)
            } catch (error: Exception) {
                throw TrainexImporterException(
                    "Trainex-Konto ${index + 1}: Verbindung zu Trainex fehlgeschlagen.",
                    error,
                )
            } finally {
                client.close()
            }
        }
    }

    private suspend fun downloadCalendar(client: HttpClient, login: String, password: String): String {
        Logger.d { "Trainex calendar download started" }

        requireSuccess(client.get(baseUrl), "Startseite")

        val loginResponse = client.submitForm(
            url = baseUrl + "start.cfm?eng=0",
            formParameters = Parameters.build {
                append("Login", login)
                append("Passwort", password)
                append("Domaene", "0")
                append("einloggen", "Anmelden")
            },
        )
        val loginHtml = requireSuccess(loginResponse, "Anmeldung")
        val loginDocument = Ksoup.parse(loginHtml, baseUrl + "navigation/")

        if (loginWasRejected(loginHtml, loginDocument)) {
            throw TrainexImporterException(
                "Anmeldung fehlgeschlagen. Benutzername oder Passwort in secrets.json prüfen.",
            )
        }

        val studyPlanUrl = loginDocument.findUrl("value", "studienplan")
            ?: throw TrainexImporterException("Studienplan-Link wurde nach erfolgreicher Anmeldung nicht gefunden.")

        val studyPlanHtml = requireSuccess(client.get(studyPlanUrl), "Studienplan")
        val studyPlanDocument = Ksoup.parse(studyPlanHtml, baseUrl + "navigation/")
        val scheduleUrl = studyPlanDocument.findUrl("src", "einsatzplan")
            ?: throw TrainexImporterException("Einsatzplan-Link wurde nicht gefunden.")

        val scheduleHtml = requireSuccess(client.get(scheduleUrl), "Einsatzplan")
        val scheduleDocument = Ksoup.parse(scheduleHtml, baseUrl + "cfm/einsatzplan/")
        val listViewUrl = scheduleDocument.findUrl("href", "listenansicht")
            ?: throw TrainexImporterException("Listenansicht-Link wurde nicht gefunden.")

        val listViewHtml = requireSuccess(client.get(listViewUrl), "Listenansicht")
        val listViewDocument = Ksoup.parse(listViewHtml, baseUrl + "cfm/einsatzplan/")
        val calendarUrl = listViewDocument.findUrl("href", "einsatzplan_listenansicht_ical")
            ?: throw TrainexImporterException("iCal-Download-Link wurde nicht gefunden.")

        val calendar = requireSuccess(client.get(calendarUrl), "iCal-Download")
        if (!calendar.contains("BEGIN:VCALENDAR", ignoreCase = true)) {
            throw TrainexImporterException("Trainex hat keinen gültigen iCal-Kalender geliefert.")
        }

        studyPlanDocument.findUrl("href", "logout")?.let { logoutUrl ->
            runCatching { client.get(logoutUrl) }
        }

        Logger.d { "Trainex calendar download finished" }
        return calendar
    }

    private suspend fun requireSuccess(response: HttpResponse, step: String): String {
        if (response.status.value !in 200..299) {
            throw TrainexImporterException("$step fehlgeschlagen (HTTP ${response.status.value}).")
        }
        return response.bodyAsText()
    }

    private fun loginWasRejected(html: String, document: Document): Boolean {
        val normalized = html.lowercase()
        return "login existiert nicht" in normalized ||
            "passwort nicht korrekt" in normalized ||
            document.select("input[name=Login], input[name=Passwort]").isNotEmpty()
    }

    private fun Document.findUrl(attribute: String, token: String): String? {
        val normalizedToken = token.lowercase()
        return select("option[$attribute], iframe[$attribute], a[$attribute]")
            .firstOrNull { element ->
                val value = element.attr(attribute)
                value.contains(normalizedToken, ignoreCase = true) ||
                    element.text().contains(normalizedToken, ignoreCase = true)
            }
            ?.absUrl(attribute)
            ?.takeIf(String::isNotBlank)
    }
}

class TrainexImporterException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
