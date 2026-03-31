package com.example.phishguard.ml

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhishingDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interpreter: Interpreter? = null
    private val vocabulary = mutableMapOf<String, Int>()
    private val maxLength = 20

    init {
        loadModel()
        loadVocabulary()
    }

    private fun loadModel() {
        try {
            val inputStream = context.assets.open("phishing_model.tflite")
            val bytes = inputStream.readBytes()
            val buffer = ByteBuffer.allocateDirect(bytes.size)
            buffer.put(bytes)
            buffer.rewind()
            interpreter = Interpreter(buffer)
            Log.d("PhishGuard", "TFLite 모델 로드 성공")
        } catch (e: Exception) {
            Log.e("PhishGuard", "TFLite 모델 로드 실패: ${e.message}")
        }
    }

    private var maxLen = 30  // 기본값

    private fun loadVocabulary() {
        try {
            val json = context.assets.open("vocab.json")
                .bufferedReader()
                .readText()

            // word_index 추출
            val wordIndexStart = json.indexOf("\"word_index\":") + "\"word_index\":".length
            val wordIndexEnd = json.lastIndexOf("},\"max_length\"")
                .takeIf { it > 0 } ?: json.lastIndexOf("}")

            val wordIndexJson = json.substring(wordIndexStart, wordIndexEnd).trim()

            wordIndexJson.trimStart('{').trimEnd('}')
                .split(",")
                .forEach { entry ->
                    val parts = entry.trim().split(":")
                    if (parts.size == 2) {
                        val word = parts[0].trim().trim('"')
                        val index = parts[1].trim().toIntOrNull() ?: 0
                        vocabulary[word] = index
                    }
                }

            // max_length 추출
            val maxLengthMatch = Regex("\"max_length\":\\s*(\\d+)").find(json)
            maxLen = maxLengthMatch?.groupValues?.get(1)?.toIntOrNull() ?: 30

            Log.d("PhishGuard", "vocab 로드 성공: ${vocabulary.size}개 단어, maxLen: $maxLen")
        } catch (e: Exception) {
            Log.e("PhishGuard", "vocab 로드 실패: ${e.message}")
        }
    }

    fun analyze(text: String): Float {
        return try {
            val input = textToInputArray(text)
            val output = Array(1) { FloatArray(2) }
            interpreter?.run(input, output)

            // output[0][1] = 스팸(피싱)일 확률
            val score = output[0][1]
            Log.d("PhishGuard", "TFLite 판별 점수: $score")
            score
        } catch (e: Exception) {
            Log.e("PhishGuard", "TFLite 추론 실패: ${e.message}")
            -1f
        }
    }

    private fun textToInputArray(text: String): Array<FloatArray> {
        val tokens = text.lowercase().split(" ")
        val inputArray = FloatArray(maxLen) { 0f }  // maxLength → maxLen

        tokens.take(maxLen).forEachIndexed { index, token ->
            inputArray[index] = (vocabulary[token] ?: 0).toFloat()
        }

        return arrayOf(inputArray)
    }

    fun close() {
        interpreter?.close()
    }
}