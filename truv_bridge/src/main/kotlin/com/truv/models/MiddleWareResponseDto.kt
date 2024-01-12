package com.truv.models

import org.json.JSONObject

data class MiddleWareResponseDto(
    val headers: Headers?,
    val installation_id: String?,
    val payload: Payload?,
    val source: String?
) {
    data class Headers(
        val keyValue: Map<String,String>?
    ){
        companion object {
            fun parse(json: JSONObject): Headers {
                val headersMap = mutableMapOf<String,String>()
                json.keys().forEach {
                    headersMap[it] = json[it] as String
                }
                return Headers(headersMap)
            }
        }
    }

    data class Payload(
        val adpDeviceFingerprint: String?,
        val identifier: String?,
        val session: String?
    ){
        companion object {
            fun parse(json: JSONObject): Payload {
                return Payload(
                    adpDeviceFingerprint = json.optString("adpDeviceFingerprint"),
                    identifier = json.optString("identifier"),
                    session = json.optString("session"),
                )
            }
        }
    }

    companion object {
        fun parse(json: JSONObject): MiddleWareResponseDto {
            return MiddleWareResponseDto(
                installation_id = json.optString("installation_id"),
                payload = json.optJSONObject("payload")?.let { MiddleWareResponseDto.Payload.parse(it) },
                source = json.optString("source"),
                headers = json.optJSONObject("headers")?.let { MiddleWareResponseDto.Headers.parse(it) }
            )
        }
    }
}