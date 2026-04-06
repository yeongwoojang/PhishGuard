package com.example.phishguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.phishguard.data.local.entity.ThreatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreatDao {

    //_ 전체 이력 조회 — 최신순
    @Query("SELECT * FROM threats ORDER BY analyzedAt DESC")
    fun getAllThreats(): Flow<List<ThreatEntity>>

    //_ 위험/주의만 조회
    @Query("""
    SELECT COUNT(*) FROM threats 
    WHERE messageText = :text 
    AND analyzedAt > :since
""")
    suspend fun countRecentDuplicate(text: String, since: Long): Int

    @Query("""
        SELECT * FROM threats 
        WHERE riskLevel IN ('DANGER', 'CAUTION') 
        ORDER BY analyzedAt DESC
    """)
    fun getDangerousThreats(): Flow<List<ThreatEntity>>

    //_ 저장
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(threat: ThreatEntity)

    //_ 전체 삭제
    @Query("DELETE FROM threats")
    suspend fun deleteAll()

    //_ 오래된 기록 삭제 (30일 이전)
    @Query("DELETE FROM threats WHERE analyzedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}