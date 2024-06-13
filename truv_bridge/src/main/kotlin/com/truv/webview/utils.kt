package com.truv.webview

import java.net.URL

fun getDomainFromUrl(urlString: String): String {
    return try {
        val url = URL(urlString)
        url.host
    } catch (e: Exception) {
        e.printStackTrace()
        urlString
    }
}
