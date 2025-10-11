package com.jholachhapdevs.pdfjuggler.feature.ai.data.remote

object ImageTokenParser {

    private val token = Regex("\\[\\[(image/[a-zA-Z0-9+\\-_.]+);base64,([A-Za-z0-9+/=]+)]]")

    sealed class Part {
      data class Text(val text: String) : Part()
      data class Image(val mimeType: String, val base64: String) : Part()
    }

    fun parseToParts(source: String): List<Part> {
        val parts = mutableListOf<Part>()
        var cursor = 0
        token.findAll(source).forEach { m ->
            if (m.range.first > cursor) {
                val chunk = source.substring(cursor, m.range.first)
                if (chunk.isNotBlank()) parts += Part.Text(chunk)
            }
            val mime = m.groupValues[1]
            val b64 = m.groupValues[2]
            parts += Part.Image(mime, b64)
            cursor = m.range.last + 1
        }
        if (cursor < source.length) {
            val tail = source.substring(cursor)
            if (tail.isNotBlank()) parts += Part.Text(tail)
        }
        return parts
    }
}
