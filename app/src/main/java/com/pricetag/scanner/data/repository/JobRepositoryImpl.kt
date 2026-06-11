package com.pricetag.scanner.data.repository

import com.pricetag.scanner.data.db.dao.JobDao
import com.pricetag.scanner.data.db.entity.JobEntity
import com.pricetag.scanner.domain.repository.JobRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepositoryImpl @Inject constructor(
    private val dao: JobDao,
) : JobRepository {

    override suspend fun saveJob(job: JobEntity): Long = dao.insert(job)

    override suspend fun markSent(id: Long, serverResponse: String) =
        dao.updateStatus(id, JobEntity.STATUS_SENT, serverResponse)

    override suspend fun markFailed(id: Long, error: String) =
        dao.markFailed(id, error)

    override suspend fun deleteJob(id: Long) = dao.deleteById(id)

    override suspend fun deleteAll() = dao.deleteAll()

    override fun getPendingJobs(): Flow<List<JobEntity>> = dao.getPendingJobsFlow()

    override fun getAllJobs(): Flow<List<JobEntity>> = dao.getAllJobsFlow()

    override suspend fun getPendingList(): List<JobEntity> = dao.getPendingJobsList()
}
