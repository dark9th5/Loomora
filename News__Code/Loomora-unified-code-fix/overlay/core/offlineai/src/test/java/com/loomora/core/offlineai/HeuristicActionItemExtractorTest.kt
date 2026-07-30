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
        assertTrue(item.task.startsWith("Cập nhật", ignoreCase = true))
    }

    @Test
    fun rejectsQuestionsSuggestionsAndNegations() {
        listOf(
            "Ai sẽ gửi báo cáo vào ngày mai?",
            "Chúng ta không cần gửi báo cáo nữa.",
            "Có nên gửi báo cáo không?",
            "Tôi không đồng ý giao cho Lan cập nhật báo cáo.",
            "Chắc có thể xem lại sau."
        ).forEach { text ->
            assertFalse(text, HeuristicActionItemExtractor.isActionable(text))
            assertNull(HeuristicActionItemExtractor.extract(text, listOf("s")))
        }
    }

    @Test
    fun extractsNamedEnglishImperative() {
        val item = HeuristicActionItemExtractor.extract(
            "Lan, please send the final report tomorrow.",
            listOf("segment-2")
        )
        requireNotNull(item)
        assertEquals("Lan", item.assignee)
        assertEquals("tomorrow", item.dueDate)
        assertTrue(item.task.startsWith("Send", ignoreCase = true))
    }

    @Test
    fun extractsTwoTasksFromOneTranscriptRow() {
        val items = HeuristicActionItemExtractor.extractAll(
            "Lan cập nhật báo cáo trước thứ Sáu và Minh sẽ kiểm thử trước ngày 12/08.",
            listOf("segment-3")
        )
        assertEquals(2, items.size)
        assertTrue(items.any { it.assignee == "Lan" })
        assertTrue(items.any { it.assignee == "Minh" })
    }

    @Test
    fun extractsFirstPersonCommitmentWithoutInventingAssignee() {
        val item = HeuristicActionItemExtractor.extract(
            "Tôi sẽ gửi bản thiết kế lúc 3 giờ chiều.",
            listOf("segment-4")
        )
        requireNotNull(item)
        assertNull(item.assignee)
        assertTrue(item.dueDate?.contains("3") == true)
    }
}
