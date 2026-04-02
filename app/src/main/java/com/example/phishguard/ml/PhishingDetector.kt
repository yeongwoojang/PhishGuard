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
    private var maxLen = 30  // 기본값

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

    private fun loadVocabulary() {
        try {
            val json = context.assets.open("vocab.json")
                .bufferedReader()
                .readText()

            // word_index 추출
            val wordIndexStart = json.indexOf("\"word_index\":") + "\"word_index\":".length
            Log.d("PhishGuard", "wordIndexStart: $wordIndexStart")
            val wordIndexEnd = json.lastIndexOf("},\"max_length\"")
                .takeIf { it > 0 } ?: json.lastIndexOf("}")
            Log.d("PhishGuard", "wordIndexStart: $wordIndexEnd")

            val wordIndexJson = json.substring(wordIndexStart, wordIndexEnd).trim()
            Log.d("PhishGuard", "wordIndexJson: $wordIndexJson")

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
            Log.d("PhishGuard", "vocabulary: $vocabulary")
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
            val tokens = text.lowercase().split(" ")

            // 30단어 이하면 기존 방식 그대로
            if (tokens.size <= maxLen) {
                val input = tokensToInputArray(tokens)
                return runInference(input)
            }

            // 30단어 초과면 청크로 나눠서 분석
            val chunks = tokens.chunked(maxLen)
            Log.d("PhishGuard", "긴 문자 감지 → ${chunks.size}개 청크로 분석")

            val scores = chunks.map { chunk ->
                val input = tokensToInputArray(chunk)
                runInference(input)
            }

            // 가장 높은 점수 반환 (어느 부분이든 피싱이면 위험)
            val maxScore = scores.max()
            Log.d("PhishGuard", "청크별 점수: $scores → 최종: $maxScore")
            maxScore

        } catch (e: Exception) {
            Log.e("PhishGuard", "TFLite 추론 실패: ${e.message}")
            -1f
        }
    }

    // 토큰 배열 → 모델 입력 배열 변환
    private fun tokensToInputArray(tokens: List<String>): Array<FloatArray> {
        val inputArray = FloatArray(maxLen) { 0f }

        tokens.take(maxLen).forEachIndexed { index, token ->
            inputArray[index] = (vocabulary[token] ?: 0).toFloat()
        }

        return arrayOf(inputArray)
    }

    // 실제 모델 추론
    private fun runInference(input: Array<FloatArray>): Float {
        val output = Array(1) { FloatArray(2) }
        interpreter?.run(input, output)
        return output[0][1]
    }
}