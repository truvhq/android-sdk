package com.truv.models

import org.json.JSONObject

data class ExternalLoginConfig(
    val url: String? = null,
    val selector: String? = null,
    val scriptUrl: String? = null,
    val script: ResponseDto.Payload.Script? = null,
){
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("url", url)
            put("selector", selector)
            put("script", script?.toJson())
        }
    }
}