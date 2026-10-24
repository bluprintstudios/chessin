# Release Readiness Audit — October 19, 2026

## Scope and current result

This is a source, configuration, documentation, and recorded-evidence audit. It did not publish an app, deploy a Worker, create a Play Console app, create billing products, or submit a Play declaration.

**Result: not ready for public release.** The app is a safe development build: AI is fail-closed, new checkout is disabled, and the Worker configuration starts with AI disabled. This prevents an accidental paid or AI-enabled release, but it also means the paid AI product is not launchable yet.

## Evidence that is ready

- The current debug app assembled on October 19, 2026.
- Imports are PGN-only and local; there is no game-site account integration.
- Android backup was explicitly disabled on October 19. `allowBackup` is false and deny rules cover cloud backup and device transfer, keeping games, analysis, and preferences on the current device by default.
- A local-data deletion confirmation is implemented and was inspected on the physical Android 16 device without deleting data.
- The Worker has request-size/schema checks, redacted logging, entitlement verification code, a timeout path, and a disabled-by-default configuration.
- Draft privacy, Play Data Safety, store copy, and owner-publication documents exist in this directory.

## Public-release blockers

### Owner and external-account decisions

1. **Google Play registration and product setup:** the developer account, app record, and `premium_lifetime` one-time product do not exist yet. The app must not enable checkout before its final localized Play price and all required disclosures are available.
2. **Publisher identity:** choose the final app name after a trademark/originality review; supply a publisher name, public support email, and hosted privacy-policy URL.
3. **AI terms and budget:** select and approve the production provider/model, retention and model-improvement terms, monthly fair-use allowance, spend limit, and quota-exhaustion behavior. The present drafts intentionally leave these unresolved.
4. **Rights and licensing:** complete the commercial review for any marketed PGN-source workflow, each bundled piece-set attribution/license, and the Stockfish GPL distribution package (exact source/build/NNUE pointer and Android installation information).
5. **Figma handoff:** final visual reconciliation cannot proceed until the local Figma file reconnects and frame access is restored. See `../FIGMA_RECONCILIATION_BLOCKER.md`.

### Deployment and security

1. No production Cloudflare Worker is configured or deployed. `AI_SERVICE_ENABLED` is `false`, the Android endpoint is a `.local` placeholder, and required Worker secrets have not been provisioned.
2. Google Play purchase verification cannot go live until a least-privilege Android Publisher service account is added directly to Worker secret storage. Never place its JSON or an AI API key in the Android app, repository, logs, or chat.
3. The Android release has no Play signing configuration or recorded signed-install/internal-test evidence. It remains at application id `com.pro.chess_review`, version code `1`, version name `1.0`; confirm these are final before creating the Play app record.

### Remaining technical verification

1. The Coach instrumentation tests are skipped only on Android 16 / API 36 because of the upstream coroutine ServiceLoader failure; retain their Android 17 evidence and re-enable/re-run after upstream remediation.
2. Complete physical accessibility spoken-navigation testing, clean-install/upgrade testing, offline/poor-network/throttling tests, broader engine ABI/device coverage, and final release-build validation.
3. Recheck all current external terms, policies, SDK requirements, quotas, and Play declarations immediately before submission. Drafts are not approval evidence.

## Recommended handoff order

1. When affordable, create the Play developer account and app record; do not enable sales yet.
2. Provide the final name, publisher/support information, and product decision inputs above.
3. Restore Figma frame access and reconcile UI before treating any visual state as final.
4. Configure the Worker secrets and billing service account only after the Play product and AI policy decisions are finalized.
5. Enable checkout and AI only after their disclosures, server verification, budget guardrails, signed build, and internal-testing evidence are complete.
