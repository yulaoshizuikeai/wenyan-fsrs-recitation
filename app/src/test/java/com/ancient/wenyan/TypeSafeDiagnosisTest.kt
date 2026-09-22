package com.ancient.wenyan

import com.ancient.wenyan.domain.ai.TypeSafeDiagnosisEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TypeSafeDiagnosisTest {

    @Test
    fun testLiveTypeSafeDiagnosisEvaluation() = runBlocking {
        println("Testing TypeSafe Diagnosis Engine with live Jev model...")

        // Test 1: Near-perfect recitation with small typo
        val res1 = TypeSafeDiagnosisEngine.diagnose(
            scenarioPrompt = "荀子在《劝学》中以劣马为喻，强调坚持不懈才能取得成功的名句是：",
            expectedAnswer = "驽马十驾，功在不舍。",
            userInput = "弩马十驾，功在不舍。", // 写错“驽”为“弩”
            keyPoints = listOf("驽马", "功在不舍")
        )

        println("Result 1: isSuccess=${res1.isSuccess}, errorCategory=${res1.errorCategory}, intentRate=${res1.intentRate}%, score=${res1.masteryScore}")
        assertTrue("API call must succeed", res1.isSuccess)
        assertTrue("Intent rate should be high", res1.intentRate > 70f)
        assertNotNull(res1.advice)

        // Test 2: Incomplete or confused answer
        val res2 = TypeSafeDiagnosisEngine.diagnose(
            scenarioPrompt = "韩愈在《师说》中指出从师的根本在于‘道’，破除门第观念的名句是：",
            expectedAnswer = "是故无贵无贱，无长无少，道之所存，师之所存也。",
            userInput = "古之学者必有师。", // 答成第一句中心论点，发生审题偏差
            keyPoints = listOf("无贵无贱", "道之所存")
        )

        println("Result 2: isSuccess=${res2.isSuccess}, errorCategory=${res2.errorCategory}, intentRate=${res2.intentRate}%, score=${res2.masteryScore}")
        assertTrue("API call must succeed", res2.isSuccess)
    }
}
