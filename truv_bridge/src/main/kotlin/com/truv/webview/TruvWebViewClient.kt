package com.truv.webview

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.HttpURLConnection
import java.net.URL


class TruvWebViewClient(
    private val context: Context,
    private val eventListeners: Set<TruvEventsListener>,
    private val onLoaded: () -> Unit = {},
) : WebViewClient() {

    private var loadingFinished = true
    private var redirect = false

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): WebResourceResponse? {
        if (request.requestHeaders.containsKey("X-Requested-With")) {
            request.requestHeaders.remove("X-Requested-With")
        }
        return super.shouldInterceptRequest(view,request)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        if (!loadingFinished) {
            redirect = true;
        }

        loadingFinished = false;
        request?.let {
            view?.loadUrl(it.url.toString())
        }
        return true
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        eventListeners.forEach { it.onClose() }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        loadingFinished = false;
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (!redirect) {
            loadingFinished = true;
        }
        if (loadingFinished && !redirect) {
            onLoaded()
        } else {
            redirect = false;
        }

    }


//    private fun interceptRequest(
//        url: String,
//        requestHeaders: MutableMap<String, String>
//    ): WebResourceResponse? {
//
//        try {
//
//
//            val connection = URL(url).openConnection() as HttpURLConnection
//            // Удаление заголовка X-Requested-With
//            val remove = requestHeaders.remove("X-Requested-With")
//            remove?.let {
//                Log.d("", "removed")
//            }
//            for ((key, value) in requestHeaders) {
//                connection.setRequestProperty(key, value)
//            }
//            val inputStream = connection.inputStream
//            val mimeType = connection.getHeaderField("Content-Type")
//            val encoding = connection.contentEncoding
//            return WebResourceResponse(mimeType, encoding, inputStream)
//
//        } catch (ex: Exception) {
//            Log.e("",ex.localizedMessage)
//            return null
//        }
//    }
}