package com.jholachhapdevs.pdfjuggler.core.util

import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.Collections

object DeviceId {
    /**
     * Compute a best-effort stable device fingerprint based on MAC addresses and OS info.
     * Not guaranteed to be immutable, but good enough to detect simple file copying.
     */
    fun currentFingerprint(): String {
        return try {
            val macs = mutableListOf<ByteArray>()
            val ifaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (ni in ifaces) {
                if (!ni.isLoopback && ni.isUp) {
                    val mac = ni.hardwareAddress
                    if (mac != null && mac.isNotEmpty()) macs.add(mac)
                }
            }
            macs.sortBy { it.joinToString("") { b -> "%02x".format(b) } }
            val osName = System.getProperty("os.name") ?: "unknown-os"
            val osArch = System.getProperty("os.arch") ?: "unknown-arch"
            val user = System.getProperty("user.name") ?: "unknown-user"
            val payload = buildString {
                append(osName).append('|').append(osArch).append('|').append(user)
                macs.forEach { mac ->
                    append('|').append(mac.joinToString("") { b -> "%02x".format(b) })
                }
            }.toByteArray()
            sha256Hex(payload)
        } catch (t: Throwable) {
            // Fallback to a hash of minimal system props if interfaces failed
            val payload = (System.getProperty("os.name") + System.getProperty("os.arch") + System.getProperty("user.name")).toByteArray()
            sha256Hex(payload)
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { b -> "%02x".format(b) }
    }
}