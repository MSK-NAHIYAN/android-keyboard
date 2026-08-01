package org.futo.inputmethod.latin

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OllamaClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "http://127.0.0.1:11434"

    // 1. Automatically fetch installed models from local Ollama instance
    private suspend fun fetchActiveModel(): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/tags")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val json = JSONObject(responseBody)
                    val modelsArray = json.optJSONArray("models")

                    if (modelsArray != null && modelsArray.length() > 0) {
                        // Dynamically returns the first available model found in Termux
                        return@withContext modelsArray.getJSONObject(0).optString("name", "qwen2.5:1.5b")
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback default if fetch fails
        }
        return@withContext "qwen2.5:1.5b"
    }

    // 2. Fix grammar using the dynamically selected model
    suspend fun fixGrammar(promptText: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val selectedModel = fetchActiveModel()

            val jsonPayload = JSONObject().apply {
                put("model", selectedModel)
                put("prompt", "Fix any grammar or spelling mistakes in the following text. Return ONLY the corrected text without explanations or quotes: $promptText")
                put("stream", false)
            }.toString()

            val body = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/generate")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP Error ${response.code}"))
                }

                val responseBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(responseBody)
                val correctedText = jsonResponse.optString("response", "").trim()

                if (correctedText.isNotEmpty()) {
                    Result.success(correctedText)
                } else {
                    Result.failure(Exception("Empty response from local AI"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
