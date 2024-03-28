package com.truv.models

import org.json.JSONObject

data class ExternalLoginConfig(
    val url: String = "",
    val selector: String = "",
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