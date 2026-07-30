package com.loomora.core.offlineai

import com.loomora.core.model.ActionItem

/**
 * Conservative fallback extraction used when no local LLM is installed or the model fails.
 * It intentionally prefers missing an ambiguous task over presenting a question, negation,
 * or generic recommendation as a confirmed action item.
 */
internal object HeuristicActionItemExtractor {
    fun isActionable(text: String): Boolean {
        val cleaned = normalizeWhitespace(text)
        val wordCount = cleaned.split(Regex("\\s+")).count(String::isNotBlank)
        if (wordCount !in 3..55) return false
        if (cleaned.endsWith("?") || questionPrefixRegex.containsMatchIn(cleaned)) return false
        if (negatedActionRegex.containsMatchIn(cleaned)) return false
        if (!concreteActionVerbRegex.containsMatchIn(cleaned)) return false
        return assignmentRegex.containsMatchIn(cleaned) ||
            commitmentRegex.containsMatchIn(cleaned) ||
            explicitTaskPrefixRegex.containsMatchIn(cleaned) ||
            imperativeActionRegex.containsMatchIn(cleaned)
    }

    fun extract(text: String, evidenceSegmentIds: List<String>): ActionItem? {
        if (!isActionable(text)) return null
        val task = normalizeTask(text)
        if (task.split(Regex("\\s+")).count(String::isNotBlank) < 3) return null
        return ActionItem(
            task = task,
            assignee = extractAssignee(text),
            dueDate = extractDueDate(text),
            evidenceSegmentIds = evidenceSegmentIds.filter(String::isNotBlank).distinct()
        )
    }

