# UX and Screen Specification

## Navigation model

Primary bottom navigation:

- **Home**
- **Library**
- **Tasks** (visible when Smart Insights exists; may be introduced after MVP)
- **Settings**

A prominent Record action is available from Home and Library. Do not overload bottom navigation with the editor, paywall or profile.

## Global UX states

Every data screen defines:

- Loading.
- Content.
- Empty.
- Recoverable error.
- Blocking error.
- Offline variant when relevant.
- Permission-denied variant when relevant.

## Home

### Purpose
Start recording immediately and surface recent value.

### Structure
- Compact top bar with Loomora wordmark and settings/avatar entry.
- Primary “New recording” action.
- Optional recording mode chips: Meeting, Interview, Lecture, Voice note.
- Recent recordings section.
- Unfinished/recoverable recordings card when present.
- Trial/Pro usage shown subtly, never as a dominating ad.

### Empty state
A calm illustration/icon, one sentence, and Record CTA. No fake recent items.

## Recorder

### Visual hierarchy
1. Recording status and title.
2. Timer.
3. Real waveform/level visualization.
4. Live transcript region only if truly available.
5. Marker and secondary controls.
6. Large pause/resume and stop controls.

### States
- Preparing.
- Recording.
- Paused.
- Finalizing.
- Saved.
- Recoverable failure.
- Fatal failure.

### Safety
- Stop requires deliberate input.
- Back gesture during recording does not silently stop.
- Screen remains readable in bright and dark conditions.
- Notification controls mirror valid actions.
- Timer comes from recorder timestamps, not a standalone UI timer.

## Library

- Search.
- Filter by date, duration, favorite, tag and processing state.
- Sort by newest, oldest, title and duration.
- List/card density adapts to device width.
- Each item shows title, date, duration, status and optional tags.
- Swipe actions must have undo.
- Multi-select is introduced only when implemented completely.

## Recording detail

Tabs or sections:
- Overview.
- Transcript.
- Audio.
- Tasks.

Overview:
- editable title;
- date/duration/source;
- summary if available;
- key points and decisions;
- markers;
- processing/error status.

Audio:
- waveform;
- playback controls;
- speed;
- marker list;
- edit and enhance actions.

Transcript:
- speaker/time;
- search;
- edit;
- tap to seek;
- clear distinction between provisional and final text.

Tasks:
- checkbox;
- title;
- assignee and due date only when supported by evidence;
- evidence link.

## Editor

- Non-destructive timeline.
- Handles with accessible alternatives to drag gestures.
- Zoom.
- Play selection.
- Trim, split, delete range.
- Undo/redo.
- Before/after preview for enhancement.
- Explicit “Save edits” and “Export copy”.
- Never overwrite original without a separate, explicit action.

## Paywall

- Explain concrete benefits.
- Show remaining trial first.
- Make Free continuation obvious.
- Avoid false urgency.
- Show internet requirement for cloud features.
- Provide Restore/Activate and Contact support.
- Do not block the back button.

## Settings

Sections:
- Appearance.
- Language.
- Recording quality.
- Storage.
- Playback.
- Smart features and privacy.
- Trial/Pro/activation.
- Export defaults.
- About, privacy, terms and delete data.

## Responsive requirements

- Compact portrait: primary target.
- Landscape recorder: usable, not stretched.
- Large phone/foldable: center content with max width and use extra space for detail.
- Font scaling up to 200% must preserve access to controls.
- Avoid hard-coded heights for text containers.
