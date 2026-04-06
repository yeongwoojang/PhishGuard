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
    private val TAG = "PhishingDetector"
    private var interpreter: Interpreter? = null
    private val vocabulary = mutableMapOf<String, Int>()
    private var maxLen = 30  //_ Colab에서 모델 학습 시 max_length 값을 30으로 학습했기 때문에 기본값을 30으로 설정,

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
            Log.d(TAG, "TFLite 모델 로드 성공")
        } catch (e: Exception) {
            Log.e(TAG, "TFLite 모델 로드 실패: ${e.message}")
        }
    }


    /**
     * assets/vocab.json 파일에서 단어 목록과 최대 길이를 로드
     */
    private fun loadVocabulary() {
        try {
            val json = context.assets.open("vocab.json")
                .bufferedReader()
                .readText()

            //_ vocab.json 파일의 단어들은 "word_index"와 "max_length" 사이에 위치하기 때문에 그 사이의 문자들을 추출한다.
            val wordIndexStart = json.indexOf("\"word_index\":") + "\"word_index\":".length
            Log.d(TAG, "wordIndexStart: $wordIndexStart")
            val wordIndexEnd = json.lastIndexOf("},\"max_length\"")
                .takeIf { it > 0 } ?: json.lastIndexOf("}")
            Log.d(TAG, "wordIndexStart: $wordIndexEnd")

            val wordIndexJson = json.substring(wordIndexStart, wordIndexEnd).trim()
            Log.d(TAG, "wordIndexJson: $wordIndexJson")

            //_ 이 후 각단어와 번호를 저장한다.
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
            Log.d(TAG, "vocabulary: $vocabulary")
            val maxLengthMatch = Regex("\"max_length\":\\s*(\\d+)").find(json)
            maxLen = maxLengthMatch?.groupValues?.get(1)?.toIntOrNull() ?: 30 //_ 30으로 학습했기 떄문에 30이 반환될 것임.

            Log.d(TAG, "vocab 로드 성공: ${vocabulary.size}개 단어, maxLen: $maxLen")
        } catch (e: Exception) {
            Log.e(TAG, "vocab 로드 실패: ${e.message}")
        }
    }

    /**
     * 메세지의 피싱 여부를 TFLite 모델로 분석
     *
     * 30단어 이하 → 단일 추론
     * 30단어 초과 → 청크로 나눠 추론 후 최댓값 반환
     *
     * @param text 분석할 문자 텍스트
     * @return 피싱 확률 (0.0 ~ 1.0), 분석 실패 시 -1f 반환
     */
    fun analyze(text: String): Float {
        return try {
            val tokens = text.lowercase().split(" ")

            //_ 30단어 이하면 기존 방식 그대로
            if (tokens.size <= maxLen) {
                val input = tokensToInputArray(tokens)
                return runInference(input)
            }

            //_ 30단어 초과면 청크로 나눠서 분석
            val chunks = tokens.chunked(maxLen)
            Log.d(TAG, "긴 문자 감지 → ${chunks.size}개 청크로 분석")

            val scores = chunks.map { chunk ->
                val input = tokensToInputArray(chunk)
                runInference(input)
            }

            //_ 메세지 중 일부라도 위험하다면 위험 문자이기 때문에 청크 중 가장 높은 점수 반환
            val maxScore = scores.max()
            Log.d(TAG, "청크별 점수: $scores → 최종: $maxScore")
            maxScore

        } catch (e: Exception) {
            Log.e(TAG, "TFLite 추론 실패: ${e.message}")
            -1f
        }
    }

    /**
     * 토큰 리스트를 TFLite 모델 입력 형식으로 변환
     * 모델은 항상 고정된 크기(maxLen)의 Float 배열을 입력으로 받음
     *
     * [배치크기, maxLen] 표현은
     * 딥러닝에서 배열 크기를 [행, 열] 로 나타내기 때문.
     *
     * 배치크기 = 행 (한 번에 몇 개 처리)
     * maxLen = 열 (단어 몇 개)
     *
     * 현재 코드에서는 한 번에 1개씩 처리하니까
     * 항상 [1, 30(maxLen)] 이다.
     *
     * @param tokens 변환할 단어 토큰 리스트
     * @return Array<FloatArray> — shape: [1, maxLen] | vocab.json 파일에 없는 단어는 0f으로 처리
     *
     */
    private fun tokensToInputArray(tokens: List<String>): Array<FloatArray> {
        //_ 모델 입력 크기(maxLen)만큼 0f으로 초기화 — 부족한 부분은 패딩(0)으로 채움
        val inputArray = FloatArray(maxLen) { 0f }

        //_ 토큰을 vocab 인덱스로 변환하여 배열에 삽입
        //_ maxLen 초과 토큰은 버림
        //_ vocab.json 파일에 없는 단어는 0으로 처리
        tokens.take(maxLen).forEachIndexed { index, token ->
            inputArray[index] = (vocabulary[token] ?: 0).toFloat()
        }

        return arrayOf(inputArray)
    }

    /**
     * TFLite 모델 분석 실행
     *
     * @param input  모델 입력 배열 — shape: [1, maxLen]
     *               ex) [[5f, 8f, 2f, 3f, 0f, ... 0f]]
     * @return 피싱 확률 (0.0 ~ 1.0)
     *         ex) input이 피싱 문자면 0.997f 반환
     */
    private fun runInference(input: Array<FloatArray>): Float {
        //_ 모델 출력을 담을 2차원 배열 초기화 — shape: [1, 2]
        //_ [배치 1개, [정상확률, 피싱확률]]
        //_ ex) [[0.003f, 0.997f]]
        val output = Array(1) { FloatArray(2) }

        //_ input을 모델에 넣어 output 채움
        //_ interpreter가 null이면 실행 안 됨 -> output은 초기값 [0f, 0f] 유지
        interpreter?.run(input, output)

        //_ output[0]: 첫 번째(유일한) 배치 결과 [정상확률, 피싱확률]
        //_ output[0][1]: 피싱 확률만 추출해서 반환
        return output[0][1]
    }
}