package com.loomora.core.offlineai

import com.loomora.core.database.dao.TrialOperationDao
import com.loomora.core.database.entity.TrialOperationEntity
import com.loomora.core.model.Capability
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

interface TrialReservationPort {
    suspend fun reserve(
        logicalJobKey: String,
        capability: Capability = Capability.SMART_INSIGHTS
    ): TrialReservation

    suspend fun commit(
        reservation: TrialReservation,
        resultRevisionId: String? = null
    )

    suspend fun release(reservation: TrialReservation)
}

data class TrialReservation(
    val id: String,
    val logicalJobKey: String,
    val capability: Capability,
    val reserved: Boolean
)

@Singleton
class DurableTrialReservationPort @Inject constructor(
    private val trialOperationDao: TrialOperationDao
) : TrialReservationPort {
    override suspend fun reserve(
        logicalJobKey: String,
        capability: Capability
    ): TrialReservation {
        val now = System.currentTimeMillis()
        val operationId = stableOperationId(logicalJobKey, capability)
        trialOperationDao.insertIfAbsent(
            TrialOperationEntity(
                trialOperationId = operationId,
                logicalJobKey = logicalJobKey,
                capability = capability.name,
                status = "RESERVED",
                reservedAt = now,
                committedAt = null,
                releasedAt = null,
                resultRevisionId = null,
                updatedAt = now
            )
        )
        val operation = requireNotNull(
            trialOperationDao.getByLogicalJobAndCapability(logicalJobKey, capability.name)
        )
        return TrialReservation(
            id = operation.trialOperationId,
            logicalJobKey = operation.logicalJobKey,
            capability = capability,
            reserved = operation.status == "RESERVED"
        )
    }

    override suspend fun commit(
        reservation: TrialReservation,
        resultRevisionId: String?
    ) {
        trialOperationDao.commit(
            trialOperationId = reservation.id,
            resultRevisionId = resultRevisionId,
            committedAt = System.currentTimeMillis()
        )
    }

    override suspend fun release(reservation: TrialReservation) {
        trialOperationDao.release(
            trialOperationId = reservation.id,
            releasedAt = System.currentTimeMillis()
        )
    }

    private fun stableOperationId(logicalJobKey: String, capability: Capability): String {
        val value = "$logicalJobKey|${capability.name}"
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }.take(32)
    }
}
