package com.truv.models

import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws

data class TruvEventPayload(
    val payload: Payload?,
    val eventType: EventType
) {

    class Payload(
        val bridgeToken: String?,
        val productType: String?,
        val viewName: String?,
        val employer: TruvEmployer?,
        val publicToken: String?,
        val taskId: String?,
        val providerId: String?,
        val error: TruvError?
    )


    enum class EventType(event: String) {
        LOAD("LOAD"),
        OPEN("OPEN"),
        SCREEN_VIEW("SCREEN_VIEW"),
        EMPLOYER_SELECTED("EMPLOYER_SELECTED"),
        LINK_CREATED("LINK_CREATED"),
        LOGIN_COMPLETE("LOGIN_COMPLETE"),
        SUCCESS("SUCCESS"),
        ERROR("ERROR"),
        UNSUPPORTED_BROWSER("UNSUPPORTED_BROWSER"),
        CLOSE("CLOSE")
    }

    companion object {

        @Throws(JSONException::class)
        fun fromJson(event: String): TruvEventPayload {
            val json = JSONObject(event)
            val type = json.getString("event_type")
            val payloadJson = json.optJSONObject("payload")

            val payload = payloadJson?.let { payloadJson ->
                val bridgeToken = payloadJson.optString("bridge_token")
                val taskId = payloadJson.optString("task_id")
                val productType = payloadJson.optString("product_type")
                val publicToken = payloadJson.optString("public_token")
                val viewName = payloadJson.optString("view_name")
                val providerId = payloadJson.optString("provider_id")

                val employerJson = payloadJson.optJSONObject("employer")
                val employer = employerJson?.let { employerJson ->
                    TruvEmployer(employerJson.getString("name"))
                }

                val errorJson = payloadJson.optJSONObject("error")
                val error = errorJson?.let { errorJson ->
                    TruvError(
                        type = errorJson.getString("type"),
                        code = TruvError.ErrorCode.valueOf(errorJson.getString("code")),
                        message = errorJson.getString("message")
                    )
                }

                Payload(
                    bridgeToken = bridgeToken,
                    productType = productType,
                    viewName = viewName,
                    employer = employer,
                    publicToken = publicToken,
                    taskId = taskId,
                    providerId = providerId,
                    error = error,
                )
            }

            return TruvEventPayload(eventType = EventType.valueOf(type), payload = payload)
        }

    }

}