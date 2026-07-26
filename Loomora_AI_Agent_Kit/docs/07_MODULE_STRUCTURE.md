# Module Structure

Start with enough separation to protect audio and data logic without creating dozens of empty modules.

## Recommended initial modules

```text
:app
:core:common
:core:model
:core:designsystem
:core:database
:core:datastore
:core:audio
:core:network
:core:testing
:feature:onboarding
:feature:home
:feature:recorder
:feature:library
:feature:recordingdetail
:feature:editor
:feature:settings
:feature:subscription
```

Add later only when real:
```text
:feature:transcript
:feature:insights
:feature:tasks
:core:ai
:core:license
```

## Dependency direction

- Feature modules may depend on core modules.
- Core modules never depend on features.
- Feature-to-feature dependencies are avoided; share contracts through core/domain boundaries.
- `:app` wires navigation and DI.
- `:core:audio` has no dependency on Compose UI.
- `:core:database` does not know network DTOs.
- `:core:model` stays Android-light where practical.

## Package rules

Feature package:
```text
feature/
  data/       only when feature-specific
  domain/     meaningful rules
  ui/
    components/
    navigation/
    Screen.kt
    ViewModel.kt
    UiState.kt
    UiAction.kt
```

Do not copy this structure blindly if a folder would remain empty.
