package com.ancient.wenyan.data.db

import android.content.Context
import android.content.SharedPreferences
import com.ancient.wenyan.data.db.entities.CardStateEntity
import com.ancient.wenyan.data.db.entities.DailyRecordEntity
import com.ancient.wenyan.data.db.entities.ReviewLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Migration helper to migrate legacy SharedPreferences string serialization into Room SQLite.
 */
object DatabaseMigrationHelper {

    private const val PREF_MIGRATION_FLAG = "has_migrated_to_room_v1"
    private const val PREF_CARD_STATES_KEY_V2 = "pref_persisted_card_states_v2"
    private const val PREF_CARD_STATES_KEY_V1 = "pref_persisted_card_states_v1"
    private const val PREF_REVIEW_LOGS_KEY = "pref_persisted_review_logs_v1"

    suspend fun migrateIfNeeded(context: Context, database: AppDatabase) = withContext(Dispatchers.IO) {
        val prefs: SharedPreferences = context.getSharedPreferences("wenyan_study_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_MIGRATION_FLAG, false)) {
            return@withContext
        }

        val cardStateDao = database.cardStateDao()
        val reviewLogDao = database.reviewLogDao()
        val dailyRecordDao = database.dailyRecordDao()

        // 1. Migrate Card States (prioritize v2 then fallback to v1)
        val savedCardsStr = prefs.getString(PREF_CARD_STATES_KEY_V2, null)?.takeIf { it.isNotBlank() }
            ?: prefs.getString(PREF_CARD_STATES_KEY_V1, null)
        if (!savedCardsStr.isNullOrBlank()) {
            val lines = savedCardsStr.split("\n")
            val cardEntities = mutableListOf<CardStateEntity>()
            for (line in lines) {
                if (line.isBlank()) continue
                val parts = line.split("|")
                if (parts.size >= 11) {
                    try {
                        val lapses = parts[8].toIntOrNull() ?: 0
                        cardEntities.add(
                            CardStateEntity(
                                cardId = parts[0],
                                state = parts[1],
                                step = parts[2].toIntOrNull(),
                                stability = parts[3].toDoubleOrNull() ?: 0.0,
                                difficulty = parts[4].toDoubleOrNull() ?: 0.0,
                                elapsedDays = parts[5].toIntOrNull() ?: 0,
                                scheduledDays = parts[6].toIntOrNull() ?: 0,
                                reps = parts[7].toIntOrNull() ?: 0,
                                lapses = lapses,
                                lastReviewTime = parts[9].toLongOrNull(),
                                dueTime = parts[10].toLongOrNull() ?: 0L,
                                isLeech = lapses >= 4
                            )
                        )
                    } catch (_: Exception) {}
                }
            }
            if (cardEntities.isNotEmpty()) {
                cardStateDao.upsertCardStates(cardEntities)
            }
        }

        // 2. Migrate Review Logs
        val savedLogsStr = prefs.getString(PREF_REVIEW_LOGS_KEY, null)
        if (!savedLogsStr.isNullOrBlank()) {
            val lines = savedLogsStr.split("\n")
            val logEntities = mutableListOf<ReviewLogEntity>()
            for (line in lines) {
                if (line.isBlank()) continue
                val p = line.split("|")
                if (p.size >= 8) {
                    try {
                        logEntities.add(
                            ReviewLogEntity(
                                cardId = p[0],
                                rating = p[1],
                                previousState = p[2],
                                currentState = p[3],
                                stability = p[4].toDoubleOrNull() ?: 1.0,
                                difficulty = p[5].toDoubleOrNull() ?: 5.0,
                                elapsedDays = p[6].toIntOrNull() ?: 0,
                                scheduledDays = p[7].toIntOrNull() ?: 1,
                                reviewTime = p.getOrNull(8)?.toLongOrNull() ?: System.currentTimeMillis()
                            )
                        )
                    } catch (_: Exception) {}
                }
            }
            if (logEntities.isNotEmpty()) {
                reviewLogDao.insertLogs(logEntities)
            }
        }

        // 3. Migrate Daily Study Records
        val allEntries = prefs.all
        val dailyMap = mutableMapOf<String, Pair<Int, Int>>() // date -> (review, new)
        for ((key, value) in allEntries) {
            if (key.startsWith("review_date_") && value is Int) {
                val date = key.removePrefix("review_date_")
                val cur = dailyMap[date] ?: Pair(0, 0)
                dailyMap[date] = cur.copy(first = value)
            } else if (key.startsWith("pref_daily_new_learned_") && value is Int) {
                val date = key.removePrefix("pref_daily_new_learned_")
                val cur = dailyMap[date] ?: Pair(0, 0)
                dailyMap[date] = cur.copy(second = value)
            }
        }
        if (dailyMap.isNotEmpty()) {
            val dailyEntities = dailyMap.map { (date, counts) ->
                DailyRecordEntity(
                    date = date,
                    reviewCount = counts.first,
                    newLearnedCount = counts.second
                )
            }
            dailyRecordDao.upsertRecords(dailyEntities)
        }

        // Mark as migrated
        prefs.edit().putBoolean(PREF_MIGRATION_FLAG, true).apply()
    }
}
