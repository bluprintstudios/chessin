# Google Play Data Safety Worksheet

*This is a preparatory worksheet. Do not submit to Google Play until the project owner makes final decisions regarding analytics, AI terms, and logging.*

## 1. App Activity & Interactions
**Does your app collect or share any of the required user data types?**  
**Draft Answer:** Yes (If AI Coach is used).

**Is all of the user data collected by your app encrypted in transit?**  
**Draft Answer:** Yes. All data sent to the Cloudflare Worker/OpenRouter is sent over HTTPS.

**Do you provide a way for users to request that their data be deleted?**  
**Draft Answer:** Yes. Users can wipe all local storage via the in-app Settings.

## 2. Data Types Collected & Shared

### A. Personal & Financial Information
- **Name / Email / Address:** Not collected.
- **User IDs:** Not collected.
- **Purchase History:** Processed by Google Play. The app verifies anonymous purchase tokens but does not collect or link them to a user identity.

### B. User Content
- **Other User-Generated Content (AI Questions)**
  - **Collected?** Yes (Opt-in).
  - **Shared?** Yes (Sent to Cloudflare -> OpenRouter/OpenAI).
  - **Processed Ephemerally?** To be confirmed from the approved production OpenRouter/OpenAI configuration before submission.
  - **Required or Optional?** Optional (User must consent and trigger the request).
  - **Purpose:** App Functionality.

### C. App Activity & Game Info
- **Game Context (FEN Strings, played moves)**
  - **Collected?** Yes (Opt-in).
  - **Shared?** Yes (Sent to AI provider as context).
  - **Required or Optional?** Optional (Only sent if AI Coach is used).
  - **Purpose:** App Functionality.
- **PGN Files / Full Game History**
  - **Collected?** No. Stored strictly locally. Never transmitted off-device.

### D. App Info and Performance
- **Crash Logs / Diagnostics:** Currently Not Collected (No Firebase Crashlytics integrated yet).

## 3. Unresolved Owner Decisions (Blocking Play Console Submission)
- [ ] **Provider retention:** Approve the production OpenRouter/OpenAI retention and model-improvement terms, including whether a zero-data-retention configuration is available and selected.
- [ ] **Analytics/Crashlytics:** Will the app integrate crash reporting before launch? If so, "Crash logs" must be added to this worksheet.
- [ ] **Support Contact:** An official support email and privacy policy URL must be generated and hosted publicly to link in the Play Console.
