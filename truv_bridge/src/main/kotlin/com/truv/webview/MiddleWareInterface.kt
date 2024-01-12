package com.truv.webview

import android.util.Log
import android.webkit.JavascriptInterface

class MiddleWareInterface(val onCallCallback : (String) -> Unit) {
    
    @JavascriptInterface
    fun call(response: String) {
        Log.d(TAG, "call invoked")
        onCallCallback(response)
    }

    companion object {
        const val TAG = "MiddleWareInterface"
    }

}