# Monetization, Trial and License

## Commercial model

- Loomora Free: reliable local recorder and organizer.
- Loomora Trial: limited successful uses of premium capabilities.
- Loomora Pro: advanced enhancement, transcript, insights, export and optional cloud.
- Loomora Business: later team/admin capabilities.

## Purchase path for early stage

```text
App paywall
→ Open Loomora marketing site
→ Pricing / Buy Pro
→ payment or contact flow
→ account/license issued
→ Activate in app
```

The first commercial release may use manual fulfillment, but entitlement verification must still be secure and auditable.

## Activation model

Recommended:
- user enters email + license code or signs in;
- backend validates;
- backend returns signed entitlement with plan, capabilities and expiry;
- app verifies signature and stores protected token;
- backend can revoke at next verification;
- app supports offline validity window.

Never use a plain local boolean such as `isPro=true`.

## Capability-based entitlements

Prefer:
```text
record.local
edit.basic
enhance.advanced
transcript.cloud
insights.smart
export.transcript
sync.cloud
```

This allows plan evolution without app-wide conditionals.

## Trial accounting

- Operation has unique ID.
- Reserve use before starting.
- Mark consumed only after usable success.
- Retry same idempotency key does not consume twice.
- User sees remaining count.
- Trial exhaustion never blocks Free.
- Avoid invasive anti-abuse fingerprinting.

## Paywall ethics

- No fake countdown.
- No preselected expensive plan without clarity.
- Explain recurring billing if used.
- Clear restore/activation.
- Clear contact/support.
- Back/close remains available.
