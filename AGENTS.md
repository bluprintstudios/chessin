# Agent Instructions

These instructions apply to AI coding agents working in this repository.

## Read first

1. [PLAN.md](PLAN.md) — product scope, architecture, constraints, and decisions.
2. [PHASES.md](PHASES.md) — implementation sequence and phase exit criteria.
3. [CHECKLIST_ACCEPTANCE.md](CHECKLIST_ACCEPTANCE.md) — release acceptance checklist.
4. [AGENT_PROMPTS.md](AGENT_PROMPTS.md) — ready-to-paste prompts for each phase.
5. [docs/AI_AGENT_BUILD_PLAN.md](docs/AI_AGENT_BUILD_PLAN.md) — detailed technical reference and official links.

## Mission

Build an original Kotlin/Jetpack Compose Android chess post-game review and coaching app. Follow the approved Figma/Stitch design handoff. Implement one phase at a time, maintain a working app, and report completed items, changed files, verification performed, and unresolved blockers at the end of each phase.

## Non-negotiable constraints

- Do not copy Chess.com branding, UI, layout, artwork, proprietary move glyphs, sounds, or feature names. Use an original name and original visual system. The app must clearly state it is independent and unaffiliated.
- Use Chess.com’s documented public API only; never scrape pages or use undocumented endpoints. Work with public completed-game data only. Do not provide engine hints for live games.
- Commercial use of Chess.com data is a launch gate. Do not initiate outreach or claim approval. Mark the gate unresolved until the project owner supplies written authorization or chooses another permitted product/data path.
- Treat Stockfish as the authority for evaluations and candidate lines. Use AI to explain structured engine evidence; never ask AI to invent scores, lines, classifications, or ratings.
- Keep games and analysis on device by default. Send only the minimum context required for an explicit AI request. Do not put PGN/FEN, prompts, usernames, purchase tokens, or secrets in logs.
- Never put provider keys, Google service account credentials, or other secrets in the Android app or source control. Backend AI and purchase verification must validate requests server-side.
- The proposed $9.99 purchase is one-time and non-consumable. Do not silently add a subscription. Disclose any AI fair-use allowance and what happens when provider quotas/budgets are reached.
- Check the current official docs for external services/libraries before integration. Record license, quota, and terms assumptions in the plan or implementation notes.
- Preserve existing user changes. Do not reset, overwrite, or broadly reformat unrelated files.
- If a design, credential, permission, signing key, or external account is missing, complete independent work and record the dependency. Do not fabricate access or approval.

## Repository-specific guidance

- The current app is a minimal single-module Android Compose starter. Keep `:app` as one module until there is a concrete reason to split it.
- Use Kotlin, Jetpack Compose, coroutines/Flow, lifecycle-aware ViewModels, Room for local games/reviews, and DataStore for settings unless a documented constraint warrants a different choice.
- Keep the chess rules/PGN parser behind an interface. Confirm its Android support and license before adding it.
- Keep Stockfish behind a `ChessEngine` interface. Version the engine and NNUE data; handle supported Android ABIs, UCI process lifecycle, cancellation, battery use, and GPL source/notice requirements.
- Use Cloudflare Worker as a small protected AI/purchase-verification gateway. Do not run Stockfish in a Worker. Keep the Gemini key and Play service-account credentials as Worker secrets.
- Do not make MVP dependent on Cloud Functions being free. Avoid remote storage of game archives unless a later approved feature requires it.

## How to work

1. Confirm the active phase and its prerequisites before editing.
2. Read the exact screens/states from the Figma/Stitch handoff before UI work. If the handoff is absent, do non-visual work and record UI items as awaiting design; do not invent final UI.
3. State a short implementation plan, then edit only files needed for the active phase.
4. Keep architecture and contracts small and typed. Handle loading, empty, offline, cancellation, malformed input, throttling, and provider failures.
5. Update the applicable checklist and record evidence for completed acceptance items. Never check an item based only on intention.
6. Do not claim a build, test, manual check, API approval, or billing verification happened unless it actually happened. Do not run tests unless the project owner explicitly asks for testing/verification.
7. Do not move into a dependent phase when a launch gate is unresolved. Continue independent work where possible and report the specific dependency.

## End-of-phase report format

```text
Phase: <number and name>
Status: complete | partial | blocked
Changed files: <list>
Acceptance items completed: <list with evidence>
Verification performed: <only what actually ran or was inspected>
Open blockers/decisions: <specific items>
Next phase recommendation: <number and reason>
```

