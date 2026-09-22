package com.ancient.wenyan.domain.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ScenarioDiagnosisResult(
    val isSuccess: Boolean,
    val intentRate: Float = 0f,         // 0..100% 意象捕捉度 (Noul)
    val errorCategory: String = "",     // 错误归因 (Choice: accurate, phonetic_or_typo, context_mismatch, incomplete_clause)
    val categoryDesc: String = "",      // 中文解释标签
    val confidence: Float = 0f,         // 置信度
    val masteryScore: Float = 0f,       // 掌握度得分 1.0 ~ 4.0 (Score)
    val advice: String = "",            // 靶向名师点拨
    val rawError: String? = null
)

/**
 * TypeSafe System One (Jev) AI Evaluation Engine for GaoKao Scenarios.
 * Transforms student recitation attempts into calibrated semantic judgments.
 */
object TypeSafeDiagnosisEngine {

    private const val API_ENDPOINT = "https://api.typesafe.ai/v1/systemone"
    
    // Developer provided runtime API key
    private const val DEFAULT_API_KEY = "apikey_2254aa16e56061fe4f9c868636572350faf5_c76c129f715a90ed4d03581f969094d5670dc723133a40701257ecf9a8844e64"

    suspend fun diagnose(
        scenarioPrompt: String,
        expectedAnswer: String,
        userInput: String,
        keyPoints: List<String>,
        apiKey: String = DEFAULT_API_KEY
    ): ScenarioDiagnosisResult = withContext(Dispatchers.IO) {
        if (userInput.isBlank()) {
            return@withContext ScenarioDiagnosisResult(
                isSuccess = false,
                rawError = "作答内容为空"
            )
        }

        try {
            val url = URL(API_ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                connectTimeout = 12000
                readTimeout = 15000
                doOutput = true
                doInput = true
            }

            // Build payload following TypeSafe System One specs
            val stateJson = JSONObject().apply {
                put("scenario_prompt", scenarioPrompt)
                put("expected_answer", expectedAnswer)
                put("user_input", userInput)
                put("key_points", JSONArray(keyPoints))
            }

            val questionsJson = JSONObject().apply {
                // 1. Noul: Intent & core imagery capture
                put("intent_capture", JSONObject().apply {
                    put("type", "noul")
                    put("instructions", "Does the `user_input` capture the core imagery or poetic scene required by `scenario_prompt`?")
                })

                // 2. Choice: Error Root Cause Attribution
                put("error_type", JSONObject().apply {
                    put("type", "choice")
                    put("instructions", "Diagnose the student's recitation problem compared to `expected_answer`:")
                    put("criteria", JSONObject().apply {
                        put("accurate", "Recitation is substantially accurate with no major errors")
                        put("phonetic_or_typo", "Has phonetic loan, typo, or homophone character confusion")
                        put("context_mismatch", "Misunderstood the scenario prompt, recited a wrong verse or another poem")
                        put("incomplete_clause", "Missing a critical half-clause or sentence is incomplete")
                    })
                })

                // 3. Score: Mastery rubric
                put("mastery_score", JSONObject().apply {
                    put("type", "score")
                    put("instructions", "Rate the student's mastery level:")
                    put("criteria", JSONArray().apply {
                        put("Severe errors or completely off-topic")
                        put("Partial recall with noticeable mistakes")
                        put("Good recitation with only minor typos or particles omitted")
                        put("Flawless recall perfectly matching the scenario")
                    })
                })
            }

            val requestBody = JSONObject().apply {
                put("model", "jev-latest")
                put("state", stateJson)
                put("questions", questionsJson)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorStream = conn.errorStream ?: conn.inputStream
                val errorMsg = BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                return@withContext ScenarioDiagnosisResult(
                    isSuccess = false,
                    rawError = "HTTP $responseCode: $errorMsg"
                )
            }

            val responseText = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
            val respJson = JSONObject(responseText)
            val answers = respJson.getJSONObject("answers")

            // Parse Noul
            val intentNoul = answers.getJSONObject("intent_capture").optDouble("noul", 0.0).toFloat() * 100f

            // Parse Choice
            val choiceObj = answers.getJSONObject("error_type")
            val errorChoice = choiceObj.optString("choice", "accurate")
            val confidence = choiceObj.optDouble("confidence", 0.8).toFloat()

            // Parse Score
            val scoreObj = answers.getJSONObject("mastery_score")
            val scoreVal = scoreObj.optDouble("score", 2.0).toFloat() + 1.0f // map 0..3 to 1..4 scale

            val (desc, advice) = when (errorChoice) {
                "accurate" -> "精准无误 · 形神兼备" to "背诵高度切题，完美契合高考设问意境！"
                "phonetic_or_typo" -> "字形通假 · 笔误微瑕" to "核心意象已掌握，特别注意通假字形与采分点易混错字。"
                "context_mismatch" -> "审题偏差 · 意象混淆" to "请重新仔细审读情境题眼，切勿将该篇其他写景句混淆代入。"
                "incomplete_clause" -> "句式残缺 · 断篇遗漏" to "注意上下联句式的完整连贯，避免出现漏字或落句。"
                else -> "学情解析就绪" to "结合标准答案与核心采分点加强记忆复习。"
            }

            ScenarioDiagnosisResult(
                isSuccess = true,
                intentRate = intentNoul,
                errorCategory = errorChoice,
                categoryDesc = desc,
                confidence = confidence,
                masteryScore = scoreVal,
                advice = advice
            )
        } catch (e: Exception) {
            ScenarioDiagnosisResult(
                isSuccess = false,
                rawError = e.localizedMessage ?: e.javaClass.simpleName
            )
        }
    }
}
