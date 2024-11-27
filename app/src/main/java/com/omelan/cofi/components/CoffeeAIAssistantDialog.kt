package com.omelan.cofi.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.TextFieldValue
import android.util.Log
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import android.widget.TextView
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.text.Html
import android.text.Spanned
import androidx.compose.ui.graphics.toArgb
import androidx.core.text.HtmlCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

class WenXinAPI {
    companion object {
        private const val API_KEY = "kec2fq9yo27KWmTwYuoAurl2"
        private const val SECRET_KEY = "bFS62bfjurWCffOssqmLVIOiHrEvb3hW"
        
        private val HTTP_CLIENT = OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val original = chain.request()
                var response = chain.proceed(original)
                var tryCount = 0
                
                while (!response.isSuccessful && tryCount < 3) {
                    tryCount++
                    Log.d("CoffeeAI", "Retrying request, attempt $tryCount")
                    response.close()
                    response = chain.proceed(original)
                }
                
                response
            }
            .build()
    }

    private val dialogueContent = JSONArray()

    private suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        try {
            val mediaType = "application/x-www-form-urlencoded".toMediaType()
            val body = "grant_type=client_credentials&client_id=$API_KEY&client_secret=$SECRET_KEY"
                .toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://aip.baidubce.com/oauth/2.0/token")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()

            Log.d("CoffeeAI", "Requesting access token...")
            val response = HTTP_CLIENT.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("CoffeeAI", "Token response: $responseBody")

            if (!response.isSuccessful) {
                throw Exception("Failed to get access token: ${response.code}")
            }

            val jsonResponse = JSONObject(responseBody ?: "")
            if (!jsonResponse.has("access_token")) {
                Log.e("CoffeeAI", "Response does not contain access_token: $responseBody")
                throw Exception("Invalid response format")
            }

            jsonResponse.getString("access_token")
        } catch (e: Exception) {
            Log.e("CoffeeAI", "Error getting access token", e)
            throw e
        }
    }

    suspend fun getAnswer(userMsg: String): String = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken()
            Log.d("CoffeeAI", "Got access token: $accessToken")

            val userMessage = JSONObject().apply {
                put("role", "user")
                put("content", userMsg)
            }
            dialogueContent.put(userMessage)

            val mediaType = "application/json".toMediaType()
            val requestBody = JSONObject().apply {
                put("messages", dialogueContent)
                put("system", "你是一位专业的咖啡师助手，请用英文回答用户的问题。")
                put("disable_search", false)
                put("enable_citation", false)
            }

            val chatUrl = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions?access_token=$accessToken"
            Log.d("CoffeeAI", "Sending chat request to: $chatUrl")
            Log.d("CoffeeAI", "Request body: ${requestBody}")

            val request = Request.Builder()
                .url(chatUrl)
                .post(requestBody.toString().toRequestBody(mediaType))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = HTTP_CLIENT.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("CoffeeAI", "Chat response: $responseBody")

            if (!response.isSuccessful) {
                throw Exception("Chat request failed: ${response.code}")
            }

            val jsonResponse = JSONObject(responseBody ?: "")
            val result = jsonResponse.getString("result")

            val assistantMessage = JSONObject().apply {
                put("role", "assistant")
                put("content", result)
            }
            dialogueContent.put(assistantMessage)

            result
        } catch (e: Exception) {
            Log.e("CoffeeAI", "Error in getAnswer", e)
            throw Exception("The network request timed out, please try again later.")  // 用户友好的错误信息
        }
    }
}

@Composable
fun CoffeeAIAssistantDialog(
    onDismiss: () -> Unit,
    apiKey: String
) {
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var inputText by remember { mutableStateOf(TextFieldValue()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val wenXinAPI = remember { WenXinAPI() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your BrewPal") },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(messages) { message ->
                        MessageBubble(message)
                    }
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your question about coffee brewing...") },
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (inputText.text.isNotBlank()) {
                        scope.launch {
                            isLoading = true
                            try {
                                val userMessage = inputText.text
                                messages = messages + Message(userMessage, true)
                                inputText = TextFieldValue("")
                                
                                val response = wenXinAPI.getAnswer(userMessage)
                                messages = messages + Message(response, false)
                            } catch (e: Exception) {
                                Log.e("CoffeeAI", "Error sending message", e)
                                messages = messages + Message("Error: ${e.localizedMessage}", false)
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading && inputText.text.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Send")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

data class Message(
    val content: String,
    val isUser: Boolean
)

@Composable
private fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(4.dp)
        ) {
            if (message.isUser) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(8.dp),
                    color = Color.White
                )
            } else {
                MarkdownText(
                    markdown = message.content,
                    modifier = Modifier.padding(8.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    onLinkClick = { url -> /* 处理链接点击 */ }
                )
            }
        }
    }
}

@Composable
private fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    onLinkClick: ((String) -> Unit)? = null
) {
    val markdownParser = remember { Parser.builder().build() }
    val document = remember(markdown) { markdownParser.parse(markdown) }
    val visitor = remember { HtmlRenderer.builder().build() }
    val html = remember(document) { visitor.render(document) }
    
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setTextColor(color.toArgb())
                textSize = style.fontSize.value
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            onLinkClick?.let { callback ->
                textView.setOnClickListener { view ->
                    if (view is TextView) {
                        val text = view.text
                        if (text is Spanned) {
                            val spans = text.getSpans(0, text.length, URLSpan::class.java)
                            spans.forEach { span ->
                                callback(span.url)
                            }
                        }
                    }
                }
            }
        }
    )
} 
