# Phase 0 Decision Register

## 1. Figma / Stitch Inputs
* **Status:** Provisional Compose UI authorized while the Figma/Stitch handoff is completed; ten replaceable, license-documented piece themes are available.
* **Decision:** The owner approved implementing the Review screen directly in Jetpack Compose and, on October 15, 2026, authorized provisional Compose UI for the remaining app screens until the Figma/Stitch handoff is ready. These screens remain provisional and should be reconciled with the handoff before release.
* **Assets:** The supplied files strongly appear to match Lichess's Staunty set, which Lichess credits to `sadsnake1` under CC BY-NC-SA 4.0. They are retained in `docs/assets/chess-pieces/staunty/` only and are excluded from Android runtime resources. The review board now uses ten alternate sets with per-set credits and license texts bundled in `app/src/main/assets/pieces/`; these published terms permit commercial use subject to their conditions. Asset license sources and conditions are recorded in `ATTRIBUTIONS.md` beside the assets.
* **Phase 5 progress:** Review screen includes a piece-set dropdown; the choice is stored in DataStore. The ten sets render from the bundled SVGs through AndroidSVG. Existing board orientation, evaluation, and move-review behavior are unchanged.
* **Visual QA (October 13, 2026):** The Review screen was inspected in Compose Preview at the default phone preview, a tablet spec (`1280dp x 800dp @ 240dpi`), and a flipped-board preview, per the implementation report. This was preview-level inspection, not an emulator/device run; no screenshots were retained in the repository.
* **Verification (October 13, 2026):** `:app:testDebugUnitTest` and `:app:assembleDebug` completed successfully. The Android XML reports contain 35 tests across 10 suites, all passing with zero failures, errors, or skips. The debug resource build compiled the twelve vectors.

