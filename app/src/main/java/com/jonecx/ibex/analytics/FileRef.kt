package com.jonecx.ibex.analytics

import java.security.MessageDigest

// Stable, privacy-preserving reference for a file or share path in telemetry, mirroring the
// Azmaree SDK's video_ref: a truncated SHA-256 of the asset. Credentials, query, and fragment are
// dropped first so the same file maps to one ref regardless of signed-URL rotation or login.
object FileRef {

    private const val HASH_BYTES = 8

    fun hash(reference: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(canonicalize(reference).toByteArray())
        return digest.take(HASH_BYTES).joinToString("") { "%02x".format(it) }
    }

    private fun canonicalize(reference: String): String {
        val asset = reference.substringBefore('#').substringBefore('?')
        val schemeEnd = asset.indexOf("://")
        if (schemeEnd < 0) return asset
        val scheme = asset.substring(0, schemeEnd + 3)
        val rest = asset.substring(schemeEnd + 3)
        val slash = rest.indexOf('/')
        val authority = if (slash >= 0) rest.substring(0, slash) else rest
        val path = if (slash >= 0) rest.substring(slash) else ""
        // Drop userinfo (user:pass@) so credentials never feed the hash.
        return scheme + authority.substringAfter('@') + path
    }
}
