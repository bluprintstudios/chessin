# Build Phases

Run these phases in order. Use the matching prompt in [AGENT_PROMPTS.md](AGENT_PROMPTS.md). A phase is complete only when its exit criteria are evidenced and its boxes are updated in [CHECKLIST_ACCEPTANCE.md](CHECKLIST_ACCEPTANCE.md).

## Phase 0 — Discovery and release gates

**Goal:** settle product blockers and gather inputs without starting dependent implementation.

- [x] Obtain the Figma handoff and editable Android screen inventory. The Chess_Review file now contains the Chessin' foundations and 16 mobile frames (October 15, 2026); see `docs/FIGMA_HANDOFF_STATE.json`.
- [x] Record the owner's PGN-only MVP decision: no game-site account/API integration; commercial rights for third-party-origin PGNs remain a release review item.
- [x] Record the owner's provisional product name, Chessin'; final name/trademark clearance and identity review remain open.
- [x] Record the owner's GPL route and pin source-built Stockfish 19 Android binaries; final distribution/install-information review remains open.
- [ ] Select AI privacy tier, disclosed use allowance, and monthly spend limit.
- [ ] Confirm Play Console app/package readiness, one-time SKU, verification approach, restore/refund behavior.

**Exit:** design handoff is actionable; risks and human-owned decisions are recorded. No claim of approval without evidence.

## Phase 1 — App foundation and design system

**Goal:** build the app foundation and provisional launch flow now; refine final screens once the approved design arrives.

- [x] Add domain game/failure models, repository boundary, and stable route identifiers without inventing screen UI.
- [x] Add Room game entity/DAO/database/repository and DataStore preferences.
- [x] Add Room, DataStore, Navigation Compose, and KSP dependencies through the version catalog.
- [x] Keep the existing single app module.
- [x] Add typed screen state (`UiState`) for empty/loading/error/offline representations.
- [x] Add Navigation Compose host and owner-authorized provisional game-library shell; final visual refinement awaits Figma/Stitch handoff.
- [x] Implement an original provisional theme and library loading/empty/error states; PGN library works locally without a network connection.
- [ ] Add design-backed screen previews and restore selected screen/state as appropriate.

**Exit:** architecture, persistence, routes, and a working provisional launcher/library are in place. Design-backed previews and final visual refinement remain open until Figma/Stitch is supplied.

## Phase 2 — Rules, PGN, and local library

**Goal:** parse and replay games without network or engine dependencies.

- [x] Select a supported chess/PGN rules library after checking license and Android support.
- [x] Parse games, metadata, moves, comments/clocks, castling, en passant, promotions, and incomplete/malformed input.
- [x] Add board position and legal replay from PGN/FEN.
- [x] Persist games, selected ply, and import dedupe key in Room.
- [x] Add PGN file picker and understandable import failures.

**Exit:** a PGN imports and replays accurately after restart; broken files fail safely.

## Phase 3 — PGN-only import scope

**Goal:** keep the MVP source-neutral and import user-selected PGN files locally. Account connections and automatic game-site imports are out of scope.

- [x] Remove Chess.com API client, sync use case/ViewModel, app wiring, and username preference from the MVP.
- [x] Keep the existing system PGN picker, parser, local persistence, and replay path.
- [ ] Review terms for any third-party-origin PGNs before describing a source as supported or selling source-specific import/review.

**Exit:** PGNs import and remain local; no game-site network connection, credential collection, scraping, or automatic download is part of the MVP.

## Phase 4 — Local Stockfish engine

**Goal:** produce reproducible, local evaluation and up to five continuations.

