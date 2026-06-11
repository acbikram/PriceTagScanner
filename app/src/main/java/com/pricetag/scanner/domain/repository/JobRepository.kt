package com.pricetag.scanner.domain.repository

import com.pricetag.scanner.data.db.entity.JobEntity
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    suspend fun saveJob(job: JobEntity): Long
    suspend fun markSent(id: Long, serverResponse: String)
    suspend fun markFailed(id: Long, error: String)
    suspend fun deleteJob(id: Long)
    suspend fun deleteAll()
    fun getPendingJobs(): Flow<List<JobEntity>>
    fun getAllJobs(): Flow<List<JobEntity>>
    suspend fun getPendingList(): List<JobEntity>
}
