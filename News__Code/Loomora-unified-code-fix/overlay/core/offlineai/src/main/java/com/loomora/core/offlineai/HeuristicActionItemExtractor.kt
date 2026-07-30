package com.loomora.core.offlineai

import com.loomora.core.model.ActionItem

/**
 * Conservative bilingual fallback used when the local LLM is unavailable.
 *
 * It extracts confirmed assignments/commitments, rejects questions and negations,
 * and can return more than one task when a transcript row contains multiple clauses.
 */
internal object HeuristicActionItemExtractor {
    fun isActionable(text: String): Boolean {
        val cleaned = normalizeWhitespace(text)
        val wordCount = cleaned.words().size
        if (wordCount !in 3..70) return false
        if (cleaned.endsWith("?") || questionPrefixRegex.containsMatchIn(cleaned)) return false
        if (negatedActionRegex.containsMatchIn(cleaned)) return false
        if (!concreteActionVerbRegex.containsMatchIn(cleaned)) return false
        return assignmentRegex.containsMatchIn(cleaned) ||
            commitmentRegex.containsMatchIn(cleaned) ||
            explicitTaskPrefixRegex.containsMatchIn(cleaned) ||
            imperativeActionRegex.containsMatchIn(cleaned) ||
            namedImperativeRegex.containsMatchIn(cleaned) ||
            namedDirectActionRegex.containsMatchIn(cleaned)
    }

    fun extract(text: String, evidenceSegmentIds: List<String>): ActionItem? =
        extractAll(text, evidenceSegmentIds).firstOrNull()

    fun extractAll(text: String, evidenceSegmentIds: List<String>): List<ActionItem> {
        val evidence = evidenceSegmentIds.filter(String::isNotBlank).distinct()
        return splitCandidateClauses(text)
            .mapNotNull { clause -> extractSingle(clause, evidence) }
            .distinctBy { item ->
                listOf(
                    normalizeIdentity(item.task),
                    normalizeIdentity(item.assignee.orEmpty()),
                    item.dueDate?.lowercase().orEmpty()
                ).joinToString("|")
            }
    }

    private fun extractSingle(text: String, evidence: List<String>): ActionItem? {
        if (!isActionable(text)) return null
        val task = normalizeTask(text)
        if (task.words().size < 2) return null
        return ActionItem(
            task = task,
            assignee = extractAssignee(text),
            dueDate = extractDueDate(text),
            evidenceSegmentIds = evidence
        )
    }

    private fun splitCandidateClauses(text: String): List<String> {
        val normalized = normalizeWhitespace(text)
        if (normalized.isBlank()) return emptyList()

        val sentenceParts = normalized
            .split(Regex("""(?<=[.!?…;])\s+|[\r\n]+"""))
            .map(::normalizeWhitespace)
            .filter(String::isNotBlank)

        return sentenceParts.flatMap { sentence ->
            sentence.split(multiTaskConnectorRegex)
                .map(::normalizeWhitespace)
                .filter(String::isNotBlank)
        }
    }

