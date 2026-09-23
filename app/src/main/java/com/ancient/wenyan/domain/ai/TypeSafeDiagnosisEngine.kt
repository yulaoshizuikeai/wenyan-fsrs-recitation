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
    
    // API Key resolution (customApiKey -> System env TYPESAFE_API_KEY -> blank)
    var customApiKey: String? = null

    /**
     * Mock dispatcher for hermetic unit testing and offline diagnostics.
     */
    var mockDispatcher: ((scenarioPrompt: String, expectedAnswer: String, userInput: String, keyPoints: List<String>) -> ScenarioDiagnosisResult)? = null

    fun getEffectiveApiKey(): String {
        return customApiKey ?: System.getenv("TYPESAFE_API_KEY") ?: ""
    }

    suspend fun diagnose(
        scenarioPrompt: String,
        expectedAnswer: String,
        userInput: String,
        keyPoints: List<String>,
        apiKey: String = getEffectiveApiKey()
    ): ScenarioDiagnosisResult = withContext(Dispatchers.IO) {
        if (userInput.isBlank()) {
            return@withContext ScenarioDiagnosisResult(
                isSuccess = false,
                rawError = "作答内容为空"
            )
        }

        mockDispatcher?.let { dispatcher ->
            return@withContext dispatcher(scenarioPrompt, expectedAnswer, userInput, keyPoints)
        }

        val effectiveKey = if (apiKey.isNotBlank()) apiKey else getEffectiveApiKey()
        if (effectiveKey.isBlank()) {
            return@withContext evaluateLocalRuleBased(scenarioPrompt, expectedAnswer, userInput, keyPoints)
        }

        try {
            val url = URL(API_ENDPOINT)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $effectiveKey")
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
            parseDiagnosisResponse(responseText)
        } catch (e: Exception) {
            ScenarioDiagnosisResult(
                isSuccess = false,
                rawError = e.localizedMessage ?: e.javaClass.simpleName
            )
        }
    }

    /**
     * Parses TypeSafe System One JSON response into calibrated ScenarioDiagnosisResult.
     */
    fun parseDiagnosisResponse(responseText: String): ScenarioDiagnosisResult {
        return try {
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
                rawError = "解析诊断响应失败: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    /**
     * Offline rule-based fallback when no external API key is configured.
     */
    fun evaluateLocalRuleBased(
        scenarioPrompt: String,
        expectedAnswer: String,
        userInput: String,
        keyPoints: List<String>
    ): ScenarioDiagnosisResult {
        val cleanInput = userInput.replace(Regex("[，。？！、：；“”‘’\\s]"), "")
        val cleanExpected = expectedAnswer.replace(Regex("[，。？！、：；“”‘’\\s]"), "")

        if (cleanInput.isEmpty()) {
            return ScenarioDiagnosisResult(isSuccess = false, rawError = "作答内容为空")
        }

        val matchedKeyPoints = keyPoints.count { kp ->
            cleanInput.contains(kp.replace(Regex("[，。？！、：；“”‘’\\s]"), ""))
        }
        val keyPointCoverage = if (keyPoints.isNotEmpty()) matchedKeyPoints.toFloat() / keyPoints.size else 1f

        var commonChars = 0
        val expCharCounts = mutableMapOf<Char, Int>()
        for (c in cleanExpected) expCharCounts[c] = (expCharCounts[c] ?: 0) + 1
        for (c in cleanInput) {
            val count = expCharCounts[c] ?: 0
            if (count > 0) {
                commonChars++
                expCharCounts[c] = count - 1
            }
        }
        val charAccuracy = if (cleanExpected.isNotEmpty()) commonChars.toFloat() / cleanExpected.length else 0f

        val (category, desc, advice, score) = when {
            cleanInput == cleanExpected -> {
                Quadruple(
                    "accurate",
                    "精准无误 · 形神兼备",
                    "背诵高度切题，完美契合高考设问意境！",
                    4.0f
                )
            }
            charAccuracy >= 0.8f || (charAccuracy >= 0.6f && keyPointCoverage >= 0.5f) -> {
                Quadruple(
                    "phonetic_or_typo",
                    "字形通假 · 笔误微瑕",
                    "核心意象已掌握，特别注意通假字形与采分点易混错字。",
                    3.0f
                )
            }
            cleanInput.length < cleanExpected.length / 2 -> {
                Quadruple(
                    "incomplete_clause",
                    "句式残缺 · 断篇遗漏",
                    "注意上下联句式的完整连贯，避免出现漏字或落句。",
                    2.0f
                )
            }
            else -> {
                Quadruple(
                    "context_mismatch",
                    "审题偏差 · 意象混淆",
                    "请重新仔细审读情境题眼，切勿将该篇其他写景句混淆代入。",
                    1.5f
                )
            }
        }

        return ScenarioDiagnosisResult(
            isSuccess = true,
            intentRate = (charAccuracy * 0.5f + keyPointCoverage * 0.5f) * 100f,
            errorCategory = category,
            categoryDesc = desc,
            confidence = 0.85f,
            masteryScore = score,
            advice = advice
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