    private fun normalizeTask(text: String): String {
        val withoutPrefix = normalizeWhitespace(text)
            .replace(explicitTaskPrefixRegex, "")
            .replace(pronounCommitmentPrefixRegex, "")
            .trim(' ', '-', ':', ';', '.', ',')
        return withoutPrefix.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase() else first.toString()
        }
    }

    private fun extractAssignee(text: String): String? {
        val match = assigneePatterns.firstNotNullOfOrNull { it.find(text) } ?: return null
        val candidate = match.groupValues.getOrNull(1)
            ?.trim(' ', ',', '.', ':', ';')
            ?.replace(Regex("\\s+"), " ")
            ?.takeIf(String::isNotBlank)
            ?: return null
        if (candidate.lowercase() in nonPersonLabels) return null
        return candidate
    }

    private fun extractDueDate(text: String): String? {
        return dateRegex.find(text)?.value
            ?: relativeDueDateRegex.find(text)?.value
            ?: duePhraseRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun normalizeWhitespace(text: String): String = text.replace(Regex("\\s+"), " ").trim()

    private val personToken = """[\p{L}][\p{L}\p{N}_-]*"""
    private val namePattern = """$personToken(?:\s+$personToken){0,2}?"""
    private val actionVerbStart = """(?:gửi|hoàn\s+thành|kiểm\s+tra|cập\s+nhật|chuẩn\s+bị|triển\s+khai|sửa|tạo|viết|liên\s+hệ|gọi|rà\s+soát|xác\s+nhận|nộp|phát\s+hành|lên\s+lịch|theo\s+dõi|send|finish|complete|review|update|prepare|implement|fix|create|write|contact|call|verify|submit|publish|schedule|follow\s+up)"""
    private val assigneePatterns = listOf(
        Regex("""(?:giao(?:\s+việc)?\s+cho|phân\s+công(?:\s+cho)?|nhờ)\s+($namePattern)(?=\s+$actionVerbStart)""", RegexOption.IGNORE_CASE),
        Regex("""(?:assigned\s+to|ask(?:ed)?|owner\s*:)\s+($namePattern)(?=\s+(?:to\s+)?$actionVerbStart)""", RegexOption.IGNORE_CASE),
        Regex("""\b($namePattern)\s+(?:sẽ\s+phụ\s+trách|chịu\s+trách\s+nhiệm|phụ\s+trách|will\s+$actionVerbStart)\b""", RegexOption.IGNORE_CASE)
    )
    private val nonPersonLabels = setOf(
        "tôi", "mình", "chúng ta", "chúng tôi", "bạn", "mọi người", "team", "nhóm",
        "i", "we", "you", "they", "everyone", "the team"
    )

    private val explicitTaskPrefixRegex = Regex(
        "^(?:action\\s*item|todo|task|việc\\s+cần\\s+làm|công\\s+việc)\\s*[:\\-]\\s*",
        RegexOption.IGNORE_CASE
    )
    private val pronounCommitmentPrefixRegex = Regex(
        "^(?:(?:chúng\\s+ta|chúng\\s+tôi|tôi|mình)\\s+(?:cần|phải|sẽ)\\s+|(?:we|i)\\s+(?:need\\s+to|have\\s+to|will)\\s+)",
        RegexOption.IGNORE_CASE
    )
    private val assignmentRegex = Regex(
        "\\b(?:giao(?:\\s+việc)?\\s+cho|phân\\s+công|nhờ|phụ\\s+trách|chịu\\s+trách\\s+nhiệm|assign(?:ed)?\\s+to|owner\\s*:|ask(?:ed)?)\\b",
        RegexOption.IGNORE_CASE
    )
    private val commitmentRegex = Regex(
        "\\b(?:tôi|mình|chúng\\s+ta|chúng\\s+tôi|[\\p{L}][\\p{L}\\p{N}_-]+)\\s+(?:sẽ|cần|phải)\\s+|\\b(?:i|we|[A-Z][a-z]+)\\s+(?:will|need\\s+to|have\\s+to)\\s+",
        RegexOption.IGNORE_CASE
    )
    private val imperativeActionRegex = Regex(
        "^(?:(?:hãy|vui\\s+lòng|please)\\s+)?(?:gửi|hoàn\\s+thành|kiểm\\s+tra|cập\\s+nhật|chuẩn\\s+bị|triển\\s+khai|sửa|tạo|viết|liên\\s+hệ|gọi|rà\\s+soát|xác\\s+nhận|nộp|phát\\s+hành|send|finish|complete|review|update|prepare|implement|fix|create|write|contact|call|verify|submit|publish|schedule|follow\\s+up)\\b",
        RegexOption.IGNORE_CASE
    )
    private val concreteActionVerbRegex = Regex(
        "\\b(?:gửi|hoàn\\s+thành|kiểm\\s+tra|cập\\s+nhật|chuẩn\\s+bị|triển\\s+khai|sửa|tạo|viết|liên\\s+hệ|gọi|rà\\s+soát|xác\\s+nhận|nộp|phát\\s+hành|lên\\s+lịch|theo\\s+dõi|send|finish|complete|review|update|prepare|implement|fix|create|write|contact|call|verify|submit|publish|schedule|follow\\s+up)\\b",
        RegexOption.IGNORE_CASE
    )
    private val negatedActionRegex = Regex(
        "\\b(?:không|chưa)\\s+(?:cần|phải|nên|sẽ|gửi|làm|triển\\s+khai|hoàn\\s+thành)|\\b(?:do\\s+not|don't|doesn't|didn't|no\\s+need\\s+to|not\\s+going\\s+to)\\b",
        RegexOption.IGNORE_CASE
    )
    private val questionPrefixRegex = Regex(
        "^(?:ai|khi\\s+nào|bao\\s+giờ|tại\\s+sao|vì\\s+sao|liệu|who|when|why|how|should\\s+we|can\\s+we|could\\s+we)\\b",
        RegexOption.IGNORE_CASE
    )
    private val dateRegex = Regex(
        "\\b(?:\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?)\\b"
    )
    private val relativeDueDateRegex = Regex(
        "\\b(?:hôm\\s+nay|ngày\\s+mai|cuối\\s+tuần(?:\\s+này)?|đầu\\s+tuần\\s+sau|tuần\\s+sau|cuối\\s+tháng(?:\\s+này)?|thứ\\s+hai|thứ\\s+ba|thứ\\s+tư|thứ\\s+năm|thứ\\s+sáu|thứ\\s+bảy|chủ\\s+nhật|today|tomorrow|this\\s+weekend|end\\s+of\\s+(?:the\\s+)?week|next\\s+week|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b",
        RegexOption.IGNORE_CASE
    )
    private val duePhraseRegex = Regex(
        "(?:trước|hạn|đến|by|before|due)\\s+([^,.;]{2,40})",
        RegexOption.IGNORE_CASE
    )
}
