package com.ancient.wenyan.data.db.dao

import androidx.room.*
import com.ancient.wenyan.data.db.entities.CardStateEntity
import com.ancient.wenyan.data.db.entities.DailyRecordEntity
import com.ancient.wenyan.data.db.entities.ReviewLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardStateDao {
    @Query("SELECT * FROM card_states")
    fun getAllCardStatesFlow(): Flow<List<CardStateEntity>>

    @Query("SELECT * FROM card_states")
    suspend fun getAllCardStates(): List<CardStateEntity>

    @Query("SELECT * FROM card_states WHERE cardId = :cardId LIMIT 1")
    suspend fun getCardStateById(cardId: String): CardStateEntity?

    @Query("SELECT * FROM card_states WHERE isLeech = 1")
    suspend fun getLeechCardStates(): List<CardStateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCardState(cardState: CardStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCardStates(cardStates: List<CardStateEntity>)

    @Query("DELETE FROM card_states WHERE cardId = :cardId")
    suspend fun deleteCardState(cardId: String)

    @Query("DELETE FROM card_states")
    suspend fun clearAll()
}

@Dao
interface ReviewLogDao {
    @Query("SELECT * FROM review_logs ORDER BY reviewTime ASC")
    fun getAllReviewLogsFlow(): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM review_logs ORDER BY reviewTime ASC")
    suspend fun getAllReviewLogs(): List<ReviewLogEntity>

    @Query("SELECT * FROM review_logs WHERE cardId = :cardId ORDER BY reviewTime ASC")
    suspend fun getReviewLogsForCard(cardId: String): List<ReviewLogEntity>

    @Query("SELECT COUNT(*) FROM review_logs")
    suspend fun getReviewLogCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ReviewLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<ReviewLogEntity>)

    @Query("DELETE FROM review_logs")
    suspend fun clearAll()
}

@Dao
interface DailyRecordDao {
    @Query("SELECT * FROM daily_study_records")
    fun getAllDailyRecordsFlow(): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_study_records")
    suspend fun getAllDailyRecords(): List<DailyRecordEntity>

    @Query("SELECT * FROM daily_study_records WHERE date = :date LIMIT 1")
    suspend fun getRecordForDate(date: String): DailyRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: DailyRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecords(records: List<DailyRecordEntity>)

    @Query("DELETE FROM daily_study_records")
    suspend fun clearAll()
}
