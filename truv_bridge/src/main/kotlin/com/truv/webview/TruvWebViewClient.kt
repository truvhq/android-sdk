package com.truv.webview

import android.content.Context
import android.content.Intent
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class TruvWebViewClient(
    private val context: Context,
    private val eventListeners: Set<TruvEventsListener>
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        request?.let {
            view?.loadUrl(it.url.toString())
        }
//        val i = Intent(Intent.ACTION_VIEW)
//        i.data = request?.url
//
//        context.startActivity(i)
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        eventListeners.forEach { it.onClose() }
    }

}