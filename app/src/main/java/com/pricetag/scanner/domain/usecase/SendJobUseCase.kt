package com.pricetag.scanner.domain.usecase

import com.pricetag.scanner.data.db.entity.JobEntity
import com.pricetag.scanner.data.network.SocketManager
import com.pricetag.scanner.domain.model.ScanJob
import com.pricetag.scanner.domain.repository.JobRepository
import javax.inject.Inject

sealed class SendResult {
    data class Success(val jobId: Long, val serverResponse: String) : SendResult()
    data class Queued(val jobId: Long)                              : SendResult()
    data class Failure(val message: String)                         : SendResult()
}

class SendJobUseCase @Inject constructor(
    private val socketManager:    SocketManager,
    private val jobRepository:    JobRepository,
    private val buildPayloadUseCase: BuildPayloadUseCase,
) {
    suspend operator fun invoke(job: ScanJob): SendResult {
        // 1. Build and validate payload
        val payloadResult = buildPayloadUseCase(job)
        if (payloadResult.isFailure) {
            return SendResult.Failure(
                payloadResult.exceptionOrNull()?.message ?: "Invalid job"
            )
        }
        val payload = payloadResult.getOrThrow()

        // 2. Persist job locally (as PENDING) — ensures no data loss
        val entity = JobEntity(
            payload   = payload,
            barcodes  = job.barcodes.joinToString(","),
            tagType   = job.tagType.protocol,
            unitType  = job.unitType.protocol,
            copies    = job.copies,
            timestamp = job.timestamp,
        )
        val savedId = jobRepository.saveJob(entity)

        // 3. Attempt to send via socket
        if (!socketManager.isConnected) {
            // Not connected — job is safely queued in DB; will be sent when reconnected
            return SendResult.Queued(savedId)
        }

        return try {
            val response = socketManager.send(payload)
            if (response != null && response.startsWith("OK")) {
                jobRepository.markSent(savedId, response)
                SendResult.Success(savedId, response)
            } else {
                val errMsg = response ?: "No response from server"
                jobRepository.markFailed(savedId, errMsg)
                SendResult.Failure(errMsg)
            }
        } catch (e: Exception) {
            val errMsg = e.message ?: "Send failed"
            jobRepository.markFailed(savedId, errMsg)
            SendResult.Failure(errMsg)
        }
    }
}
