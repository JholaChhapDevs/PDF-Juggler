package com.jholachhapdevs.pdfjuggler.core.datastore

import com.jholachhapdevs.pdfjuggler.core.util.DeviceId

object ApiKeyStore {
    private const val GEMINI_API_KEY = "gemini_api_key"
    private const val GEMINI_API_KEY_DEVICE = "gemini_api_key_device_id"

    suspend fun getGeminiApiKey(): String? {
        val key = PrefsManager.getString(GEMINI_API_KEY) ?: return null
        val savedDevice = PrefsManager.getString(GEMINI_API_KEY_DEVICE) ?: return null
        val currentDevice = DeviceId.currentFingerprint()
        return if (savedDevice == currentDevice) key else null
    }

    suspend fun saveGeminiApiKey(value: String) {
        val currentDevice = DeviceId.currentFingerprint()
        PrefsManager.saveString(GEMINI_API_KEY, value)
        PrefsManager.saveString(GEMINI_API_KEY_DEVICE, currentDevice)
    }

    suspend fun clearGeminiApiKey() {
        PrefsManager.delete(GEMINI_API_KEY)
        PrefsManager.delete(GEMINI_API_KEY_DEVICE)
    }
}