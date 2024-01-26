package com.truv.models

import org.json.JSONException
import org.json.JSONObject
import kotlin.jvm.Throws

data class TruvSuccessPayload(
    val publicToken: String,
    val metadata: Metadata,
    val json: String
) {

    class Metadata(
        val taskId: String,
        val employer: TruvEmployer?
    )

    companion object {

        @Throws(JSONException::class)
        fun fromJson(json: String): TruvSuccessPayload {
            val payloadJson = JSONObject(json)

            val publicToken = payloadJson.getString("public_token")

            val metadataJson = payloadJson.getJSONObject("metadata")
            val taskId = metadataJson.getString("task_id")
            val employerJson = metadataJson.getJSONObject("employer")
            val employerName = employerJson.getString("name")

            val metadata = Metadata(taskId, TruvEmployer(employerName))

            return TruvSuccessPayload(publicToken, metadata, json)
        }

    }

}