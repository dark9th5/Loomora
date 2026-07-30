package com.loomora.core.offlineai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeuristicActionItemExtractorTest {
    @Test
    fun extractsVietnameseAssignmentAndDueDate() {
        val item = HeuristicActionItemExtractor.extract(
            "Giao cho Lan cập nhật báo cáo trước thứ Sáu.",
            listOf("segment-1")
        )

        requireNotNull(item)
        assertEquals("Lan", item.assignee)
        assertEquals("thứ Sáu", item.dueDate)
        assertEquals(listOf("segment-1"), item.evidenceSegmentIds)
    }

    @Test
    fun rejectsQuestionAndNegatedTask() {
        assertFalse(HeuristicActionItemExtractor.isActionable("Ai sẽ gửi báo cáo vào ngày mai?"))
        assertFalse(HeuristicActionItemExtractor.isActionable("Chúng ta không cần gửi báo cáo nữa."))
        assertNull(HeuristicActionItemExtractor.extract("Chúng ta không cần gửi báo cáo nữa.", emptyList()))
    }

    @Test
    fun extractsEnglishCommitment() {
        val item = HeuristicActionItemExtractor.extract(
            "Minh will send the approved design by Friday.",
            listOf("segment-2")
        )

        requireNotNull(item)
        assertEquals("Minh", item.assignee)
        assertEquals("Friday", item.dueDate)
        assertTrue(item.task.contains("send", ignoreCase = true))
    }
}
