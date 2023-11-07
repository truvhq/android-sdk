package com.truv.models

sealed class TruvEvent {
    data object Close : TruvEvent()
    data object Error : TruvEvent()
    class Event(val payload: TruvEventPayload?) : TruvEvent()
    data object Load : TruvEvent()
    class Success(val payload: TruvSuccessPayload?) : TruvEvent()
}