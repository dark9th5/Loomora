# Loomora Design System

## Design direction

Calm, trustworthy and focused. Audio is the hero; the UI should not resemble a generic admin dashboard or neon AI template.

## Brand attributes

- Clear.
- Quietly premium.
- Private.
- Dependable.
- Human.
- Modern without novelty effects.

## Color strategy

Use semantic tokens, not raw colors in feature code.

Recommended seed direction:
- Primary: deep indigo/blue-violet that remains legible in dark mode.
- Secondary: cool teal for audio/processing accents.
- Neutral surfaces: slightly warm/blue-neutral rather than pure gray.
- Recording: semantic red reserved for active recording/destructive confirmation.
- Success: green only for actual success.
- Warning: amber.
- Error: accessible red.

Required token families:
- `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer`
- `surface`, `surfaceContainer*`, `onSurface`, `onSurfaceVariant`
- `outline`, `outlineVariant`
- `recording`, `onRecording`
- `success`, `warning`, `error`
- `waveformActive`, `waveformInactive`

Do not hard-code alpha tricks repeatedly. Create tokens.

## Typography

Use a highly readable system-compatible sans serif. Prefer Android system/Roboto unless a licensed bundled brand font is deliberately selected.

Roles:
- Display: recording timer only.
- Headline: page titles.
- Title: recording titles and cards.
- Body: transcript and descriptions.
- Label: buttons, chips and metadata.

Rules:
- Transcript body prioritizes readability over density.
- Timer uses tabular digits if available.
- Minimum important text contrast meets accessibility requirements.
- Avoid all-caps except very small status badges.

## Spacing

Base grid: 4dp.

Common values:
- 4, 8, 12, 16, 20, 24, 32, 40.
- Screen horizontal padding: 16dp compact, 24dp medium, max-width layouts on large screens.
- Card inner padding: 16dp.
- Section gap: 24dp.
- Minimum touch target: 48dp.

## Shape

- Small controls/chips: 10–12dp.
- Cards/sheets: 16–24dp depending on hierarchy.
- Primary record button: circular.
- Avoid making every container a rounded card.

## Elevation

Prefer tonal surfaces and borders. Use elevation sparingly:
- floating recorder button;
- modal/sheet;
- active mini-player.

## Components

Required reusable components:
- `LoomoraTopBar`
- `PrimaryRecordButton`
- `RecorderStatusPill`
- `AudioWaveform`
- `RecordingListItem`
- `PlaybackControls`
- `EmptyState`
- `ErrorState`
- `OfflineBanner`
- `ProcessingCard`
- `TrialUsageChip`
- `ProBadge`
- `ConfirmActionSheet`
- `PermissionRationale`
- `SettingRow`

## Motion

- Motion communicates state, not decoration.
- Recorder transition must be immediate and reassuring.
- Waveform motion must reflect real amplitude.
- Respect reduced-motion settings where available.
- Avoid infinite shimmer after a terminal error.
- Haptics for record start, marker, pause/resume and stop confirmation.

## UI review questions

- Is the main action obvious in under two seconds?
- Does the screen still work with no data?
- Are all visible controls functional?
- Can a user distinguish recording, paused and finalizing without color alone?
- Does dark mode feel designed rather than inverted?
- Does the screen fit at 360dp and 200% font scale?
