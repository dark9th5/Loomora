package com.loomora.core.offlineai

import com.loomora.core.model.SpeakerTurn
import com.loomora.core.model.TranscriptSegment

object TranscriptSpeakerFusion {
    private const val MERGE_TRANSCRIPT_GAP_MS = 1_800L
    private const val MIN_WORDS_FOR_PROJECTED_TURN = 7

    fun fuse(
        transcript: List<TranscriptSegment>,
        turns: List<SpeakerTurn>
    ): List<TranscriptSegment> {
        val orderedTranscript = transcript
            .filter { it.endMs > it.startMs && it.text.isNotBlank() }
            .sortedWith(compareBy({ it.startMs }, { it.endMs }))
        if (orderedTranscript.isEmpty()) return emptyList()
        if (turns.isEmpty()) return compact(orderedTranscript)

        val stableTurns = SpeakerTurnStabilizer.stabilize(turns)
        if (stableTurns.isEmpty()) return compact(orderedTranscript)

        val assigned = if (orderedTranscript.size == 1 && shouldProject(orderedTranscript.single(), stableTurns)) {
            projectOntoStableTurns(orderedTranscript.single(), stableTurns)
        } else {
            orderedTranscript.map { assignDominantSpeaker(it, stableTurns) }
        }
        return compact(assigned)
    }

    /** Also compacts old persisted revisions at display/export time. */
    fun compact(transcript: List<TranscriptSegment>): List<TranscriptSegment> {
        if (transcript.isEmpty()) return emptyList()
        val result = mutableListOf<TranscriptSegment>()
        transcript
            .filter { it.endMs > it.startMs && it.text.isNotBlank() }
            .sortedWith(compareBy({ it.startMs }, { it.endMs }))
            .forEach { next ->
                val previous = result.lastOrNull()
                val sameSpeaker = previous?.speakerLabel == next.speakerLabel
                val gap = previous?.let { next.startMs - it.endMs } ?: Long.MAX_VALUE
                if (previous != null && sameSpeaker && gap <= MERGE_TRANSCRIPT_GAP_MS) {
                    result[result.lastIndex] = previous.copy(
                        endMs = maxOf(previous.endMs, next.endMs),
                        text = joinText(previous.text, next.text),
                        rawText = joinText(previous.rawText, next.rawText),
                        speakerConfidence = weightedConfidence(previous, next),
                        speakerIsUncertain = previous.speakerIsUncertain || next.speakerIsUncertain
                    )
                } else {
                    result += next.copy(
                        text = normalize(next.text),
                        rawText = normalize(next.rawText)
                    )
                }
            }
        return result
    }

    private fun shouldProject(segment: TranscriptSegment, turns: List<SpeakerTurn>): Boolean {
        val overlapping = turns.filter { overlapMs(segment.startMs, segment.endMs, it.startMs, it.endMs) > 0L }
        val speakers = overlapping.map { it.speakerLabel }.distinct()
        val words = segment.text.wordCount()
        return speakers.size > 1 && words >= speakers.size * MIN_WORDS_FOR_PROJECTED_TURN
    }

    private fun projectOntoStableTurns(
        segment: TranscriptSegment,
        turns: List<SpeakerTurn>
    ): List<TranscriptSegment> {
        val blocks = SpeakerTurnStabilizer.toSpeakerBlocks(
            turns.filter { overlapMs(segment.startMs, segment.endMs, it.startMs, it.endMs) > 0L }
        )
        if (blocks.size <= 1) return listOf(assignDominantSpeaker(segment, turns))

        val parts = TranscriptTextSegmenter.splitAcrossIntervals(segment.text, blocks.size)
        if (parts.size <= 1) return listOf(assignDominantSpeaker(segment, turns))

        return blocks.take(parts.size).mapIndexed { index, turn ->
            segment.copy(
                startMs = maxOf(segment.startMs, turn.startMs),
                endMs = minOf(segment.endMs, turn.endMs).coerceAtLeast(maxOf(segment.startMs, turn.startMs) + 1L),
                text = parts[index],
                rawText = parts[index],
                speakerLabel = turn.speakerLabel,
                speakerConfidence = 1f,
                speakerIsUncertain = turn.isUncertain || turn.isOverlapped
            )
        }
    }

    private fun assignDominantSpeaker(
        segment: TranscriptSegment,
        turns: List<SpeakerTurn>
    ): TranscriptSegment {
        val overlaps = turns.mapNotNull { turn ->
            val overlap = overlapMs(segment.startMs, segment.endMs, turn.startMs, turn.endMs)
            if (overlap <= 0L) null else SpeakerOverlap(turn, overlap)
        }
        if (overlaps.isEmpty()) {
            val nearest = turns.minByOrNull { turn ->
                when {
                    segment.endMs < turn.startMs -> turn.startMs - segment.endMs
                    segment.startMs > turn.endMs -> segment.startMs - turn.endMs
                    else -> 0L
                }
            }
            return if (nearest != null && distanceMs(segment, nearest) <= 1_000L) {
                segment.copy(
                    speakerLabel = nearest.speakerLabel,
                    speakerConfidence = 0.5f,
                    speakerIsUncertain = true
                )
            } else {
                segment.copy(speakerLabel = null, speakerConfidence = null, speakerIsUncertain = true)
            }
        }

        val grouped = overlaps.groupBy { it.turn.speakerLabel }
            .mapValues { (_, values) -> values.sumOf { it.overlapMs } }
            .entries.sortedByDescending { it.value }
        val bestLabel = grouped.first().key
        val bestOverlap = grouped.first().value
        val totalOverlap = grouped.sumOf { it.value }.coerceAtLeast(1L)
        val confidence = bestOverlap.toFloat() / totalOverlap.toFloat()
        val bestTurn = overlaps.filter { it.turn.speakerLabel == bestLabel }.maxBy { it.overlapMs }.turn
        return segment.copy(
            speakerLabel = bestLabel,
            speakerConfidence = confidence.coerceIn(0f, 1f),
            speakerIsUncertain = confidence < 0.65f || bestTurn.isUncertain || bestTurn.isOverlapped
        )
    }

