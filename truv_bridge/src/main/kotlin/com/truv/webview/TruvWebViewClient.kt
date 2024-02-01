package com.truv.webview

import android.content.Context
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient


class TruvWebViewClient(
    private val context: Context,
    private val eventListeners: Set<TruvEventsListener>,
    private val onLoading: (Boolean) -> Unit = {},
    private val onLoaded: () -> Unit = {},
    private val openExternalLinkInAppBrowser:((String) -> Unit)? = null ,
) : WebViewClient() {
    private val seenURLs = mutableSetOf<String>()

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): WebResourceResponse? {
        if (request.requestHeaders.containsKey("X-Requested-With")) {
            request.requestHeaders.remove("X-Requested-With")
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        Log.d("TruvWebViewClient", "URL: ${request?.url}")
        onLoading(true)
        request?.let {
            view?.loadUrl(it.url.toString())
            openExternalLinkInAppBrowser?.invoke(it.url.toString())
        }

        seenURLs.add(request?.url.toString())
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        eventListeners.forEach { it.onClose() }
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        onLoading(false)
        onLoaded()
    }

    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
        Log.d("TruvWebViewClient", "doUpdateVisitedHistory: $url")
        if (url != null) {
            seenURLs.add(url)
        }
        super.doUpdateVisitedHistory(view, url, isReload)
    }

    fun getSeenPages(): Set<String> {
        return seenURLs
    }

}