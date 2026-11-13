package de.jscoding.bacheckin.shared.trainexImporter

import io.ktor.client.HttpClient

expect fun createTrainexHttpClient(): HttpClient
