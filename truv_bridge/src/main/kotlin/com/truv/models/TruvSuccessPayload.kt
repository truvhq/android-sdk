package com.truv.models

data class TruvSuccessPayload(
    val publicToken: String,
    val metadata: Metadata
) {

    class Metadata(
        val taskId: String,
        val employer: TruvEmployer?
    )

}