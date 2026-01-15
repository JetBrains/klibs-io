package io.klibs.app.util

fun normalizeTag(raw: String): String {
    var v = raw.trim().lowercase()
    if (v.isEmpty()) return v
    v = v.replace(Regex("[^a-z0-9]+"), "-")
    v = v.replace(Regex("-{2,}"), "-")
    v = v.trim('-')
    return v
}