    private fun normalizeTask(text: String): String {
        val withoutPrefix = normalizeWhitespace(text)
            .replace(explicitTaskPrefixRegex, "")
            .replace(assignmentPrefixRegex, "")
            .replace(namedImperativePrefixRegex, "")
            .replace(namedDirectActionPrefixRegex, "")
            .replace(pronounCommitmentPrefixRegex, "")
            .trim(' ', '-', ':', ';', '.', ',')
        return withoutPrefix.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase() else first.toString()
        }
    }

    private fun extractAssignee(text: String): String? {
        val match = assigneePatterns.firstNotNullOfOrNull { it.find(text) } ?: return null
        val candidate = match.groupValues.getOrNull(1)
            ?.trim(' ', ',', '.', ':', ';', '-')
            ?.replace(Regex("""\s+"""), " ")
            ?.replace(assigneeTailRegex, "")
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: return null
        if (normalizeIdentity(candidate) in nonPersonLabels) return null
        return candidate
    }

    private fun extractDueDate(text: String): String? =
        dateRegex.find(text)?.value
            ?: relativeDueDateRegex.find(text)?.value
            ?: timeRegex.find(text)?.value
            ?: duePhraseRegex.find(text)?.groupValues?.getOrNull(1)?.trim()

    private fun normalizeWhitespace(text: String): String =
        text.replace(Regex("""\s+"""), " ").trim()

    private fun normalizeIdentity(text: String): String =
        text.lowercase()
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()

    private fun String.words(): List<String> =
        split(Regex("""\s+""")).filter(String::isNotBlank)

    private val personToken = """[\p{L}][\p{L}\p{N}_-]*"""
    private val namePattern = """$personToken(?:\s+$personToken){0,2}?"""
    private val actionVerbStart =
        """(?:gửi|hoàn\s+thành|kiểm\s+tra|kiểm\s+thử|cập\s+nhật|chuẩn\s+bị|triển\s+khai|sửa|tạo|viết|liên\s+hệ|gọi|rà\s+soát|xác\s+nhận|nộp|phát\s+hành|lên\s+lịch|theo\s+dõi|send|finish|complete|review|update|prepare|implement|fix|create|write|contact|call|verify|submit|publish|schedule|test|follow\s+up)"""

    private val assigneePatterns = listOf(
        Regex(
            """(?:giao(?:\s+việc)?\s+cho|phân\s+công(?:\s+cho)?|nhờ)\s+($namePattern)(?=\s+$actionVerbStart)""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """(?:assigned\s+to|ask(?:ed)?|owner\s*:)\s+($namePattern)(?=\s+(?:to\s+)?$actionVerbStart)""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\b($namePattern)\s+(?:sẽ\s+phụ\s+trách|chịu\s+trách\s+nhiệm|phụ\s+trách)\s+$actionVerbStart(?![\p{L}\p{N}_])""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^\s*($namePattern)\s*[,:\-]\s*(?:please\s+|vui\s+lòng\s+)?$actionVerbStart(?![\p{L}\p{N}_])""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^\s*($namePattern)\s+(?:will|needs?\s+to|has\s+to)\s+$actionVerbStart""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """^\s*($namePattern)\s+$actionVerbStart""",
            RegexOption.IGNORE_CASE
        )
    )

    private val assigneeTailRegex = Regex(
        """\s+(?:sẽ|cần|phải|phụ\s+trách|chịu\s+trách\s+nhiệm|will|needs?|has|have)$""",
        RegexOption.IGNORE_CASE
    )

    private val nonPersonLabels = setOf(
        "tôi", "mình", "chúng ta", "chúng tôi", "bạn", "mọi người", "team", "nhóm",
        "i", "we", "you", "they", "everyone", "the team"
    )

    private val explicitTaskPrefixRegex = Regex(
        """^(?:action\s*item|todo|task|việc\s+cần\s+làm|công\s+việc)\s*[:\-]\s*""",
        RegexOption.IGNORE_CASE
    )
    private val assignmentPrefixRegex = Regex(
        """^(?:giao(?:\s+việc)?\s+cho|phân\s+công(?:\s+cho)?|nhờ)\s+$namePattern\s+""",
        RegexOption.IGNORE_CASE
    )
    private val namedImperativePrefixRegex = Regex(
        """^$namePattern\s*[,:\-]\s*(?:please\s+|vui\s+lòng\s+)?""",
        RegexOption.IGNORE_CASE
    )
    private val namedDirectActionPrefixRegex = Regex(
        """^$namePattern\s+(?=$actionVerbStart)""",
        RegexOption.IGNORE_CASE
    )
    private val pronounCommitmentPrefixRegex = Regex(
        """^(?:(?:chúng\s+ta|chúng\s+tôi|tôi|mình)\s+(?:cần|phải|sẽ)\s+|(?:we|i)\s+(?:need\s+to|have\s+to|will)\s+)""",
        RegexOption.IGNORE_CASE
    )
    private val assignmentRegex = Regex(
        """\b(?:giao(?:\s+việc)?\s+cho|phân\s+công|nhờ|phụ\s+trách|chịu\s+trách\s+nhiệm|assign(?:ed)?\s+to|owner\s*:|ask(?:ed)?)\b""",
        RegexOption.IGNORE_CASE
    )
    private val commitmentRegex = Regex(
        """\b(?:tôi|mình|chúng\s+ta|chúng\s+tôi|[\p{L}][\p{L}\p{N}_-]+)\s+(?:sẽ|cần|phải)\s+|\b(?:i|we|[A-Z][a-z]+)\s+(?:will|need\s+to|needs\s+to|have\s+to|has\s+to)\s+""",
        RegexOption.IGNORE_CASE
    )
    private val imperativeActionRegex = Regex(
        """^(?:(?:hãy|vui\s+lòng|please)\s+)?$actionVerbStart(?![\p{L}\p{N}_])""",
        RegexOption.IGNORE_CASE
    )
    private val namedImperativeRegex = Regex(
        """^\s*$namePattern\s*[,:\-]\s*(?:please\s+|vui\s+lòng\s+)?$actionVerbStart(?![\p{L}\p{N}_])""",
        RegexOption.IGNORE_CASE
    )
    private val namedDirectActionRegex = Regex(
        """^\s*$namePattern\s+$actionVerbStart(?![\p{L}\p{N}_])""",
        RegexOption.IGNORE_CASE
    )
    private val concreteActionVerbRegex = Regex(
        """(?<![\p{L}\p{N}_])$actionVerbStart(?![\p{L}\p{N}_])""",
        RegexOption.IGNORE_CASE
    )
    private val negatedActionRegex = Regex(
        """(?:không|chưa)\s+(?:cần|phải|nên|sẽ|gửi|làm|triển\s+khai|hoàn\s+thành|giao|phân\s+công)|""" +
            """(?:không\s+đồng\s+ý|đừng|chưa\s+cần)|""" +
            """(?:do\s+not|don't|doesn't|didn't|no\s+need\s+to|not\s+going\s+to|shouldn't|won't|mustn't|not\s+assigned)""",
        RegexOption.IGNORE_CASE
    )
    private val questionPrefixRegex = Regex(
        """^(?:ai|cái\s+gì|việc\s+gì|khi\s+nào|bao\s+giờ|ở\s+đâu|tại\s+sao|vì\s+sao|như\s+thế\s+nào|liệu|có\s+nên|có\s+thể|who|what|when|where|why|how|should\s+we|can\s+we|could\s+we|would\s+we)\b""",
        RegexOption.IGNORE_CASE
    )
    private val multiTaskConnectorRegex = Regex(
        """\s+(?:và|and)\s+(?=(?:(?:$namePattern)\s*(?:[,:\-]|\s+(?:sẽ|phụ\s+trách|will|needs?\s+to|has\s+to))|(?:hãy|vui\s+lòng|please)\s+|$actionVerbStart\b))""",
        RegexOption.IGNORE_CASE
    )
    private val dateRegex = Regex(
        """\b(?:\d{4}-\d{1,2}-\d{1,2}|\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?)\b"""
    )
    private val relativeDueDateRegex = Regex(
        """\b(?:hôm\s+nay|ngày\s+mai|cuối\s+tuần(?:\s+này)?|đầu\s+tuần\s+sau|tuần\s+sau|cuối\s+tháng(?:\s+này)?|thứ\s+hai|thứ\s+ba|thứ\s+tư|thứ\s+năm|thứ\s+sáu|thứ\s+bảy|chủ\s+nhật|today|tomorrow|this\s+weekend|end\s+of\s+(?:the\s+)?week|next\s+week|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b""",
        RegexOption.IGNORE_CASE
    )
    private val timeRegex = Regex(
        """\b(?:lúc\s+)?\d{1,2}(?::\d{2})?\s*(?:giờ|h|am|pm)?\s*(?:sáng|trưa|chiều|tối)?\b""",
        RegexOption.IGNORE_CASE
    )
    private val duePhraseRegex = Regex(
        """(?:trước|hạn|đến|by|before|due)\s+([^,.;]{2,40})""",
        RegexOption.IGNORE_CASE
    )
}
