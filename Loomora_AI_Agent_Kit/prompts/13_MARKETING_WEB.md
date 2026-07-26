# 13 — Loomora Marketing Website

## Use when
When app screenshots and purchase flow are ready.

## Agent prompt

```text
Implement/rebrand the marketing website to Loomora using docs/16_MARKETING_WEB.md and docs/23_COPYWRITING_LOCALIZATION.md.

Required:
- Loomora branding;
- Home, Features, Pricing, Download, Blog, Buy Pro, Contact, Privacy, Terms and Data Deletion;
- responsive light/dark;
- real app screenshots only;
- environment variables for APK/Play URL, version, checksum and support;
- clear Free/Trial/Pro comparison;
- manual purchase/activation flow;
- secure forms and no client-side secrets;
- sitemap/robots/metadata;
- production build verification.

Do not deploy unless the user explicitly authorizes deployment and credentials/tools are available.
Report exact placeholders still requiring owner values.
```

## Stop condition
Stop after production build or exact environment blocker.
