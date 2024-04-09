package com.markvisor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _result = MutableStateFlow<Resource<String>?>(null)
    val result: StateFlow<Resource<String>?> = _result.asStateFlow()
    private var inFlightJob: Job? = null
    private var cachedImage: Image? = null

    private val generativeMultiModel = GenerativeModel(
        modelName = "gemini-pro-vision",
        apiKey = BuildConfig.API_KEY,
    )

    fun getResultFromModule(context: Context, imageUri: Uri, tone: String, language: String) {
        inFlightJob?.cancel()
        inFlightJob = viewModelScope.launch(Dispatchers.IO) {
            val prompt = """
                Create an engaging product advertisement, up to 80 words, based on the given image. Use the user provided tone style (wrapped by triple quote ""${'"'}) 
                ""${'"'}$tone""${'"'} 
                to effectively highlight the product's key qualities. Additionally, suggest five relevant tags with format (#CONTENT) that best describe the featured item. Provide your response in locale "$language"
            """.trimIndent()

            val inputContent = content {
                createBitmapFromUri(context, imageUri)?.let { bitmap ->
                    image(bitmap)
                    cachedImage = Image(uri = imageUri, bitmap = bitmap)
                }
                text(prompt)
            }

            _result.value = Resource.Loading()
            var fullResponse = ""
            try {
                generativeMultiModel.generateContentStream(inputContent).collect { chunk ->
                    ensureActive()
                    fullResponse += chunk.text
                    _result.value = Resource.Success(fullResponse)
                }
            } catch (e: Exception) {
                _result.value = Resource.Error(message = e.message ?: "Error !")
            }
        }
    }

    fun recycleBitmap() {
        cachedImage?.bitmap?.apply {
            if (!isRecycled) {
                recycle()
                cachedImage = null
            }
        }
    }

    private fun createBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        if (cachedImage?.uri == uri) {
            return cachedImage?.bitmap
        }
        val inputStream = context.contentResolver.openInputStream(uri)
        return inputStream?.use {
            var bitmap: Bitmap? = null
            try {
                bitmap = BitmapFactory.decodeStream(it)
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            }
            return bitmap
        }
    }
}

sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T> : Resource<T>()
}

data class Image(val uri: Uri, val bitmap: Bitmap)
