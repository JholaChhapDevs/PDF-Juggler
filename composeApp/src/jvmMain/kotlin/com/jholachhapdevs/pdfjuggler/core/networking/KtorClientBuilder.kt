package com.jholachhapdevs.pdfjuggler.core.networking

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun buildHttpClient(engine: HttpClientEngine): HttpClient {
    println("KTOR_HTTP_CLIENT_BUILDING")
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(
                json = Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                }
            )
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
            level = LogLevel.BODY
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 6000
            connectTimeoutMillis = 3000
            socketTimeoutMillis = 6000
        }
    }
}