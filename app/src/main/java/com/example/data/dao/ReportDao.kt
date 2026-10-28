package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun getAllReportsFlow(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: String): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Query("UPDATE reports SET status = :status, adminNote = :note WHERE id = :id")
    suspend fun updateReportStatus(id: String, status: String, note: String)

    @Query("SELECT COUNT(*) FROM reports WHERE status = 'PENDING' OR status = 'INVESTIGATING'")
    fun getPendingReportsCountFlow(): Flow<Int>
}
