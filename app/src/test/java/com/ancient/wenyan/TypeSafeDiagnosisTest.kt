package com.ancient.wenyan

import com.ancient.wenyan.domain.ai.TypeSafeDiagnosisEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TypeSafeDiagnosisTest {

    @Test
    fun testParseDiagnosisResponseSuccess() {
        val jsonPayload = """
        {
          "answers": {
            "intent_capture": { "noul": 0.88 },
            "error_type": { "choice": "phonetic_or_typo", "confidence": 0.92 },
            "mastery_score": { "score": 2.0 }
          }
        }
        """.trimIndent()

        val res = TypeSafeDiagnosisEngine.parseDiagnosisResponse(jsonPayload)
        assertTrue("Parser must succeed", res.isSuccess)
        assertEquals(88f, res.intentRate, 0.1f)
        assertEquals("phonetic_or_typo", res.errorCategory)
        assertEquals("字形通假 · 笔误微瑕", res.categoryDesc)
        assertEquals(3.0f, res.masteryScore, 0.1f)
        assertTrue(res.advice.contains("通假字形"))
    }

    @Test
    fun testMockTypeSafeDiagnosisEvaluation() = runBlocking {
        // Set mock dispatcher to prevent real network calls
        TypeSafeDiagnosisEngine.mockDispatcher = { prompt, expected, input, keyPoints ->
            if (input == "弩马十驾，功在不舍。") {
                com.ancient.wenyan.domain.ai.ScenarioDiagnosisResult(
                    isSuccess = true,
                    intentRate = 85.5f,
                    errorCategory = "phonetic_or_typo",
                    categoryDesc = "字形通假 · 笔误微瑕",
                    confidence = 0.9f,
                    masteryScore = 3.0f,
                    advice = "核心意象已掌握，特别注意通假字形与采分点易混错字。"
                )
            } else {
                com.ancient.wenyan.domain.ai.ScenarioDiagnosisResult(
                    isSuccess = true,
                    intentRate = 30.0f,
                    errorCategory = "context_mismatch",
                    categoryDesc = "审题偏差 · 意象混淆",
                    confidence = 0.8f,
                    masteryScore = 1.5f,
                    advice = "请重新仔细审读情境题眼，切勿将该篇其他写景句混淆代入。"
                )
            }
        }

        try {
            // Test 1: Typo case
            val res1 = TypeSafeDiagnosisEngine.diagnose(
                scenarioPrompt = "荀子在《劝学》中以劣马为喻，强调坚持不懈才能取得成功的名句是：",
                expectedAnswer = "驽马十驾，功在不舍。",
                userInput = "弩马十驾，功在不舍。",
                keyPoints = listOf("驽马", "功在不舍")
            )
            assertTrue("Mocked API call must succeed", res1.isSuccess)
            assertEquals("phonetic_or_typo", res1.errorCategory)
            assertTrue(res1.intentRate > 70f)

            // Test 2: Mismatch case
            val res2 = TypeSafeDiagnosisEngine.diagnose(
                scenarioPrompt = "韩愈在《师说》中指出从师的根本在于‘道’，破除门第观念的名句是：",
                expectedAnswer = "是故无贵无贱，无长无少，道之所存，师之所存也。",
                userInput = "古之学者必有师。",
                keyPoints = listOf("无贵无贱", "道之所存")
            )
            assertTrue("Mocked API call must succeed", res2.isSuccess)
            assertEquals("context_mismatch", res2.errorCategory)
        } finally {
            TypeSafeDiagnosisEngine.mockDispatcher = null
        }
    }

    @Test
    fun testBlankUserInputValidation() = runBlocking {
        val res = TypeSafeDiagnosisEngine.diagnose(
            scenarioPrompt = "Prompt",
            expectedAnswer = "Answer",
            userInput = "   ",
            keyPoints = emptyList()
        )
        assertFalse("Blank input must fail validation", res.isSuccess)
        assertEquals("作答内容为空", res.rawError)
    }

    @Test
    fun testOfflineLocalRuleBasedEvaluation() = runBlocking {
        TypeSafeDiagnosisEngine.mockDispatcher = null
        TypeSafeDiagnosisEngine.customApiKey = null

        val res = TypeSafeDiagnosisEngine.diagnose(
            scenarioPrompt = "荀子在《劝学》中以劣马为喻，强调坚持不懈才能取得成功的名句是：",
            expectedAnswer = "驽马十驾，功在不舍。",
            userInput = "驽马十驾，功在不舍。",
            keyPoints = listOf("驽马", "功在不舍")
        )
        assertTrue("Offline evaluation must succeed", res.isSuccess)
        assertEquals("accurate", res.errorCategory)
        assertEquals(4.0f, res.masteryScore, 0.1f)
    }
}
