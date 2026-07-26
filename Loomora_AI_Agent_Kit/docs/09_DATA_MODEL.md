# Data Model

Room is the source of truth for app metadata. Audio bytes remain in files/object storage, not database blobs.

## Core entities

### RecordingEntity
- id
- title
- createdAt
- updatedAt
- durationMs
- status
- originalFileUri/path
- editedOutputUri/path nullable
- mimeType
- sampleRate
- channels
- bitrate
- sizeBytes
- languageHint
- isFavorite
- deletedAt nullable
- recoveryState
- transcriptStatus
- insightStatus

### AudioSegmentEntity
- id
- recordingId
- orderIndex
- startOffsetMs
- durationMs
- file path
- sizeBytes
- checksum
- finalized
- createdAt

### MarkerEntity
- id
- recordingId
- timeMs
- label
- createdAt

### TagEntity / RecordingTagCrossRef

### TranscriptSegmentEntity
- id
- recordingId
- speakerId nullable
- startMs
- endMs
- text
- confidence nullable
- final
- source/provider version

### SpeakerEntity
- id
- recordingId
- label
- displayName nullable

### InsightEntity
- id
- recordingId
- type
- text/content JSON
- confidence nullable
- status
- model/provider metadata
- createdAt

### EvidenceLinkEntity
- insightId
- transcriptSegmentId
- startMs
- endMs

### ActionTaskEntity
- id
- recordingId
- title
- assignee nullable
- dueDate nullable
- completion state
- evidence link

### EditRecipeEntity
- id
- recordingId
- version
- operations JSON or normalized operations
- createdAt
- updatedAt

### BackgroundJobEntity
- id
- recordingId nullable
- type
- state
- progress
- attempt
- errorCode nullable
- idempotencyKey
- timestamps

### EntitlementEntity
- plan
- signedToken
- validUntil
- offlineGraceUntil
- lastVerifiedAt
- source
- status

### TrialUsageEntity
- capability
- successfulUses
- reservedOperationId nullable
- lastUpdatedAt

## Migrations

- Every schema change requires a real migration test.
- Never use destructive migration in production for user recordings.
- Export schema files to version control.
- Test upgrades from every supported production schema version.
- Back up/restore metadata where appropriate.

## Data deletion

Trash is soft delete. Permanent delete:
- cancels jobs;
- deletes derived local files;
- deletes or requests deletion of remote artifacts;
- deletes database rows in a transaction;
- reports partial remote failure honestly.
