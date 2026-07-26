# Analytics Events

Analytics is optional and privacy-minimized. Never include recording title, transcript, audio, task text or license code.

## Core funnel

- `app_open`
- `onboarding_completed`
- `record_permission_result`
- `recording_started`
- `recording_paused`
- `recording_resumed`
- `recording_completed`
- `recording_failed` with category
- `recording_recovered`
- `playback_started`
- `edit_export_completed`
- `smart_insights_requested`
- `smart_insights_completed`
- `smart_insights_failed` with category
- `paywall_viewed`
- `trial_started`
- `activation_started`
- `activation_completed`
- `activation_failed`

## Properties

Allowed:
- app version;
- Android API bucket;
- device performance bucket;
- duration bucket;
- file-size bucket;
- feature source;
- error category;
- plan state;
- network type category.

Not allowed:
- content;
- exact names;
- exact recording timestamps that expose behavior unnecessarily;
- persistent invasive device fingerprint.

## Consent and opt-out

Follow the selected analytics/privacy policy and platform requirements. App functionality must not depend on analytics.
