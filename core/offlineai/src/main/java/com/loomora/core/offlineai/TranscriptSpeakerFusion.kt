package com.loomora.core.offlineai

import com.loomora.core.model.SpeakerTurn
import com.loomora.core.model.TranscriptSegment

object TranscriptSpeakerFusion {
    fun fuse(
        transcript: List<TranscriptSegment>,
        turns: List<SpeakerTurn>
    ): List<TranscriptSegment> {
        if (transcript.isEmpty() || turns.isEmpty()) return transcript
        if (transcript.size == 1 && shouldProjectSparseCoverage(transcript.single(), turns)) {
            return projectOntoTurns(transcript, turns)
        }
        if (transcript.none { segment -> turns.any { turn -> overlapMs(segment.startMs, segment.endMs, turn.startMs, turn.endMs) > 0L } }) {
            return projectOntoTurns(transcript, turns)
        }
        val boundaries = turns.flatMap { listOf(it.startMs, it.endMs) }.distinct().sorted()
        return transcript.sortedWith(compareBy({ it.startMs }, { it.endMs }))
            .flatMap { segment -> splitAtBoundaries(segment, boundaries) }
            .map { segment -> assignSpeaker(segment, turns) }
            .filter { it.endMs > it.startMs && it.text.isNotBlank() }
    }

    private fun shouldProjectSparseCoverage(
        segment: TranscriptSegment,
        turns: List<SpeakerTurn>
    ): Boolean {
        val duration = (segment.endMs - segment.startMs).coerceAtLeast(1L)
        val covered = turns.sumOf { turn ->
            overlapMs(segment.startMs, segment.endMs, turn.startMs, turn.endMs)
        }.coerceAtMost(duration)
        return duration >= 5_000L && covered * 2L < duration
    }

    private fun projectOntoTurns(
        transcript: List<TranscriptSegment>,
        turns: List<SpeakerTurn>
    ): List<TranscriptSegment> {
        val orderedTurns = turns.sortedBy { it.startMs }
        val template = transcript.minBy { it.startMs }
        val text = transcript.sortedBy { it.startMs }.joinToString(" ") { it.text }.trim()
        val parts = TranscriptTextSegmenter.splitAcrossIntervals(text, orderedTurns.size)
        return orderedTurns.take(parts.size).mapIndexed { index, turn ->
            template.copy(
                startMs = turn.startMs,
                endMs = turn.endMs,
                text = parts[index],
                rawText = parts[index],
                speakerLabel = turn.speakerLabel,
                speakerConfidence = 1f,
                speakerIsUncertain = turn.isUncertain || turn.isOverlapped
            )
        }
    }

    private fun splitAtBoundaries(
        segment: TranscriptSegment,
        boundaries: List<Long>
    ): List<TranscriptSegment> {
        val internal = boundaries.filter { it > segment.startMs && it < segment.endMs }
        if (internal.isEmpty()) return listOf(segment)
        val intervals = (listOf(segment.startMs) + internal + listOf(segment.endMs)).zipWithNext()
        val textParts = TranscriptTextSegmenter.splitAcrossIntervals(segment.text, intervals.size)
        if (textParts.size <= 1) return listOf(segment)
        return intervals.take(textParts.size).mapIndexed { index, (start, end) ->
            segment.copy(
                startMs = start,
                endMs = end,
                text = textParts[index],
                rawText = textParts[index]
            )
        }
    }

    private fun assignSpeaker(
        segment: TranscriptSegment,
        turns: List<SpeakerTurn>
    ): TranscriptSegment {
        val overlaps = turns.mapNotNull { turn ->
            val overlap = overlapMs(segment.startMs, segment.endMs, turn.startMs, turn.endMs)
            if (overlap <= 0L) null else SpeakerOverlap(turn, overlap)
        }
        if (overlaps.isEmpty()) {
            return segment.copy(
                speakerLabel = null,
                speakerConfidence = null,
                speakerIsUncertain = true
            )
        }
        val sorted = overlaps.sortedWith(compareByDescending<SpeakerOverlap> { it.overlapMs }.thenBy { it.turn.speakerIndex })
        val best = sorted.first()
        val tied = sorted.filter { it.overlapMs == best.overlapMs }
        val overlapLabels = sorted.map { it.turn.speakerLabel }.distinct()
        val uncertain = tied.size > 1 || best.turn.isUncertain || best.turn.isOverlapped || overlapLabels.size > 1
        val label = if (overlapLabels.size > 1) {
            overlapLabels.joinToString(" + ")
        } else {
            best.turn.speakerLabel
        }
        val confidence = best.overlapMs.toFloat() / (segment.endMs - segment.startMs).coerceAtLeast(1L).toFloat()
        return segment.copy(
            speakerLabel = label,
            speakerConfidence = confidence.coerceIn(0f, 1f),
            speakerIsUncertain = uncertain
        )
    }

    private fun overlapMs(aStart: Long, aEnd: Long, bStart: Long, bEnd: Long): Long {
        return (minOf(aEnd, bEnd) - maxOf(aStart, bStart)).coerceAtLeast(0L)
    }

    private data class SpeakerOverlap(
        val turn: SpeakerTurn,
        val overlapMs: Long
    )
}
