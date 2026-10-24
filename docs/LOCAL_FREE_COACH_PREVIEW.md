# Local free Coach preview

This setup is for a private debug build only. It runs the Worker on the developer machine with
OpenRouter's `openrouter/free` router. It does not alter the deployed Worker, release build, or
Google Play entitlement rule.

Free-router requests can be rate-limited and may use a provider other than OpenAI. They are not
approved for the production privacy route. Only the selected FEN, question, played move, and
Stockfish evidence are sent.

## Setup

1. In `worker/`, copy `.dev.vars.example` to `.dev.vars`, then paste the OpenRouter key into the
   empty `OPENROUTER_API_KEY=` value. Do not commit this file.

2. Start the private local Worker:

   ```powershell
   cd C:\Users\Drews\AndroidStudioProjects\chess_review\worker
   npx wrangler dev --local --port 8787
   ```

3. Add this non-secret setting to the ignored root `local.properties` file:

   ```properties
   aiCoachBaseUrl=http://10.0.2.2:8787/api/v1/explain
   ```

   `10.0.2.2` is for the Android Emulator. For a physical device, use the computer's LAN address
   and expose the local Worker only on a trusted private network.

4. Build and install the debug variant. The debug-only manifest permits cleartext traffic solely
   for this local preview. The debug build labels the local bypass in the Coach UI and does not
   require a Play purchase. The release variant still has no Coach endpoint unless one is supplied.

## Safety boundary

Do not set `MODEL_NAME=openrouter/free`, `ALLOW_FREE_ROUTER=true`, or
`REQUIRE_VERIFIED_ENTITLEMENT=false` in the deployed Worker's configuration. Those settings are
valid only in ignored local `.dev.vars` for a private development session.
