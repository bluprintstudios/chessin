# Owner Publication Checklist

*This is a concise list of missing values, decisions, and external tasks the Project Owner must complete before the app can be compiled for release and published to the Google Play Store.*

### 1. Legal & Identity
- [ ] **Publisher Name:** Provide the official developer name to be used on the App Store/Play Store.
- [ ] **Support Email:** Create and provide a public-facing support email address.
- [ ] **Privacy Policy URL:** Host the finalized `PRIVACY_POLICY_DRAFT.md` on a public webpage and provide the URL.
- [ ] **App Name & Branding:** Finalize "Chessin'" (or choose an alternative) and confirm it passes trademark/branding checks (ensuring no implied affiliation with Chess.com).

### 2. Assets & Permissions
- [ ] **PGN source terms:** Keep imports source-neutral and review a source's terms before marketing source-specific support for a paid analysis product. The MVP does not use a Chess.com API or account integration.
- [ ] **Piece asset review:** Recheck commercial-use compatibility and attribution for the ten bundled runtime sets. The Staunty SVGs are CC BY-NC-SA, excluded from runtime assets, and need no release action unless they are added later.
- [ ] **Stockfish GPL review:** Complete the distribution and Android installation-information review against the bundled GPL notice, exact source/build pointer, NNUE source, and binary hashes.

### 3. AI Infrastructure & Budget
- [ ] **OpenAI/OpenRouter Data Terms:** Formally approve the 30-day data retention policy for abuse monitoring, OR configure the API for Zero Data Retention (ZDR).
- [ ] **Fair-Use Quota:** Define the exact monthly AI question allowance for a $9.99 lifetime user.
- [ ] **Production Secret:** Provision the actual `OPENROUTER_API_KEY` into the Cloudflare Worker environment.

### 4. Play Billing
- [ ] **Play Console SKU:** Create the $9.99 one-time non-consumable SKU (`premium_lifetime`) in the Google Play Console.
- [ ] **Service Account JSON:** Generate a Google Cloud Service Account JSON with `androidpublisher` permissions and provide it securely to the Cloudflare Worker secret vault (do NOT commit to source control).
- [ ] **Revocation Architecture:** Set up Real-Time Developer Notifications (RTDN) via Google Cloud Pub/Sub and connect it to a Cloudflare KV store so the Worker can process refunds instantly without hitting Google's rate limits.

### 5. Final UI
- [ ] **Figma reconciliation:** Review the editable 16-frame Chess_Review handoff against every implemented screen and state on device, then record any visual gaps.
