# Chessin'

> **Work in progress — not a finished product.**
>
> Chessin' is an independent Android app for reviewing completed chess games, replaying key positions, and learning from local engine evidence. The screens below show the current prototype build; features, copy, pricing, and release scope are still evolving.

## What has been built so far

- Local PGN import using Android's document picker
- On-device game library and replay flow
- Stockfish-powered post-game review with evaluation trajectory and candidate lines
- A focused, opt-in AI Coach flow grounded in selected engine lines
- Themeable HUD-style Android UI for onboarding, settings, open-source notices, and membership
- Local-first storage for imported games and analysis

## Current prototype screens

<p align="center">
  <img width="220" alt="Chessin game library" src="https://github.com/user-attachments/assets/8e912707-ac72-48dc-9363-2e41cb8ba8d3" />
  <img width="220" alt="Chessin import PGN screen" src="https://github.com/user-attachments/assets/9d78e17b-edc2-469b-b3d9-412c5e71e022" />
  <img width="220" alt="Chessin AI Coach screen" src="https://github.com/user-attachments/assets/fbca8936-85d4-432b-bdb4-c551e017b112" />
</p>

<p align="center">
  <img width="220" alt="Chessin settings screen" src="https://github.com/user-attachments/assets/942cf25c-1589-4786-a0db-f880da6e7ad3" />
  <img width="220" alt="Chessin membership screen" src="https://github.com/user-attachments/assets/6be338e2-41ac-4cbf-93e8-bdb7d8d7e45a" />
</p>

## Where the project is today

Chessin' is **not ready for public release** yet. This repository is a transparent look at the current Android build and design direction.

Still in progress:

- Final QA across devices, screen sizes, accessibility settings, and error states
- Production AI service configuration, privacy terms, fair-use limits, and budget controls
- Google Play billing verification and final purchase disclosures
- Final review of third-party licenses, Stockfish distribution requirements, and source-data terms
- Polished onboarding, license, and edge-case flows

## Product principles

- **Local first:** imported games and analysis stay on the device by default.
- **Completed games only:** Chessin' does not provide live-game engine hints.
- **Grounded coaching:** the coach is intended to explain Stockfish evidence, not invent evaluations or lines.
- **Independent:** Chessin' is not affiliated with, endorsed by, or connected to Chess.com.

## Technical direction

The Android client is built with Kotlin and Jetpack Compose. It uses Room for local storage, DataStore for settings, and Stockfish for local analysis. A small Cloudflare Worker is planned as the protected gateway for explicitly requested AI explanations and purchase verification; it is not an engine host.

## Open-source notices

Stockfish is included under GPL-3.0-or-later, with its notices and source references bundled in the app. Chess piece artwork and other third-party dependencies retain their respective licenses; see the in-app **Open source** screen and the project documentation for details.

## For potential backers and collaborators

Thank you for looking at this early build. The goal is a calm, privacy-respecting post-game study tool—not another live-play companion. Feedback on the experience, the review flow, and what would make post-game learning more useful is welcome.

---

*Screens shown are development captures from the current prototype. They are not final release imagery.*
