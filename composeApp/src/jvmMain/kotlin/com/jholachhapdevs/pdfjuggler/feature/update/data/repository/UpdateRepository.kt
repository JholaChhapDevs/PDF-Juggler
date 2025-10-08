package com.jholachhapdevs.pdfjuggler.feature.update.data.repository

import com.jholachhapdevs.pdfjuggler.core.networking.httpClient
import com.jholachhapdevs.pdfjuggler.feature.update.data.model.UpdateInfoDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

class UpdateRepository {
    suspend fun getUpdateData(): UpdateInfoDto? { // '?' means it can return null, in kotlin by default everything is non-null, here its explicitly mentioned that it can return null which we need to take care of while calling this function.
        return try {
            val url = "https://kanha321.github.io/Upastithi/update.json"
            val result = httpClient.get(url)
            if (result.status == HttpStatusCode.OK) {
                result.body()  // equivalent to return result.body<UpdateInfoDto>()
            } else {
                null  // equivalent to return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null  // equivalent to return null
        }
    }

    // in kotlin if-else and try-catch are expressions, they return value.
}