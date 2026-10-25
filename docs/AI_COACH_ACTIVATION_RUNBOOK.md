# AI Coach activation runbook

## Current state

The Android app can now receive a non-secret Worker URL using the ignored local Gradle property
`aiCoachBaseUrl`. The fail-closed Worker is deployed at
`https://chess-review-ai-worker.chessin.workers.dev` (deployment version
`b2c3776f-77c9-482e-999b-1a6030da9cb9`), but no Worker secrets are provisioned. It remains
fail-closed with `AI_SERVICE_ENABLED=false`.

Do not enable the Coach until every prerequisite below is complete. A Worker URL is public
configuration; it is never an API key.

## Owner decisions required before activation

1. Accept the production OpenRouter and selected OpenAI-provider data terms, choose a privacy
   tier, and record the retention/model-improvement position.
2. Set the AI allowance (the current app implementation caps successful requests locally at 50),
   the monthly spend ceiling, and the human response to quota exhaustion.
3. Configure Play Console with the real package and `premium_lifetime` non-consumable product,
   then grant a dedicated service account only the Google Play Developer API permissions needed
   for purchase verification.
4. Finalize the privacy disclosure and pre-checkout copy with the selected terms and allowance.

## Provisioning sequence

From `worker/`, after the decisions above are recorded:

1. The first disabled deployment is already complete. To deploy later Worker changes while it
   remains disabled:

   ```powershell
   npx wrangler deploy
   ```

2. Set secrets interactively. Never pass values as command arguments, add them to
   `wrangler.json`, or place them in the Android project:

   ```powershell
   npx wrangler secret put OPENROUTER_API_KEY
   npx wrangler secret put GOOGLE_SERVICE_ACCOUNT_JSON
   ```

3. Set the real non-secret `PLAY_PACKAGE_NAME` and `PLAY_SKU` Worker configuration, retain
   `REQUIRE_VERIFIED_ENTITLEMENT=true`, and verify a valid and an invalid purchase token against
   the deployed Worker. Do not set `AI_SERVICE_ENABLED=true` until those checks pass.

4. Set the Android build's Worker endpoint only after successful Worker checks:

   ```properties
   # local.properties (ignored by Git)
   aiCoachBaseUrl=https://your-worker.your-subdomain.workers.dev/api/v1/explain
   ```

5. Enable `AI_SERVICE_ENABLED=true` in the reviewed Worker deployment, exercise one purchased
   user request, and confirm logs contain only redacted operational events. Keep the circuit
   breaker enabled whenever the spend ceiling or provider quota is reached.

## Guardrails that remain mandatory

- Keep `REQUIRE_VERIFIED_ENTITLEMENT=true`; never use a client-side premium flag as proof.
- Keep the OpenRouter route restricted to the OpenAI provider with fallbacks disabled.
- Send only the selected FEN, question, played move, and supplied Stockfish lines.
- Keep all prompts, FENs, purchase tokens, usernames, and API keys out of logs.
- Treat a live integration test and a real Play purchase/restore test as release evidence, not
  as completed until recorded.
