package com.jholachhapdevs.pdfjuggler.core.networking

import io.ktor.client.engine.okhttp.OkHttp

fun getEngine() = OkHttp.create()