- [x] Build Stockfish 19 from pinned official source for Android ARM64 and ARMv7 NEON; record source, NNUE, build inputs, and binary hashes in `docs/third_party/stockfish/README.md`.
- [x] Define `ChessEngine` and UCI process lifecycle, readiness, parser, cancellation, timeout, and cleanup.
- [x] Implement MultiPV up to five and legal UCI-to-SAN PV output (SAN conversion uses chesslib's position-aware `MoveList.toSanArray()`).
- [x] Normalize eval perspective; model mate scores separately.
- [x] Cache results by position + engine version + settings; show depth/time and incomplete state.
- [x] Bundle the GPL-3.0-or-later text and expose the Stockfish 19 source/release notice in the provisional Settings flow.
- [ ] Review final Android packaging, installation-information, source availability, and Play delivery requirements before release.

**Exit:** local analysis is perspective-correct, cancellable, reproducible at fixed settings, and does not block UI.

## Phase 5 — Review and game insights

**Goal:** connect replay and engine facts into an understandable report.

- [x] Implement eval graph/score, line alternatives, move navigation, PV continuation, and key moments.
- [x] Define and version original move-quality thresholds/labels, including mate/forced-move edge cases (`move-quality-v1`).
- [x] Calculate summary metrics from completed cached analysis only; incomplete/missing engine results remain unclassified.
- [ ] Define performance-estimate calibration; show range, sample size, and confidence. `opponent-result-v1` now calculates a local, low-confidence single-game rough performance range when both PGN ratings and a completed result exist; final Game Summary display and a multi-game, time-control-aware calibration dataset remain open. See `docs/ROUGH_STRENGTH_ESTIMATE.md`.
- [x] Implement the review board, evaluation bar, move controls, and quality annotations directly in Compose per owner approval; add a persisted selector for ten piece themes with bundled attribution/license texts. Non-commercial Staunty files remain outside runtime resources.
- [x] Inspect the review screen in Compose Preview at phone, tablet (`1280dp x 800dp @ 240dpi`), and flipped-board configurations. (Preview QA; not an emulator/device run.)

**Exit:** every selected move can be inspected with legal candidate lines and a plain-language reason tied to engine evidence.

## Phase 6 — AI coach and Cloudflare Worker

**Goal:** securely explain a selected position and answer follow-up questions.

- [x] Implement Worker endpoint, secrets, auth/App Check validation strategy, strict schema, model allowlist, size limits, timeouts, quota, and redacted logs.
- [x] Send compact selected-position context and Stockfish lines, never full account history.
- [x] Validate structured model output and provide a graceful no-AI/retry state.
- [x] Add first-use AI privacy disclosure and fair-use meter/limit.
- [x] Add server-side rate-limit/cost circuit breaker and safe behavior on exhausted provider quota.
- [x] Add a provisional Compose Coach screen with position evidence, consent, allowance, and unavailable states; requests remain disabled until gateway and production terms are approved.
- [x] Verify Coach corrections with an Android debug build, 37 unit tests, 31 Worker tests, and 5 emulator instrumentation tests; inspect dark-theme draft/preview captures (October 15, 2026). Live service and final Figma reconciliation remain pending.

**Exit:** no model secret in APK; response refers to supplied engine facts; unauthenticated/unentitled/excess requests are rejected safely.

## Phase 7 — Play Billing and entitlement

**Goal:** implement one-time purchase, restore, and protected feature access.

- [x] Configure non-consumable lifetime SKU at $9.99; render Play-provided localized price. (Configured locally; Play Console blocked).
- [x] Implement BillingClient, pending/success/error, acknowledge, restore, reconnect, and duplicate handling.
- [x] Verify purchase token and fixed package/SKU server-side with Play Developer API. (Worker checks implemented via mocked service boundary).
- [x] Handle invalid/refunded/pending state and avoid trusting a local entitlement flag on backend.
- [x] Add a provisional Compose membership screen with Play-provided price, restore path, and checkout disabled pending AI disclosures.
- [ ] Disclose lifetime access, AI allowance, restore, support, and privacy before purchase.

**Exit:** purchase and restore work, protected AI requires valid purchase, and offline local games remain available.

## Phase 8 — Release readiness

**Goal:** produce a release candidate with product, licensing, privacy, security, and cost gates resolved.

- [ ] Complete automated test suite and manual QA once project owner authorizes testing. Owner authorized testing October 17, 2026: debug build, 40 Android unit tests, 31 Worker tests, and manual onboarding/library device smoke checks passed on CPH2691 (Android 16). The connected Compose suite remains blocked by a coroutine ServiceLoader failure before 3 Coach test assertions.
- [ ] Verify small/large screens, accessibility, offline, sync errors, engine cancellation, ABI compatibility, clean install, update, and restore. Engine cancellation passed on CPH2691 (Android 16) after leaving an uncached review with Android hardware back. With Wi-Fi temporarily disabled and mobile data left in its original off state, local library and Stockfish review continued to work. The library also remained readable at 1.3× font scale and in landscape, and local games survived force-stop/relaunch and repeated debug APK updates; the other scenarios remain open.
- [ ] Complete dependency/license inventory and required Stockfish/parser source/notices.
- [ ] Complete privacy policy, AI disclosure, deletion steps, Play Data Safety, store listing, independent-product disclaimer, and support contact.
- [ ] Set production Worker secrets, monitoring, quota/cost alerts, app signing, and Play internal testing.
- [ ] Recheck third-party PGN source terms, provider data terms, Play Billing, and pricing immediately before release. Chess.com account/API integration is not in the MVP.

**Exit:** signed release candidate, all launch gates evidenced, and projected AI costs within disclosed limits.

