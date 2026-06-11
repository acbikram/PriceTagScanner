package com.pricetag.scanner.data.db.dao

import androidx.room.*
import com.pricetag.scanner.data.db.entity.JobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: JobEntity): Long

    @Query("UPDATE jobs SET status = :status, serverResponse = :response WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, response: String)

    @Query("UPDATE jobs SET status = 'FAILED', errorMessage = :error, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markFailed(id: Long, error: String)

    @Query("DELETE FROM jobs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM jobs")
    suspend fun deleteAll()

    @Query("SELECT * FROM jobs WHERE status = 'PENDING' ORDER BY timestamp ASC")
    fun getPendingJobsFlow(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingJobsList(): List<JobEntity>

    @Query("SELECT * FROM jobs ORDER BY timestamp DESC LIMIT 100")
    fun getAllJobsFlow(): Flow<List<JobEntity>>
}
