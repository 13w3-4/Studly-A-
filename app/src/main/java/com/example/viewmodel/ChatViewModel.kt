package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.gemini.Content
import com.example.gemini.GenerateContentRequest
import com.example.gemini.InlineData
import com.example.gemini.Part
import com.example.gemini.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isFromUser: Boolean,
    val image: Bitmap? = null,
    val isLoading: Boolean = false,
    val canGenerateVideo: Boolean = false
)

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val history = mutableListOf<Content>()

    init {
        _messages.update {
            listOf(
                Message(
                    text = "Merhaba! Ben David. Bana takıldığın soruları gönderebilir, notlarının veya PDF okumalarının fotoğrafını atabilirsin. Sana adım adım anlatıp, çalışma özetleri çıkarabilirim!",
                    isFromUser = false
                )
            )
        }
    }

    private val systemInstruction = Content(
        role = "user",
        parts = listOf(
            Part(text = "Sen David adında bir yapay zeka ders çalışma arkadaşısın. Kullanıcılara sorularında adım adım rehberlik edersin. Doğrudan cevap vermek yerine ipuçları vererek onların öğrenmesini sağlarsın. Nazik, motive edici ve akıcı bir Türkçe kullanıyorsun. Ayrıca kullanıcı çalışma notu gönderirse onları düzenleyip 'Canlı Çalışma Notları' haline getirebilirsin.")
        )
    )

    fun sendMessage(text: String, bitmap: Bitmap?) {
        if (text.isBlank() && bitmap == null) return
        
        val userMessage = Message(text = text, isFromUser = true, image = bitmap)
        _messages.update { it + userMessage }
        _messages.update { it + Message(text = "", isFromUser = false, isLoading = true) }

        val parts = mutableListOf<Part>()
        if (text.isNotBlank()) {
            parts.add(Part(text = text))
        }
        if (bitmap != null) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64)))
        }

        history.add(Content(role = "user", parts = parts))

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _messages.update { list ->
                         list.dropLast(1) + Message(text = "Lütfen API anahtarını Settings panelinden ekleyin.", isFromUser = false)
                    }
                    return@launch
                }

                val request = GenerateContentRequest(
                    contents = history,
                    systemInstruction = systemInstruction
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Cevap anlaşılamadı. Lütfen tekrar dener misin?"

                history.add(Content(role = "model", parts = listOf(Part(text = replyText))))

                val canGenerateVideo = replyText.contains("adım") || replyText.contains("çözüm")

                _messages.update { list ->
                    list.dropLast(1) + Message(text = replyText, isFromUser = false, canGenerateVideo = canGenerateVideo)
                }
            } catch (e: Exception) {
                 _messages.update { list ->
                     list.dropLast(1) + Message(text = "Bir hata oluştu: ${e.message}", isFromUser = false)
                 }
            }
        }
    }
}
