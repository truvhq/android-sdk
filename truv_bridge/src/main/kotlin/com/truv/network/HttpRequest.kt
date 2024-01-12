package com.truv.network


import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class HttpRequest(
    val url: String,
    val body: String = "",
    val headers: Map<String, String> = mapOf(),
    val config: ((HttpURLConnection) -> Unit)? = null
) {
    fun response(completion: (HttpResponse) -> Unit) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val url = URL(url)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                headers.forEach { (key, value) ->
                    connection.setRequestProperty(key, value)
                }
                config?.let { it(connection) }
                connection.doOutput = true
                connection.outputStream.use {
                    it.write(body.toByteArray())
                }
                val response = HttpResponse()
                response.connection = connection
                //for debug reasons
//                val responseMessage = connection.responseMessage
//                val responseCode = connection.responseCode
//                val bufferere = BufferedReader( InputStreamReader(connection.getErrorStream()))
//                var strCurrentLine: String?
//                while (bufferere.readLine().also { strCurrentLine = it } != null) {
//                    println(strCurrentLine)
//                }
                val use = connection.inputStream.use {
                    BufferedReader(InputStreamReader(it)).use { reader ->
                        reader.readText()
                    }
                }
                response.body = use
                connection.disconnect()
                completion(response)
            } catch (e: Exception) {
                val response = HttpResponse()
                response.exception = e
                completion(response)
            }
        }
    }

    companion object {
        var json = Json { ignoreUnknownKeys = true }
    }

    inline fun <reified T> json(crossinline completion: (T?, HttpResponse) -> Unit) where T : Any {
        response { response ->
            try {
                val body = response.body
                val result = if (body != null) json.decodeFromString<T>(body) else null
                completion(result, response)
            } catch (e: Exception) {
                response.exception = e
                completion(null, response)
            }
        }
    }
}

class HttpResponse {
    var connection: HttpURLConnection? = null
    var body: String? = null
    var exception: Exception? = null
    val success: Boolean get() {
        connection?.let { return it.responseCode in 200..299 }
        return false
    }
}
