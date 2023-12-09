package com.truv.webview

import com.truv.models.TruvEventPayload
import com.truv.models.TruvSuccessPayload

interface TruvEventsListener {

    fun onSuccess(payload: TruvSuccessPayload)

    fun onEvent(event: TruvEventPayload)

    fun onClose()

    fun onLoad()

    fun onError()

}