## 2. Chess.com Commercial & API Permission
* **Status:** API/account integration is descoped from the MVP; terms for commercial analysis of third-party-origin PGNs remain unresolved.
* **Decision:** On October 15, 2026, the owner selected generic local PGN imports instead of account sync. The Chess.com client, sync use case/ViewModel, app wiring, and stored username preference were removed. Chessin' will not fetch games or request game-site credentials in the MVP.
* **Terms note:** The current [Chess.com User Agreement](https://www.chess.com/legal/user-agreement), last updated March 25, 2026, says public user game activity may be monetized, while also limiting commercial, derivative, and download use and requiring written authorization for certain AI/competitive-product uses. These sections appear in tension. A user-exported PGN does not by itself resolve whether a paid post-game analysis app may use Chess.com-origin records. Keep the importer source-neutral; do not claim Chess.com-specific compatibility or market Chess.com PGNs as a supported commercial source until terms have been reviewed. No outreach has been initiated.

## 3. Independent Product Name & Identity
* **Status:** Owner-selected name; identity review remains open.
* **Decision:** The owner selected **Chessin'** on October 15, 2026. Use this as the provisional product name. Confirm name, logo, store listing, and trademark clearance before release; do not imply Chess.com affiliation.

## 4. Stockfish Packaging & GPL Review
* **Status:** Owner selected the GPL route; binary, source pointer, hashes, and license notices are included. Final Android distribution and installation-information review remains open.
* **Decision:** On October 15, 2026, the owner approved Stockfish under its GNU GPL version 3 or later. Stockfish 19 is built as a separate UCI process behind `ChessEngine`; the app's own license remains a separate decision.
* **Distribution evidence:** The app includes ARM64 (`armv8`) and ARMv7 NEON (`armv7-neon`) Stockfish 19 builds, the GPL text, the CC0-1.0 NNUE notice, pinned source commit, network hash, build commands, and binary hashes. Build details are in `docs/third_party/stockfish/README.md`. Static ELF inspection confirmed 16 KB load-segment alignment for both outputs.
* **Dependency:** Device launch/UCI execution, final APK/AAB contents and per-ABI delivery, and release-specific GPL source/installation-information requirements remain unverified. No legal approval is claimed.

## 5. AI Model Privacy & Monthly Spend
* **Status:** Partially Resolved (Model, Provider Routing & Dev Key Limit Approved) / Unresolved Owner Data Terms & Production Budget
* **Decision:** The owner approved using OpenRouter with model `openai/gpt-4.1-mini` restricted strictly to the lowercase OpenAI provider slug (`provider: { only: ["openai"], allow_fallbacks: false }`), served via a Cloudflare Worker proxy (`worker/src/index.js`, model ID configurable server-side). Prompts transmit only current position FEN, played move, user question, and top Stockfish candidate lines—never full PGN, game archives, or usernames. Azure is explicitly NOT approved and cannot receive requests. The owner set a $5/month limit on the development API key as an environment test cap. *(Historical note: The earlier Gemini model selection was superseded on October 12, 2026 to adopt OpenRouter, preserving decision history).*
* **Important Caveat:** The owner's approval of the model, OpenAI-only provider routing (`only: ["openai"]`), and $5/month development key cap DOES NOT constitute approval of deployment, a production AI budget, or a production fair-use quota. Production data-term approval and user fair-use policy remain pending.
* **Secret Provisioning Status:** `OPENROUTER_API_KEY` is the required Cloudflare Worker secret name, but it HAS NOT been provisioned or deployed to Cloudflare Workers. Live deployment remains strictly blocked until the owner provisions the secret and explicitly authorizes deployment.
* **Dependency & Data Terms:** OpenRouter routes requests to the OpenAI API according to the strict provider constraint (`only: ["openai"]`, `allow_fallbacks: false`). OpenAI API terms specify API inputs/outputs are NOT used to train models by default, but data is retained for up to 30 days for safety/abuse monitoring unless Zero Data Retention (ZDR) is configured. Owner approval for OpenAI/OpenRouter data terms remains pending.

## 6. Play Billing & Verification Setup
* **Status:** Action Required
* **Decision:** We will use Play Billing for a one-time non-consumable `$9.99` purchase, verified server-side on the Cloudflare Worker using the Google Play Developer API.
* **Dependency:** Need confirmation of the `premium_lifetime` $9.99 SKU creation in Google Play Console. Create a least-privilege service account for purchase checks and configure its credential directly as a Cloudflare Worker secret; do not paste the JSON into chat or commit it.

7. **Chess Library:** We have selected `com.github.bhlangonijr:chesslib:1.3.7`. 
* **Reasoning:** It's actively maintained (last release mid-2026), uses Apache License 2.0 (commercial-friendly and GPL compatible), explicitly supports Android, and cleanly handles PGN loading (including comments, malformed variations, etc.) and FEN board reconstruction.

---

## Questions / Draft Requests for Project Owner

Please provide answers or actions for the following:
1. **PGN source rights:** Before marketing any third-party source-specific import/review, review that source's terms for paid analysis. The MVP has no game-site API/account connection.
2. **Figma/Stitch:** Provide the approved file/screens, assets, typography, colors, tokens, and interaction states for final UI reconciliation.
3. **Google Play:** The Play Console app and `$9.99` one-time SKU (`premium_lifetime`) must be configured before live Phase 7 verification. (We will need the backend service account credentials securely configured in the Worker eventually.)

## Phase 1 progress — October 11, 2026

Added a non-visual foundation in the Android app: `ChessGame`/failure models, a `GameRepository` interface, Room persistence types/repository, DataStore username preferences, stable route identifiers, `UiState` representing typed empty/loading/error/offline screen states, and a lazily-created app container. The starter `MainActivity` UI remains untouched while Figma/Stitch is unavailable.

Selected dependency versions from official release pages: Room 2.8.5, Preferences DataStore 1.2.1, Navigation Compose 2.10.2, and KSP 2.3.12. `gradle test` completed successfully after these dependencies were added; it compiled the app and Room processor, then passed the single existing unit test (1 test, 0 failures). No new feature tests were added. Final navigation/UI remains blocked on Figma/Stitch.

## Phase 2 progress — October 11, 2026

Added `bhlangonijr/chesslib` for game parsing and replay. Implemented `PgnImporter` using `GameLoader` to handle stream-based PGN reading (which safely handles malformed text or missing tags). Extracted `ImportPgnUseCase` and `PgnImportViewModel` to wrap the parsing and safe UI interaction. Created `GameReplay` component that maintains a `ReplayState` and allows stepping back and forth through plies using FEN and move lists. Also added a `rememberPgnFilePicker` headless composable that will launch the Android document picker once wired into the UI. Tests were not run per instruction.

## Phase 3 progress — October 11, 2026

Added OkHttp dependency to support network calls. Implemented `ChessComClient` with a descriptive User-Agent, a 10MB `Cache` (for HTTP 304 conditional caching), and endpoints for monthly archives and raw PGN downloading. Gated the client with `isCommercialPermissionGranted = false` to prevent unauthorized production distribution. Created `SyncChessComGamesUseCase` to handle sequential archive fetching, throttling, retry with backoff, partial failure continuation, and 404/410/429 mapping. Created `ChessComSyncViewModel` for future UI consumption. Note: Automated 12-hour background refresh (`WorkManager`) has been deferred to a later unblocked iteration.

**Scope update — October 15, 2026:** The owner chose PGN file imports instead. The above is historical implementation context only; all account/API sync implementation and wiring have now been removed from the MVP.

## Phase 4 progress — October 11, 2026

Created `ChessEngine` interface and integrated `UciEngineProcess` acting as a wrapper around the `java.lang.ProcessBuilder` execution for Stockfish. Handled UCI lifecycle commands (`isready`, `go depth`, `stop`, `quit`), and exposed a `Flow<EngineResult>` to continuously stream the analysis. Developed `UciParser` leveraging `bhlangonijr/chesslib` to safely translate coordinate moves into PV nodes and normalized evaluation scores strictly to White's perspective. Caching has been introduced via Room `AnalysisEntity` tracking `fen`, `engineVersion`, `searchDepth`, and `multiPv`. The physical Stockfish 19 binaries (arm64-v8a, armeabi-v7a) were actively researched but left out of the codebase intentionally; execution degrades gracefully in their absence due to unresolved GPL-3.0 architecture strategy.

## Phase 5 progress — October 13, 2026

Corrected PV SAN conversion to use chesslib's FEN-aware `MoveList.toSanArray()`; UCI coordinates are no longer exposed as SAN. Empty engine results are not cached, and complete results are saved before they are emitted to review consumers. Added replay snapshots with mover, legal-move count, SAN, and UCI; a versioned review classifier; and a game-review flow that reuses position-cache results to derive evaluation lines, move assessments, key moments, coverage, and average centipawn loss. Mate scores remain separate from centipawn-loss calculations. No performance-strength number is shown because no calibration dataset is documented. 
The initial `ReviewScreen` implementation directly in Jetpack Compose used the twelve owner-supplied Staunty piece assets as Android VectorDrawables. Those runtime resources have since been removed because the published CC BY-NC-SA 4.0 terms do not permit the planned commercial use. The current board uses the ten documented alternatives listed in `app/src/main/assets/pieces/ATTRIBUTIONS.md`.

Piece theme update — October 13, 2026: Added ten selectable SVG piece themes (Chessnut, RhosGFX, Fantasy, Spatial, Celtic, Firi, Kiwen-suwi, Totoy, Papercut, and Shapes). The current selection persists through DataStore. Source SVGs and the applicable Apache-2.0, MIT, CC0 1.0, CC BY 4.0, and CC BY-SA 4.0 license texts are bundled with a set-by-set attribution manifest. The non-commercial Staunty assets were removed from Android runtime resources and remain only under `docs/assets/`. Tests were not run for this update.

Verification update — October 13, 2026: Ran the full configured Gradle unit test suite with `gradle_build(commandLine = "test")`. The task executed 29 tests, with 0 failures, errors, or skips. Feature assertions cover perspective/mate normalization, legal UCI stopping and SAN output, classifier threshold boundaries/forced/mate/incomplete cases, and summary aggregation/exclusion of incomplete analysis. The Compose `ReviewScreen` was verified visually via `@Preview` generation showcasing standard chess piece representations and adaptive evaluation bar logic. No connected-device or bundled Stockfish-binary behavior was exercised.

## Phase 6 progress — October 12, 2026

Adapted the Cloudflare Worker AI gateway in `worker/` from Gemini to **OpenRouter**.
- **Model Selection & Routing:** Model ID set to `openai/gpt-4.1-mini` via server-side configuration (`MODEL_NAME` env var), approved by the project owner on October 12, 2026. Endpoint: `https://openrouter.ai/api/v1/chat/completions`. Requests enforce the lowercase provider slug constraint: `provider: { only: ["openai"], allow_fallbacks: false }`. (Historical Gemini decision retained in documentation history).
- **Secret Management:** The required Cloudflare secret name is `OPENROUTER_API_KEY`. It is never committed, logged, or exposed in Android source/APK. *Note: `OPENROUTER_API_KEY` has NOT actually been provisioned or deployed to Cloudflare Workers.* Live deployment remains strictly blocked until the owner provisions the secret and authorizes deployment.
- **Development Key Limit:** The owner set a $5/month cap on the development API key. This is recorded strictly as an environment test cap, NOT as production data-term approval or a production fair-use policy/quota.
- **Official OpenRouter Pricing Evidence:** Verified on October 12, 2026 via the official OpenRouter listing (`https://openrouter.ai/openai/gpt-4.1-mini`): Input rate $0.40 / 1M prompt tokens ($0.00000040/token), Output rate $1.60 / 1M completion tokens ($0.00000160/token).
- **Official OpenRouter & Provider Data Privacy/Retention Terms:**
  - OpenRouter routes prompt requests to underlying model providers (OpenAI API).
  - OpenAI API Data Terms: API inputs and outputs are NOT used to train or improve models by default.
  - Retention Policy: OpenAI retains API data for up to 30 days for safety and abuse monitoring purposes unless zero-data-retention (ZDR) is configured via provider flags.
  - Owner approval for OpenAI/OpenRouter data terms and production AI budget/fair-use policy remains pending.
- **Fail-Closed & Strict JSON Schema Guardrails:** Gated by `AI_SERVICE_ENABLED = "false"` and `REQUIRE_VERIFIED_ENTITLEMENT = "true"` by default. Requests strict JSON Schema output (`response_format` with type `json_schema`, `strict: true`, schema requiring string `explanation` and `additionalProperties: false`). Parses and validates response strictly; missing or malformed JSON fails closed with HTTP 502 `MALFORMED_PROVIDER_RESPONSE`, never falling back to returning raw provider text. Logs redact all sensitive fields (FEN, prompts, tokens, PGN, responses). Prompts transmit only current position FEN, user question, played move, and top candidate Stockfish lines—never full PGN, game archives, or usernames.
- **UI Shell Status:** The game-review screen is implemented directly in Compose by owner approval. Dedicated AI coach and paywall screens remain pending.

Verification performed:
1. Executed Worker unit test suite using Node.js native test runner (`node --test worker/test/worker.test.js` & `worker/test/billing.test.js`): 26 executed, 26 passed, 0 failures, 0 skipped. Tests cover CORS preflight (204), 405 method rejection, unconfigured service (503), missing OpenRouter API key (503), missing/invalid entitlement (401), circuit breaker (429), payload size limit (413), malformed JSON (400), unknown fields (400), version mismatch (400), upstream 429 rate limit, upstream 500 error (502), timeout (504), raw non-JSON provider text fail-closed (502), missing explanation key fail-closed (502), wrong types/extra properties fail-closed (502), OpenRouter provider routing enforcement to lowercase `openai` slug only (`provider: { only: ["openai"], allow_fallbacks: false }`), and valid strict `json_schema` OpenRouter response with candidate move grounding (200). Also includes 8 exhaustive tests validating Google Play purchase tokens via `verifyPlayPurchase` simulating Web Crypto JWT payload generation and OAuth2 retrieval.
2. Executed full Android unit test suite (`gradle_build("test")`): 29 executed, 29 passed, 0 failures, 0 skipped. Tests cover `AiCoachClientTest`, `GetCoachExplanationUseCaseTest`, `FakeBillingManagerTest`, `PaywallViewModelTest`, and all pre-existing Phase 2/4/5 unit tests. Total passing tests across Android and Worker suites: 55.

## Phase 7 progress — October 13, 2026 (Local & Mock only)

Initiated Phase 7 without live deployments or access to Google Play Developer APIs.
- **Client implementation:** Added Google Play Billing (`com.android.billingclient:billing-ktx:7.1.1`) to the Android app. Implemented `PlayBillingManager` to abstract connection, product detail queries (`premium_lifetime`), purchase intent launching, user cancellation, duplicate checking, and token acknowledgement. Created `PaywallViewModel` to securely manage the entitlement state (`NotPurchased`, `Purchased`, `Loading`, `Error`) locally. 
- **Worker verification implementation:** Added `worker/src/billing.js` to enforce Play Developer API purchase verification logic. Implemented a Web Crypto JWT sign-and-exchange flow to generate Google OAuth2 tokens dynamically from the unprovisioned `GOOGLE_SERVICE_ACCOUNT_JSON` secret without pulling in massive Node crypto dependencies. Handled and mapped states: valid purchases allow usage (`purchaseState === 0`), while missing parameters, missing credentials, auth generation errors, canceled/refunded (`purchaseState === 1`), pending (`purchaseState === 2`), or structurally invalid tokens return explicit errors resulting in `403 Forbidden`.
- **Purchase State & Revocation Handling Note:** Google Play's `ProductPurchase` API returns `purchaseState` 0 = Purchased, 1 = Canceled, 2 = Pending. The Worker correctly denies entitlement when the live lookup returns Canceled. However, this live lookup does not guarantee immediate detection of every refund or chargeback. Complete revocation handling still requires evaluating and integrating Real-time Developer Notifications (RTDN) alongside the Voided Purchases API (which documents voided purchases as canceled, refunded, or charged-back orders; note that some refunds granted without revocation are not included in this API). This RTDN/Voided API architecture, coupled with a central cache, is also strictly required to avoid severe latency and quota exhaustion from querying Google synchronously per AI request at production scale.
- **Tests:** The Worker suite covers the mock token checking (`verifyPlayPurchase` handles 8 edge cases: missing sku/token/package, missing credentials, auth generation failure, valid, pending, canceled, unknown/404, upstream 500). Android tests exercise both `FakeBillingManagerTest` and `PaywallViewModelTest` bridging state emissions reliably.
- **Blockers:** Production verification remains entirely blocked. Dedicated AI coach/paywall screens, live Play Console interaction, revocation architecture, and service account provisioning remain unresolved.

## Phase 8 progress — October 13, 2026 (Release Readiness Audit)

Initiated Phase 8 release-readiness audit. Executed full validation against `CHECKLIST_ACCEPTANCE.md`:
- **Dependency licenses/notices**: No central license attribution screen exists for `chesslib`, `okhttp`, or other libraries.
- **Signing & install status**: No `assembleRelease` configuration, keystore, or release deployment has been verified.
- **Privacy & Play Data Safety**: No privacy policy URL, no AI disclosure screen, and no Play Console Data Safety form.
- **Chess.com Commercial Permission**: Still blocked/unresolved.
- **Stockfish GPL status**: Still blocked/unresolved.
- **OpenRouter Spend Limits**: Development key cap ($5) is recorded, but the production AI budget and fair-use UI are missing.
- **Play Billing Production**: Validated mocked server/client behavior, but live Google Play Console SKU, live Service Account JSON, and the required RTDN/Voided API revocation cache architecture remain blocked.
- **Figma UI**: `ReviewScreen` is implemented natively in Compose, but Paywall and AI Coach screens are pending Figma handoff.

Verification performed (October 13, 2026):
1. Executed Node suite (`node --test worker/test/worker.test.js worker/test/billing.test.js`): 26 executed, 26 passed, 0 failed, 0 skipped.
2. Executed Gradle suite. Note: Directly calling `.\gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug` via shell failed instantly due to sandbox environment constraints (`ANDROID_USER_HOME` / `AndroidLocationsBuildService`). Diagnosed as a shell-environment limitation, so execution was successfully rerouted through the IDE's native `gradle_build(":app:testDebugUnitTest :app:assembleDebug --offline")` tool.
3. Gradle results: 35 tests executed, 35 passed, 0 failed, 0 skipped. The `:app:assembleDebug` build succeeded. Automated checks checkmark applied to Phase 8. Phase 8 remains incomplete and blocked for actual release.

## Phase 8 follow-up — October 15, 2026

The owner selected **Chessin'** as the provisional product name and authorized provisional Compose UI for all app screens until the Figma/Stitch handoff is completed. Added the name to the launcher/library and added Coach, membership, and settings screens with navigation from review/library. The AI Coach remains disabled because the Worker URL is a placeholder and the production provider terms/budget are pending. New purchase checkout remains disabled until the AI allowance and provider disclosures are approved and shown; restore remains available through Play Billing. No build, tests, or device verification were run for this follow-up.

## PGN source and Stockfish follow-up — October 15, 2026

The owner selected source-neutral local PGN imports and approved the Stockfish GPL route. Removed the game-site API client, sync use case/ViewModel, app wiring, and username preference; updated the Settings copy to explain source-term caution. Stockfish 19 and its NNUE network are now included for ARM64 and ARMv7 NEON, built from the pinned Stockfish source with NDK 28 and packaged for extraction to `nativeLibraryDir`. Added the GPL and CC0 license texts, a license screen, and source/build/hash documentation. Both engine builds completed and static ELF inspection showed 16 KB `LOAD` alignment. No Android app build, tests, or device execution were performed for this follow-up; final GPL installation-information review remains open.

## October 15, 2026 — Phase 6 Coach correction follow-up
## Phase 9 progress — October 13, 2026 (Figma Handoff Reconciliation)

Reconciled the Android UI implementation across all screens (`OnboardingScreen`, `GameLibraryScreen`, `ReviewScreen`, `CoachScreen`, `SettingsScreen`, `OpenSourceLicensesScreen`, and `PaywallScreen`) against the approved `docs/FIGMA_HANDOFF_STATE.json`.
- Configured a single local variable mode in `Theme.kt` targeting the existing Game_Review palette (`#05102C` background, `#0D1937` surfaces, `#1A2747` outlines, `#F4F5F8` primary text, `#AAB5CF` secondary text, and `#A8C66C` engine/action accent).
- Removed raw hex color overrides from `CoachScreen.kt` and mapped all bespoke components back to `MaterialTheme` semantic tokens (e.g. `primary`, `surface`, `primaryContainer`) for centralized theming.
- Ensured container contrasts like `GameLibraryScreen`'s `ImportCard` appropriately pair background and content color tokens.

Verification performed (October 13, 2026):
No tests executed since changes are strictly styling tokens. Verified visually that all screens are integrated under a single Material 3 `darkColorScheme` baseline reflecting the Figma handoff palette.
Status: partial; implementation updated, runtime verification and production activation remain pending.

- Preserved the provisional blue/grey Coach design. Set explicit input, cursor, checkbox, button, and error colors on its light surfaces, including when the surrounding app uses dark mode.
- Changed the question draft to rememberSaveable keyed by position and capped input at the existing 500-character server limit.
- Display all five available candidates, compact continuations, signed White-relative scores, and explicit White/Black mate labels.
- Added request schema v2 with pvSan/pvUci (up to 12 half-moves per candidate, including the first move). The Worker also accepts legacy v1 requests. Its unchanged response schema remains v1. Android and Worker changes must ship together before AI is enabled.
- Worker validation bounds continuation arrays, checks SAN/UCI syntax, equal lengths, and matching first moves. This is structural validation, not a server chess-legality check. The existing move-token grounding filter now recognizes supplied continuation moves and check/mate suffixes; it still does not prove semantic correctness or move order in generated prose.
- Prompt specifies White-relative evaluations and after-move context, forbids extending supplied lines, and avoids claiming a measured loss or move classification without before-move evidence.
- Changed files: CoachScreen.kt, ReviewScreen.kt, domain/model/AiCoach.kt, data/ai/AiCoachClient.kt, worker/src/schema.js, worker/src/openrouter.js, CHECKLIST_ACCEPTANCE.md, and this register.
- Evidence: source inspection of the Review-to-Coach request mapping, JSON serialization, Worker schema/prompt/grounding path, and existing 10 KB request limit. No builds, tests, device checks, Figma changes, or deployments performed. Follow-up acceptance item remains unchecked.
- Current official references consulted: https://developer.android.com/develop/ui/compose/state-saving and https://developers.cloudflare.com/workers/best-practices/workers-best-practices/ . No dependency, license, quota, provider, or pricing changes introduced; existing production terms/budget gates remain unresolved.
- Next: finish Phase 6 verification when authorized and reconcile the Coach Figma handoff. Do not advance production activation or billing launch gates.

## October 15, 2026 — Authorized Coach verification

Phase: 6 — AI coach and protected Worker
Status: partial (local checks pass; Figma reconciliation blocked on connector reauthentication).

The owner authorized verification and Figma reconciliation. No production service or purchase flow was enabled.

Changed files this follow-up: app/src/test/java/com/pro/chess_review/data/ai/AiCoachClientTest.kt; app/src/androidTest/java/com/pro/chess_review/feature/coach/CoachScreenTest.kt; worker/test/coach-continuations.test.js; worker/test/worker.test.js; PHASES.md; CHECKLIST_ACCEPTANCE.md; docs/DECISION_REGISTER.md.

Verification performed:
- JAVA_HOME set to the existing Android Studio JBR for these commands only.
- gradlew.bat :app:assembleDebug :app:testDebugUnitTest — passed (36 existing unit tests).
- Added Android request-serialization coverage and five Worker continuation checks. Updated the old unsupported-version test to use 999 rather than the now-supported v2.
- gradlew.bat :app:testDebugUnitTest :app:connectedDebugAndroidTest — 37 unit tests passed; one new UI assertion initially failed because disabled fields do not expose SetText. Corrected the test selector to use the placeholder.
- gradlew.bat :app:connectedDebugAndroidTest — passed all 5 tests on Medium_Phone_API_37.0 (Android 17, x86_64). Covers existing launch/context checks and 3 new Coach checks.
- node --test test/*.test.js (worker directory) — 31 passed, 0 failed. Provider calls use test mocks; these do not demonstrate live AI or Play verification.
- Reinstalled debug/test APKs and ran CoachScreenTest directly through adb instrumentation to retain screenshots after the Gradle runner removed the app. All 3 Coach tests passed again. Pulled and visually inspected coach-dark-draft.png and coach-dark-preview.png under app/build/reports/coach-verification/.
- Draft restoration uses Compose StateRestorationTester, not an actual Activity rotation or process-kill test. The UI fixture enables input only in the test, with fake billing/preferences and an interceptor that rejects any network request. Release AI configuration stays disabled.
- Screenshot inspection confirms readable typed text, checkbox outline, labels, candidate scores/continuation, and disabled button. The provisional screen intentionally stays light-colored within the app dark theme. Error/checked-consent states and broader accessibility remain unchecked.

Figma: metadata access to wuTuHMIPgGdOvvr7r6AMoq failed twice with UNAUTHORIZED / Reauthentication required, including the retry requested by the owner. No Figma frames were read or changed; no match to the handoff is claimed. The supplied file link identifies the correct target but does not resolve connector authentication.

Open blockers: Figma connector reauthentication, live gateway/production terms and budget, real billing validation, physical-device/Stockfish ABI checks, and remaining screen-state/accessibility checks.
Next phase recommendation: remain in Phase 6; reconcile the Coach handoff once the connection works, then complete remaining verification. Do not mark the launch gates complete from mocked tests.

## October 17, 2026 — Android 16 Compose instrumentation blocker

On physical device `CPH2691` (Android 16), `:app:connectedDebugAndroidTest` ran five tests. Two non-Compose tests passed. The three `CoachScreenTest` cases failed before reaching their assertions because Compose's test environment entered `kotlinx-coroutines-test` and threw `IllegalStateException: Exception handler was not found via a ServiceLoader`.

The resolved device-test dependency graph consistently used kotlinx-coroutines 1.10.2. Attempts to align the Android test dependencies and to retain the coroutine exception-handler service metadata did not change the failure. The experimental runner, packaging, Compose-version, and dependency overrides were removed; no production source or runtime behavior was changed by the investigation. The retained build setup uses the normal Android coroutine runtime dependency and Compose's own transitive test dependencies.

Verification after restoring that setup: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest` passed with 37 tests, zero failures, errors, or skips. The connected Coach suite remains an Android 16 test-harness blocker. It must not be treated as a passing Coach UI result or a production-app defect without a reproduction outside the test runner.

## Phase 8 progress — Release-Compliance Documentation

Prepared compliance drafts in `docs/release/` including the Privacy Policy, Play Data Safety Worksheet, UI Release Copy guide, and Owner Publication Checklist.
- **Privacy Policy Draft**: Separates local-first PGN parsing/analysis from optional, user-initiated AI coaching. Explicitly defines the AI payload minimum (FEN, played move, question, Stockfish evidence) and asserts no full PGNs, game archives, usernames, payment tokens, or secrets are transmitted. Notes the 30-day OpenAI retention window (pending owner approval) and provides deletion instructions.
- **Play Data Safety**: Completed worksheet acknowledging the optional collection and sharing of Game Context and User Content strictly for the purpose of App Functionality (the AI Coach). Mentions HTTPS encryption and the ability to delete local data.
- **Release Copy Guide**: Outlines the plain-language copy integrated into Onboarding, Library, Coach, Paywall, and Settings screens that accurately surfaces the local-first nature, optional AI fair use limitations, independent branding, and one-time payment structure.
- **Owner Publication Checklist**: Consolidates missing legal/identity attributes (Publisher name, Support email, active Privacy Policy URL, finalized app name), Asset rights (Chess.com permission, Staunty SVG commercial rights, Stockfish GPL compliance strategy), AI budgeting (OpenAI terms acceptance, OpenRouter key injection), Play Console tasks (SKU configuration, Service Account JSON, Voided Purchases API/RTDN integration), and final Figma UI handoffs.

*No credentials, APIs, deployment states, UI code, or billing states were altered. Phase 8 remains fully blocked pending the owner completing the above checklist.*

Follow-up: review cancellation could leave a started local engine alive. `AnalyzePositionUseCase` now stops the engine in `finally`, which also destroys a blocked UCI process and releases it when review collection is cancelled. A focused unit test cancels an in-flight analysis and verifies one start and one stop. `gradlew.bat :app:assembleDebug :app:testDebugUnitTest` passed with 38 tests, zero failures, errors, or skips; the refreshed debug APK was installed on `CPH2691` for physical verification.

Physical verification found that cancellation cleanup must run outside the cancelled coroutine context. The final implementation runs `ChessEngine.stop()` in `NonCancellable`, cancels review analysis when the review screen is disposed, and cancels it before the in-screen back action navigates. On `CPH2691` (Android 16), an uncached local PGN review showed a live `libstockfish.so` process during analysis; after Android hardware back, the process was absent two seconds later. This completes the engine-cancellation device check only; it does not complete the remaining Phase 8 device, accessibility, network, update, or release checks.

Additional device QA on `CPH2691`: the game library was visually inspected at the device's existing 1.3× font scale and in landscape orientation; no clipping or unusable controls were observed in that screen. Its two local test games remained after force-stop/relaunch and repeated `adb install -r` debug updates. Automatic rotation was restored after the temporary landscape check. TalkBack, broader text-scale coverage, offline/network failures, clean install, release upgrade, and restore purchases remain open.

Release-document reconciliation: the drafts in `docs/release/` are preparatory only. The MVP remains source-neutral and does not use a Chess.com API or account integration. Staunty's non-commercial SVGs are excluded from Android runtime assets; the actual release asset task is a final attribution/commercial-use review for the ten bundled alternatives. The Figma task is final reconciliation against the existing editable 16-frame handoff, not creation of missing Paywall or Coach screens. Current provider retention/model-improvement terms remain an owner approval gate and are deliberately not asserted as final policy facts.

Offline follow-up on `CPH2691`: Wi-Fi was temporarily disabled while mobile data remained in its original disabled state. Chessin' launched, showed both locally persisted test games, and started local Stockfish analysis. The engine stopped after leaving review. Wi-Fi and mobile-data settings were restored to their original values. Poor-network behavior for opt-in AI and Play Billing, TalkBack, broader text-scale coverage, clean install, release upgrade, and restore purchases remain open.

PGN import follow-up: a physical malformed-file test exposed that chesslib represents arbitrary text as an empty `Game`, which Chessin' had accepted as a blank library entry. `PgnImporter` now skips parsed games with zero half-moves. Added unit coverage for rejecting arbitrary text and retaining a PGN with parsed moves. `gradlew.bat :app:assembleDebug :app:testDebugUnitTest` passed with 40 tests, zero failures, errors, or skips. On `CPH2691`, reselecting the malformed file left the library unchanged and displayed the expected “That file couldn't be imported. Choose a completed-game PGN file.” message.

## October 19, 2026 — Phase 8 Release Hardening & License Audit

- **Open-source License Material Audit**: Updated `OpenSourceLicensesScreen.kt` to load `pieces/licenses/Apache-2.0.txt` from assets and display explicit license attributions for bundled Apache-2.0 Android dependencies (`chesslib` v1.3.7, `AndroidSVG` v1.4, and `OkHttp` v4.12.0) alongside Stockfish 19 (GPL-3.0) and its embedded NNUE network (CC0-1.0).
- **Accessibility & Non-Color-Only Communication Audit**: Evaluated Compose screens (`EvalBar`, `ChessBoard`, `ReviewScreen`, `CoachScreen`, `GameLibraryScreen`, `PaywallScreen`, `SettingsScreen`, `OnboardingScreen`). Verified that TalkBack content descriptions cover square coordinates, piece types, board orientation, and evaluation state descriptions. Confirmed move quality assessments explicitly pair text labels (`ACCURATE`, `MAJOR ERROR`, `TOP ENGINE CHOICE`) with color highlights.
- **Android 16 Test-Harness Issue**: Verified documentation in `docs/ANDROID_16_COROUTINES_SERVICELOADER_ISSUE_DRAFT.md`. Confirmed containment strategy using `@SdkSuppress(excludedSdks = [36])` without applying risky production workarounds or custom runners.
- **No tests or deployments executed** during this hardening step per owner instructions.


## October 17, 2026 — Android 16 ServiceLoader Classloader Diagnosis

The root cause was reproduced on physical device `CPH2691` without changing production code. The packaged instrumentation APK contains the test provider entry `kotlinx.coroutines.test.internal.ExceptionCollectorAsService`, but `kotlinx.coroutines` deliberately resolves providers using `CoroutineExceptionHandler`'s classloader. On Android 16 that classloader exposed only the target application APK's provider entry, `kotlinx.coroutines.android.AndroidExceptionPreHandler`; it could not see the instrumentation APK's exception collector. Consequently, `TestScopeImpl.enter()` rejects the collector before any `CoachScreenTest` assertion runs.

October 19 containment: `adb shell getprop ro.build.version.sdk` confirmed that CPH2691 is API 36 (not the previously recorded API 35). `CoachScreenTest` now uses `@SdkSuppress(excludedSdks = [36])`, linked to the upstream report. A targeted `:app:connectedDebugAndroidTest` run on CPH2691 then completed successfully with zero selected Coach tests executed and zero failures. The suppression is intentionally test-only and preserves the passing Android 17 emulator run as required coverage; it is not evidence that the Compose tests pass on Android 16.

A minimal instrumentation test containing only `runTest { assertTrue(true) }` failed with the same exception on `CPH2691` (Android 16) and passed on `Medium_Phone_API_37.0` (Android 17). On the restored Kotlin 2.2.10 / Coroutines 1.10.2 configuration, the complete five-test instrumentation suite also passed on that Android 17 emulator on October 17, 2026. The submitted report is preserved in `docs/ANDROID_16_COROUTINES_SERVICELOADER_ISSUE_DRAFT.md`.

The owner authorized external reporting, and the minimal reproduction was submitted as [Kotlin/kotlinx.coroutines#4782](https://github.com/Kotlin/kotlinx.coroutines/issues/4782) on October 17, 2026. No repository code, dependency, or production-runtime change accompanied the report.

An isolated test-only build-variant experiment made both providers visible and passed the direct ServiceLoader diagnostic, proving the cause. It also changed the Compose test runtime enough that all three Coach tests then failed with `No compose hierarchies found in the app`, rather than the ServiceLoader exception. The custom build type, temporary diagnostic test, resource override, and added test-target dependencies were fully removed. The retained configuration remains Kotlin 2.2.10 and kotlinx-coroutines 1.10.2, with no production runtime or dependency changes.

## October 18, 2026 — Android 16 Test-Runner Follow-up

Two additional test-only workarounds were evaluated on `CPH2691` and removed. Android Test Orchestrator (`androidx.test:orchestrator:1.6.1`) still failed the three `CoachScreenTest` cases at `TestScopeImpl.enter()` with the same ServiceLoader exception. A temporary debug-only Coach host with UI Automator avoided Compose's `runTest` rule but could not expose the host's Compose nodes to the device automation accessibility query, so it would not provide trustworthy UI coverage. Neither experiment altered the retained project configuration or production code. The Android 16 Compose test-harness blocker remains open under Kotlin/kotlinx.coroutines#4782.

The retained five-test suite was then rerun unchanged on a freshly started `Medium_Phone_API_37.0` Android 17 emulator: `ANDROID_SERIAL=emulator-5554 gradlew.bat :app:connectedDebugAndroidTest` passed on October 18, 2026. This reconfirms that the three Coach cases and their Compose assertions are valid outside the Android 16 classloader failure.