    private fun weightedConfidence(a: TranscriptSegment, b: TranscriptSegment): Float? {
        val aConfidence = a.speakerConfidence ?: return b.speakerConfidence
        val bConfidence = b.speakerConfidence ?: return aConfidence
        val aDuration = (a.endMs - a.startMs).coerceAtLeast(1L)
        val bDuration = (b.endMs - b.startMs).coerceAtLeast(1L)
        return ((aConfidence * aDuration.toFloat()) + (bConfidence * bDuration.toFloat())) / (aDuration + bDuration).toFloat()
    }

    private fun distanceMs(segment: TranscriptSegment, turn: SpeakerTurn): Long = when {
        segment.endMs < turn.startMs -> turn.startMs - segment.endMs
        segment.startMs > turn.endMs -> segment.startMs - turn.endMs
        else -> 0L
    }

    private fun overlapMs(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Long =
        (minOf(aEnd, bEnd) - maxOf(aStart, bStart)).coerceAtLeast(0L)

    private fun joinText(first: String, second: String): String = normalize("$first $second")
    private fun normalize(text: String): String = text.replace(Regex("\\s+"), " ").trim()
    private fun String.wordCount(): Int = split(Regex("\\s+")).count(String::isNotBlank)

    private data class SpeakerOverlap(val turn: SpeakerTurn, val overlapMs: Long)
}

/**
 * Removes very short speaker islands caused by clustering noise, while preserving
 * genuine speaker changes. A short turn is relabelled only when it is surrounded
 * by the same speaker, so short real interjections are not blindly deleted.
 */
internal object SpeakerTurnStabilizer {
    private const val MERGE_SAME_SPEAKER_GAP_MS = 750L
    private const val ISLAND_MAX_DURATION_MS = 1_400L
    private const val ISLAND_MAX_EDGE_GAP_MS = 900L

    fun stabilize(turns: List<SpeakerTurn>): List<SpeakerTurn> {
        var current = sanitize(turns)
        if (current.isEmpty()) return emptyList()
        current = mergeSameSpeaker(current)
        repeat(2) {
            current = suppressSpeakerIslands(current)
            current = mergeSameSpeaker(current)
        }
        return normalizeLabels(current)
    }

    fun toSpeakerBlocks(turns: List<SpeakerTurn>): List<SpeakerTurn> = mergeSameSpeaker(sanitize(turns))

    private fun sanitize(turns: List<SpeakerTurn>): List<SpeakerTurn> = turns
        .filter { it.endMs > it.startMs }
        .sortedWith(compareBy({ it.startMs }, { it.endMs }))
        .map { it.copy(startMs = it.startMs.coerceAtLeast(0L), endMs = it.endMs.coerceAtLeast(it.startMs + 1L)) }

    private fun suppressSpeakerIslands(turns: List<SpeakerTurn>): List<SpeakerTurn> {
        if (turns.size < 3) return turns
        return turns.mapIndexed { index, current ->
            val previous = turns.getOrNull(index - 1)
            val next = turns.getOrNull(index + 1)
            val duration = current.endMs - current.startMs
            val surroundedBySameSpeaker = previous != null && next != null &&
                previous.speakerIndex == next.speakerIndex &&
                current.speakerIndex != previous.speakerIndex &&
                current.startMs - previous.endMs <= ISLAND_MAX_EDGE_GAP_MS &&
                next.startMs - current.endMs <= ISLAND_MAX_EDGE_GAP_MS
            if (duration <= ISLAND_MAX_DURATION_MS && surroundedBySameSpeaker) {
                current.copy(
                    speakerIndex = previous!!.speakerIndex,
                    speakerLabel = previous.speakerLabel,
                    isUncertain = true,
                    alternateSpeakerLabels = (current.alternateSpeakerLabels + current.speakerLabel).distinct()
                )
            } else {
                current
            }
        }
    }

    private fun mergeSameSpeaker(turns: List<SpeakerTurn>): List<SpeakerTurn> {
        if (turns.isEmpty()) return emptyList()
        val result = mutableListOf<SpeakerTurn>()
        turns.forEach { next ->
            val previous = result.lastOrNull()
            if (previous != null && previous.speakerIndex == next.speakerIndex &&
                next.startMs - previous.endMs <= MERGE_SAME_SPEAKER_GAP_MS
            ) {
                result[result.lastIndex] = previous.copy(
                    endMs = maxOf(previous.endMs, next.endMs),
                    confidence = average(previous.confidence, next.confidence),
                    isOverlapped = previous.isOverlapped || next.isOverlapped,
                    isUncertain = previous.isUncertain || next.isUncertain,
                    alternateSpeakerLabels = (previous.alternateSpeakerLabels + next.alternateSpeakerLabels).distinct()
                )
            } else {
                result += next
            }
        }
        return result
    }

    private fun normalizeLabels(turns: List<SpeakerTurn>): List<SpeakerTurn> {
        val indexes = linkedMapOf<Int, Int>()
        turns.forEach { indexes.getOrPut(it.speakerIndex) { indexes.size } }
        return turns.map { turn ->
            val index = indexes.getValue(turn.speakerIndex)
            turn.copy(speakerIndex = index, speakerLabel = "Speaker ${index + 1}")
        }
    }

    private fun average(a: Float?, b: Float?): Float? = when {
        a == null -> b
        b == null -> a
        else -> (a + b) / 2f
    }
}
