package com.jholachhapdevs.pdfjuggler.home.data.model

import com.jholachhapdevs.pdfjuggler.home.domain.model.UpdateInfo
import kotlinx.serialization.Serializable

@Serializable   // This annotation makes the class serializable by kotlinx.serialization, allowing it to be converted to and from formats like JSON.
data class UpdateInfoDto(
    val latestVersionCode: Double,
    val latestVersionName: String,
    val downloadMCA1: String,
    val downloadMCA3: String,
    val changelog: String
) {
    fun toDomain(): UpdateInfo {
        return UpdateInfo(
            latestVersionName = latestVersionName,
            downloadMCA3 = downloadMCA3
        )
    }
}