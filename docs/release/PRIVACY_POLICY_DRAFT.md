# Chessin' Privacy Policy (Draft)

**Effective Date:** [Date]  
**Publisher:** [Publisher Name]  
**Support Contact:** [Support Email]  
**Public URL:** [Placeholder URL]

This Privacy Policy explains how Chessin' ("we", "our", or "the App") collects, uses, and protects your information. Chessin' is an independent chess review application and is not affiliated with, endorsed by, or connected to Chess.com or any other chess platform.

## 1. Local Data Storage (Default)
Chessin' is designed to be local-first. By default, your data stays on your device:
- **Imported Games:** PGN (Portable Game Notation) files you import are saved locally in the app's database.
- **Game Reviews:** Cached Stockfish engine analysis, position evaluations, and move-quality annotations are stored locally.
- **Preferences:** Your UI settings, piece set choices, and AI consent flags are stored locally on your device.

We do not upload your full game library, account history, or username to our servers.
Android cloud backup and device-to-device backup are disabled for this app so that these local records are not copied by the operating system.

## 2. Optional AI Coach Data
Chessin' offers an optional "AI Coach" feature. Data is only sent to our servers when you explicitly request an explanation for a specific position and consent to the transmission.

**What is sent to the AI service:**
- The current board position (FEN string).
- The specific move played in that position.
- Your text question.
- Relevant Stockfish engine evaluation lines for that position.

**What is not sent as part of the AI Coach request:**
- Your full PGN or entire game history.
- Your opponent's moves from other parts of the game.
- Your username, real name, or player rating.
- Any payment tokens, billing details, or App secrets.

## 3. Third-Party Processing & Data Retention
*The publisher must replace this section with the approved, current OpenRouter and selected-provider terms before publication.*

When you use the AI Coach, your request is routed through our secure gateway (hosted by Cloudflare) to our AI provider (OpenRouter / OpenAI).
- **Provider terms:** The final policy must identify whether requests may be retained, whether they are used for model improvement, the configured retention period, and available deletion controls. These terms are not yet approved for Chessin'.

## 4. Payment Information
Chessin' offers a one-time lifetime membership processed entirely by Google Play Billing. We do not collect, process, or store your credit card details, billing name, or address. We only securely verify the anonymized purchase token provided by Google Play to unlock premium features.

## 5. Deleting Your Data
You have full control over your data.
- **Local Data Deletion:** You can delete all your imported games, cached analysis, and local preferences at any time by navigating to **Settings > Delete local data** and selecting "Delete all data".
- **AI Data:** The final policy will describe the approved provider retention and deletion process. Until production AI is approved and enabled, AI Coach requests remain unavailable.

## 6. Contact Us
If you have any questions about this Privacy Policy, please contact us at: [Support Email].
