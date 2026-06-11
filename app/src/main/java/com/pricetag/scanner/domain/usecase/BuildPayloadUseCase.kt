package com.pricetag.scanner.domain.usecase

import com.pricetag.scanner.domain.model.ScanJob
import com.pricetag.scanner.domain.model.TagType
import javax.inject.Inject

/**
 * Validates job completeness and builds the wire-format payload.
 * Returns Result.success(payload) or Result.failure with a user-friendly message.
 */
class BuildPayloadUseCase @Inject constructor() {

    operator fun invoke(job: ScanJob): Result<String> {
        if (job.barcodes.isEmpty() || job.barcodes.all { it.isBlank() }) {
            return Result.failure(IllegalArgumentException("No barcode scanned"))
        }
        if (job.tagType.is4PcsVariant && job.tagType != TagType.FOUR_PCS_SAME) {
            val nonEmpty = job.barcodes.count { it.isNotBlank() }
            if (nonEmpty == 0) {
                return Result.failure(IllegalArgumentException("Scan at least one barcode"))
            }
        }
        if (job.copies < 1) {
            return Result.failure(IllegalArgumentException("Copies must be at least 1"))
        }
        return Result.success(job.toWireFormat())
    }
}
