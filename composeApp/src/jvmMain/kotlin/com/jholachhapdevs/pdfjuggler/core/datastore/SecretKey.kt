package com.jholachhapdevs.pdfjuggler.core.datastore

import com.jholachhapdevs.pdfjuggler.core.util.Env
import javax.crypto.spec.SecretKeySpec

object SecretKey {
    fun get() = SecretKeySpec(Env.PREFS_KEY.toByteArray(), "AES")
}