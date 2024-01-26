package com.truv.webview.models

import org.json.JSONObject

data class Cookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("value", value)
            put("domain", domain)
            put("path", path)
            put("secure", secure)
            put("httpOnly", httpOnly)
        }
    }
}