package com.loomora.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class TagEntity(
    @PrimaryKey
    val id: String,
    val name: String
)

@Entity(
    tableName = "recording_tag_cross_ref",
    primaryKeys = ["recordingId", "tagId"],
    indices = [
        Index("recordingId"),
        Index("tagId")
    ]
)
data class RecordingTagCrossRef(
    val recordingId: String,
    val tagId: String
)
