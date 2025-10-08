package com.jholachhapdevs.pdfjuggler.feature.update.domain.model

import com.jholachhapdevs.pdfjuggler.feature.update.data.model.UpdateInfoDto
import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val latestVersionName: String,
    val downloadMCA3: String,
) {
    fun toData(
        latestVersionCode: Double,
        downloadMCA1: String,
        changelog: String
    ): UpdateInfoDto {
        return UpdateInfoDto(
            latestVersionCode = latestVersionCode,
            latestVersionName = latestVersionName,
            downloadMCA1 = downloadMCA1,
            downloadMCA3 = downloadMCA3,
            changelog = changelog
        )
    }
}
