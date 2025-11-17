package com.jholachhapdevs.pdfjuggler.feature.update.data.model

import com.jholachhapdevs.pdfjuggler.feature.update.domain.model.UpdateInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlatformDto(
    val downloadUrl: String,
    val checksum: String
)

@Serializable
data class PlatformsDto(
    val windows: PlatformDto? = null,
    val macos: PlatformDto? = null,
    val linux: PlatformDto? = null
)

@Serializable
data class UpdateInfoDto(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val mandatory: Boolean = false,
    val platforms: PlatformsDto,
    val changelog: String? = null
) {
    fun toDomain(): UpdateInfo {
        val os = System.getProperty("os.name").lowercase()
        val platform = when {
            os.contains("win") -> platforms.windows
            os.contains("mac") || os.contains("darwin") -> platforms.macos
            else -> platforms.linux
        }
        return UpdateInfo(
            versionCode = latestVersionCode,
            versionName = latestVersionName,
            mandatory = mandatory,
            downloadUrl = platform?.downloadUrl,
            checksum = platform?.checksum,
            changelogMarkdown = changelog ?: ""
        )
    }
}
