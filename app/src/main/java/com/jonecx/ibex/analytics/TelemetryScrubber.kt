package com.jonecx.ibex.analytics

// Replaces remote URLs / share paths with a stable hashed file_ref before they leave the device.
// A raw smb:// or http(s) URL can carry a credential or token in its userinfo/path/query, so it
// never ships verbatim; the hash still lets errors for the same file be grouped. Mirrors video_ref.
object TelemetryScrubber {

    private val URL_REGEX = Regex("""(?i)\b(smb|ftp|https?)://\S+""")

    fun scrub(text: String): String = URL_REGEX.replace(text) { match ->
        "${match.groupValues[1]}://${FileRef.hash(match.value)}"
    }

    fun scrub(properties: Map<String, Any?>): Map<String, Any?> =
        properties.mapValues { (_, value) -> if (value is String) scrub(value) else value }
}
