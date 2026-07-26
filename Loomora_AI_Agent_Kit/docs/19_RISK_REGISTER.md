# Risk Register

| ID | Risk | Probability | Impact | Mitigation | Trigger |
|---|---|---:|---:|---|---|
| R1 | Long recording corrupts or loses data | Medium | Critical | Segments, atomic finalization, recovery tests | Unplayable/zero-length output |
| R2 | OEM kills service | High | High | Foreground service, truthful recovery, device matrix | State/file mismatch |
| R3 | Audio route behavior differs by device | High | Medium | Detect/test routes, clear UI | Bluetooth/wired disconnect |
| R4 | Storage full during recording | Medium | Critical | Preflight, monitoring, checkpoints | Write failure/low bytes |
| R5 | AI provider cost exceeds revenue | Medium | High | Quotas, provider abstraction, metrics | Cost per active Pro rises |
| R6 | AI hallucinates tasks/decisions | Medium | High | Evidence links, null unknowns, editability | Unsupported statement |
| R7 | Trial/license easy to tamper | Medium | Medium | Signed entitlement, backend counters | Patched APK/reinstall abuse |
| R8 | Login/Pro failure blocks local files | Low | Critical | Local ownership invariant | Entitlement error on Home |
| R9 | UI agent creates generic/inconsistent design | High | Medium | Design tokens, screenshot gate, UI audit prompt | Raw colors/spacing proliferation |
| R10 | Agent claims build without running | High | High | Evidence policy | “Should compile” wording |
| R11 | Database migration loses metadata | Medium | Critical | Exported schema, migration tests | Upgrade crash/missing rows |
| R12 | Direct APK undermines trust/security | Medium | High | Signed release, checksum, transparent guide | Debug/unsigned APK |
| R13 | Privacy disclosure mismatches behavior | Medium | Critical | Data-flow inventory and release review | Undocumented upload/retention |
| R14 | Over-modularization slows development | Medium | Medium | Module criteria | Empty modules/complex cycles |
| R15 | Monolithic app tangles recorder/UI | Medium | High | Core audio boundary | Composable controls recorder directly |
