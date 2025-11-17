package com.jholachhapdevs.pdfjuggler.feature.update.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val mandatory: Boolean,
    val downloadUrl: String?,
    val checksum: String?,
    val changelogMarkdown: String
)
