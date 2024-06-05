package com.truv.network


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class HttpRequest(
    val url: String,
    val body: String? = null,
    val method: String = "POST",
    val headers: Map<String, String> = mapOf(),
    val config: ((HttpURLConnection) -> Unit)? = null
) {
    suspend fun response() = withContext(Dispatchers.IO) {
        var resultResponse: HttpResponse? = null
        try {
            val url = URL(url)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = method
            headers.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }
            config?.let { it(connection) }
            connection.doOutput = if (method == "POST") true else false
            body?.let {
                connection.outputStream.use {
                    it.write(body.toByteArray())
                }
            }

            val response = HttpResponse()
            response.connection = connection
            //for debug reasons
            val responseMessage = connection.responseMessage
            val responseCode = connection.responseCode
            connection.errorStream?.bufferedReader().use { reader ->
                var strCurrentLine: String?
                while (reader?.readLine().also { strCurrentLine = it } != null) {
                    println(strCurrentLine)
                }
            }

            val responseBody = connection.inputStream.use {
                BufferedReader(InputStreamReader(it)).use { reader ->
                    reader.readText()
                }
            }
            response.body = responseBody
            connection.disconnect()
            resultResponse = response
        } catch (e: Exception) {
            val response = HttpResponse()
            response.exception = e
            resultResponse = response
        }
        resultResponse
    }


    companion object {
        var json = Json { ignoreUnknownKeys = true }
    }

    suspend inline fun <reified T> json(): T? where T : Any {
        val body = response()?.body
        return if (body != null) json.decodeFromString<T>(body) else null
    }
}

class HttpResponse {
    var connection: HttpURLConnection? = null
    var body: String? = null
    var exception: Exception? = null
    val success: Boolean
        get() {
            connection?.let { return it.responseCode in 200..299 }
            return false
        }
}
