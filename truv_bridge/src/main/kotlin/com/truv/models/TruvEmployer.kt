package com.truv.models

import org.json.JSONObject

data class TruvEmployer(val name: String){
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
        }
    }
}
