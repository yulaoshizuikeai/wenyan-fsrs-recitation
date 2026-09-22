package com.ancient.wenyan.domain.sync

import com.ancient.wenyan.data.WenYanRepository
import com.ancient.wenyan.domain.fsrs.CardFsrsState
import com.ancient.wenyan.domain.fsrs.ReviewLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class WebDavConfig(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = ""
)

data class SyncResult(
    val success: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Pure Kotlin on-device WebDAV and Local JSON Backup/Restore manager.
 * Operates without third-party network libraries using standard HttpURLConnection.
 */
object WebDavBackupManager {

    /**
     * Serializes all learning progress, card FSRS states, and review logs into a JSON backup payload.
     */
    fun createBackupJson(repository: WenYanRepository): String {
        val sb = java.lang.StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": 1,\n")
        sb.append("  \"timestamp\": ").append(System.currentTimeMillis()).append(",\n")

        val stats = repository.statsFlow.value
        sb.append("  \"totalCards\": ").append(stats.totalCards).append(",\n")
        sb.append("  \"retentionPercentage\": ").append(stats.retentionPercentage).append(",\n")

        // Cards array
        val allStates = repository.getAllCardStates()
        val activeCards = allStates.filter {
            it.state != com.ancient.wenyan.domain.fsrs.CardState.NEW || it.reps > 0 || it.lapses > 0
        }
        sb.append("  \"cards\": [\n")
        activeCards.forEachIndexed { index, card ->
            sb.append("    {")
            sb.append("\"cardId\": \"").append(escapeJson(card.cardId)).append("\", ")
            sb.append("\"state\": \"").append(card.state.name).append("\", ")
            sb.append("\"step\": ").append(card.step ?: -1).append(", ")
            sb.append("\"stability\": ").append(card.stability).append(", ")
            sb.append("\"difficulty\": ").append(card.difficulty).append(", ")
            sb.append("\"elapsedDays\": ").append(card.elapsedDays).append(", ")
            sb.append("\"scheduledDays\": ").append(card.scheduledDays).append(", ")
            sb.append("\"reps\": ").append(card.reps).append(", ")
            sb.append("\"lapses\": ").append(card.lapses).append(", ")
            sb.append("\"lastReviewTime\": ").append(card.lastReviewTime ?: -1L).append(", ")
            sb.append("\"dueTime\": ").append(card.dueTime).append(", ")
            sb.append("\"isLeech\": ").append(card.isLeech)
            sb.append("}")
            if (index < activeCards.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ],\n")

        // Logs array (full uncapped history)
        val logs = repository.getReviewLogs()
        sb.append("  \"reviewLogs\": [\n")
        logs.forEachIndexed { index, log ->
            sb.append("    {")
            sb.append("\"cardId\": \"").append(escapeJson(log.cardId)).append("\", ")
            sb.append("\"rating\": \"").append(log.rating.name).append("\", ")
            sb.append("\"previousState\": \"").append(log.previousState.name).append("\", ")
            sb.append("\"currentState\": \"").append(log.currentState.name).append("\", ")
            sb.append("\"stability\": ").append(log.stability).append(", ")
            sb.append("\"difficulty\": ").append(log.difficulty).append(", ")
            sb.append("\"elapsedDays\": ").append(log.elapsedDays).append(", ")
            sb.append("\"scheduledDays\": ").append(log.scheduledDays).append(", ")
            sb.append("\"reviewTime\": ").append(log.reviewTime)
            sb.append("}")
            if (index < logs.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}")

        return sb.toString()
    }

    /**
     * Restores learning state from JSON string.
     */
    fun restoreFromJson(jsonStr: String, repository: WenYanRepository): Int {
        val cardsContent = extractArrayContent(jsonStr, "cards") ?: return 0
        val cardBlocks = splitJsonObjects(cardsContent)
        var restoredCount = 0

        for (block in cardBlocks) {
            val cardId = extractString(block, "cardId") ?: continue
            val stateName = extractString(block, "state") ?: "NEW"
            val stepVal = extractInt(block, "step") ?: -1
            val stability = extractDouble(block, "stability") ?: 0.0
            val difficulty = extractDouble(block, "difficulty") ?: 0.0
            val elapsed = extractInt(block, "elapsedDays") ?: 0
            val scheduled = extractInt(block, "scheduledDays") ?: 0
            val reps = extractInt(block, "reps") ?: 0
            val lapses = extractInt(block, "lapses") ?: 0
            val lastTimeVal = extractLong(block, "lastReviewTime") ?: -1L
            val dueTime = extractLong(block, "dueTime") ?: 0L
            val isLeech = extractBoolean(block, "isLeech") ?: (lapses >= 4)

            val state = CardFsrsState(
                cardId = cardId,
                state = try { com.ancient.wenyan.domain.fsrs.CardState.valueOf(stateName) } catch (_: Exception) { com.ancient.wenyan.domain.fsrs.CardState.NEW },
                step = if (stepVal >= 0) stepVal else null,
                stability = stability,
                difficulty = difficulty,
                elapsedDays = elapsed,
                scheduledDays = scheduled,
                reps = reps,
                lapses = lapses,
                lastReviewTime = if (lastTimeVal > 0) lastTimeVal else null,
                dueTime = dueTime,
                isLeech = isLeech
            )
            repository.setCardStateForTesting(state)
            restoredCount++
        }
        return restoredCount
    }

    private fun extractArrayContent(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\\[([\\s\\S]*?)\\]\\s*(?:,|\\})")
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun splitJsonObjects(arrayContent: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var start = -1
        for (i in arrayContent.indices) {
            val c = arrayContent[i]
            if (c == '{') {
                if (depth == 0) start = i
                depth++
            } else if (c == '}') {
                depth--
                if (depth == 0 && start != -1) {
                    result.add(arrayContent.substring(start, i + 1))
                    start = -1
                }
            }
        }
        return result
    }

    private fun extractString(json: String, key: String): String? {
        val match = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(json)
        return match?.groupValues?.get(1)
    }

    private fun extractInt(json: String, key: String): Int? {
        val match = Regex("\"$key\"\\s*:\\s*(-?\\d+)").find(json)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractLong(json: String, key: String): Long? {
        val match = Regex("\"$key\"\\s*:\\s*(-?\\d+)").find(json)
        return match?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun extractDouble(json: String, key: String): Double? {
        val match = Regex("\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").find(json)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractBoolean(json: String, key: String): Boolean? {
        val match = Regex("\"$key\"\\s*:\\s*(true|false)").find(json)
        return match?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
    }

    /**
     * Uploads backup JSON payload to a WebDAV remote endpoint via HTTP PUT.
     */
    suspend fun uploadToWebDav(config: WebDavConfig, jsonContent: String): SyncResult = withContext(Dispatchers.IO) {
        if (config.serverUrl.isBlank()) {
            return@withContext SyncResult(false, "WebDAV 服务器地址不能为空")
        }
        try {
            val endpoint = if (config.serverUrl.endsWith("/")) "${config.serverUrl}wenyan_backup.json" else "${config.serverUrl}/wenyan_backup.json"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 15000

            if (config.username.isNotBlank()) {
                val auth = "${config.username}:${config.password}"
                val encodedAuth = android.util.Base64.encodeToString(auth.toByteArray(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP)
                conn.setRequestProperty("Authorization", "Basic $encodedAuth")
            }
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")

            conn.outputStream.use { os ->
                os.write(jsonContent.toByteArray(StandardCharsets.UTF_8))
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299 || responseCode == 201 || responseCode == 204) {
                SyncResult(true, "WebDAV 云端同步成功（状态码 $responseCode）")
            } else {
                SyncResult(false, "WebDAV 上传失败，状态码: $responseCode")
            }
        } catch (e: Exception) {
            SyncResult(false, "WebDAV 连接异常: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Downloads backup JSON payload from a WebDAV remote endpoint via HTTP GET.
     */
    suspend fun downloadFromWebDav(config: WebDavConfig): Pair<SyncResult, String?> = withContext(Dispatchers.IO) {
        if (config.serverUrl.isBlank()) {
            return@withContext Pair(SyncResult(false, "WebDAV 服务器地址不能为空"), null)
        }
        try {
            val endpoint = if (config.serverUrl.endsWith("/")) "${config.serverUrl}wenyan_backup.json" else "${config.serverUrl}/wenyan_backup.json"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 15000

            if (config.username.isNotBlank()) {
                val auth = "${config.username}:${config.password}"
                val encodedAuth = android.util.Base64.encodeToString(auth.toByteArray(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP)
                conn.setRequestProperty("Authorization", "Basic $encodedAuth")
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val content = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                Pair(SyncResult(true, "从云端成功拉取备份数据"), content)
            } else {
                Pair(SyncResult(false, "云端未找到备份或拉取失败，状态码: $responseCode"), null)
            }
        } catch (e: Exception) {
            Pair(SyncResult(false, "WebDAV 连接异常: ${e.localizedMessage ?: e.message}"), null)
        }
    }
}
