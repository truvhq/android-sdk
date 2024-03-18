package com.truv.models

import org.json.JSONObject

data class ResponseDto(
    val eventType: String?,
    val payload: Payload?
) {
    data class Error(
        val type: String?,
        val code: String?,
        val message: String?
    ) {
        companion object {
            fun parse(json: JSONObject): Error {
                return Error(
                    type = json.optString("error_type"),
                    code = json.optString("error_code"),
                    message = json.optString("error_message")
                )
            }
        }
    }

    data class Payload(
        val externalLoginType: String?,
        val isLoggedIn: IsLoggedIn?,
        val providerId: String?,
        val script: Script?,
        val url: String?,
        val bridgeToken: String?,
        val taskId: String?,
        val productType: String?,
        val publicToken: String?,
        val viewName: String?,
        val employer: Employer?,
        val error: Error?,
    ) {
        data class Employer(
            val name: String?
        ) {
            companion object {
                fun parse(json: JSONObject): Employer {
                    return Employer(
                        name = json.optString("name")
                    )
                }
            }
        }

        data class IsLoggedIn(
            val selector: String?
        ) {
            companion object {
                fun parse(json: JSONObject): IsLoggedIn {
                    return IsLoggedIn(
                        selector = json.optString("selector")
                    )
                }
            }
        }

        data class Script(
            val callbackHeaders: CallbackHeaders?,
            val callbackMethod: String?,
            val callbackUrl: String?,
            val url: String?
        ) {
            fun toJson(): JSONObject {
                return JSONObject().apply {
                    put("callback_headers", callbackHeaders?.toJson())
                    put("callback_method", callbackMethod)
                    put("callback_url", callbackUrl)
                    put("url", url)
                }
            }
            data class CallbackHeaders(
                val contentType: String?,
                val xAccessToken: String?
            ) {
                fun toJson(): JSONObject {
                    return JSONObject().apply {
                        put("Content-Type", contentType)
                        put("X-Access-Token", xAccessToken)
                    }
                }
                companion object {
                    fun parse(json: JSONObject): CallbackHeaders {
                        return CallbackHeaders(
                            contentType = json.optString("Content-Type"),
                            xAccessToken = json.optString("X-Access-Token")
                        )
                    }
                }
            }

            companion object {
                fun parse(json: JSONObject): Script {
                    return Script(
                        callbackHeaders = json.optJSONObject("callback_headers")
                            ?.let { CallbackHeaders.parse(it) },
                        callbackMethod = json.optString("callback_method"),
                        callbackUrl = json.optString("callback_url"),
                        url = json.optString("url"),
                    )
                }

            }
        }

        companion object {
            fun parse(json: JSONObject): Payload {
                return Payload(
                    externalLoginType = json.optString("external_login_type"),
                    isLoggedIn = json.optJSONObject("is_logged_in")?.let { IsLoggedIn.parse(it) },
                    providerId = json.optString("provider_id"),
                    script = json.optJSONObject("script")?.let { Script.parse(it) },
                    url = json.optString("url"),
                    bridgeToken = json.optString("bridge_token"),
                    taskId = json.optString("task_id"),
                    productType = json.optString("product_type"),
                    publicToken = json.optString("public_token"),
                    viewName = json.optString("view_name"),
                    error = json.optJSONObject("error")?.let { Error.parse(it) },
                    employer = json.optJSONObject("employer")?.let { Employer.parse(it) },
                )
            }
        }
    }

    companion object {
        fun parse(json: JSONObject): ResponseDto {
            return ResponseDto(
                eventType = json.optString("event_type"),
                payload = json.optJSONObject("payload")?.let { Payload.parse(it) },
            )
        }
    }

}

