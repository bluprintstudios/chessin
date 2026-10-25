/**
 * Cloudflare Worker Entrypoint - Chess Review AI Gateway (OpenRouter)
 */

import { validateRequestSchema, extractAndValidateCandidateGrounding } from './schema.js';
import { callOpenRouterApi } from './openrouter.js';
import { verifyPlayPurchase } from './billing.js';

const MAX_PAYLOAD_BYTES = 10 * 1024; // 10 KB
const PRODUCTION_MODEL = 'openai/gpt-4.1-mini';
const LOCAL_PREVIEW_MODEL = 'openrouter/free';

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-App-Entitlement',
};

export default {
  async fetch(request, env, ctx, fetchImpl = fetch) {
    const requestId = crypto.randomUUID().substring(0, 8);

    // Handle CORS preflight
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }

    if (request.method !== 'POST') {
      return jsonResponse({ error: 'Method Not Allowed' }, 405);
    }

    // 1. Fail Closed / Disabled Service Check
    if (env?.AI_SERVICE_ENABLED !== 'true' || !env?.OPENROUTER_API_KEY) {
      logRedacted(requestId, 'SERVICE_DISABLED', 'AI service is currently unconfigured or disabled.');
      return jsonResponse({
        error: 'SERVICE_UNAVAILABLE',
        message: 'The AI coach service is currently unavailable or unconfigured.'
      }, 503);
    }

    // 2. Entitlement / Identity Check (Fail closed when entitlement required)
    if (env?.REQUIRE_VERIFIED_ENTITLEMENT === 'true') {
      const authHeader = request.headers.get('Authorization') || request.headers.get('X-App-Entitlement');
      if (!authHeader || !authHeader.startsWith('Bearer ') || authHeader.length < 15) {
        logRedacted(requestId, 'UNAUTHORIZED', 'Missing or invalid entitlement token.');
        return jsonResponse({
          error: 'UNAUTHORIZED',
          message: 'A valid purchase entitlement token is required.'
        }, 401);
      }

      const token = authHeader.substring(7);
      const verifyResult = await verifyPlayPurchase(
        token,
        env.PLAY_SKU || 'premium_lifetime',
        env.PLAY_PACKAGE_NAME || 'com.pro.chess_review',
        env.GOOGLE_SERVICE_ACCOUNT_JSON,
        fetchImpl
      );

      if (!verifyResult.valid) {
        logRedacted(requestId, 'FORBIDDEN', `Purchase verification failed: ${verifyResult.error}`);
        return jsonResponse({
          error: 'FORBIDDEN',
          message: `Purchase verification failed: ${verifyResult.error}`
        }, 403);
      }
    }

    // 3. Quota / Circuit Breaker Check
    if (env?.CIRCUIT_BREAKER_ACTIVE === 'true') {
      logRedacted(requestId, 'CIRCUIT_BREAKER', 'Budget or provider quota limit reached.');
      return jsonResponse({
        error: 'QUOTA_EXHAUSTED',
        message: 'AI usage quota or budget limit has been reached for this period.'
      }, 429);
    }

    // 4. Payload Size Limit Check
    const contentLength = parseInt(request.headers.get('content-length') || '0', 10);
    if (contentLength > MAX_PAYLOAD_BYTES) {
      logRedacted(requestId, 'PAYLOAD_TOO_LARGE', `Content-Length ${contentLength} exceeds max allowed.`);
      return jsonResponse({
        error: 'PAYLOAD_TOO_LARGE',
        message: 'Request payload exceeds maximum size limit (10 KB).'
      }, 413);
    }

    // 5. Parse JSON Body
    let bodyText;
    try {
      bodyText = await request.text();
    } catch {
      return jsonResponse({ error: 'BAD_REQUEST', message: 'Failed to read request body.' }, 400);
    }

    if (bodyText.length > MAX_PAYLOAD_BYTES) {
      return jsonResponse({ error: 'PAYLOAD_TOO_LARGE', message: 'Request payload exceeds maximum size limit (10 KB).' }, 413);
    }

    let body;
    try {
      body = JSON.parse(bodyText);
    } catch {
      return jsonResponse({ error: 'INVALID_JSON', message: 'Malformed JSON payload.' }, 400);
    }

    // 6. Schema & Unknown Field Validation
    const schemaValidation = validateRequestSchema(body);
    if (!schemaValidation.valid) {
      logRedacted(requestId, 'SCHEMA_INVALID', schemaValidation.error);
      return jsonResponse({
        error: 'INVALID_SCHEMA',
        message: schemaValidation.error
      }, 400);
    }

    // 7. Call OpenRouter Model Upstream
    const modelName = env.MODEL_NAME || PRODUCTION_MODEL;
    const allowFreeRouter = env.ALLOW_FREE_ROUTER === 'true' && modelName === LOCAL_PREVIEW_MODEL;
    if (modelName !== PRODUCTION_MODEL && !allowFreeRouter) {
      logRedacted(requestId, 'MODEL_NOT_ALLOWED', 'The configured model is not on the approved allowlist.');
      return jsonResponse({
        error: 'SERVICE_UNAVAILABLE',
        message: 'The AI coach service is currently unavailable or unconfigured.'
      }, 503);
    }
    const openRouterResult = await callOpenRouterApi({
      apiKey: env.OPENROUTER_API_KEY,
      modelName,
      allowFreeRouter,
      body,
      timeoutMs: 8000,
      fetchImpl
    });

    if (openRouterResult.error) {
      logRedacted(requestId, 'UPSTREAM_ERROR', `Upstream status: ${openRouterResult.status}, code: ${openRouterResult.error}`);
      return jsonResponse({
        error: openRouterResult.error,
        message: 'AI provider service error or timeout.'
      }, openRouterResult.status || 502);
    }

    // 8. Grounding Check & Response Formatting
    const grounding = extractAndValidateCandidateGrounding(openRouterResult.text, body.engineEvidence);

    logRedacted(requestId, 'SUCCESS', 'Explanation generated successfully.');

    return jsonResponse({
      version: '1',
      explanation: openRouterResult.text,
      groundedCandidates: grounding.groundedCandidates,
      disclaimer: 'AI explanation grounded in local engine facts. Scores and lines authority belongs strictly to Stockfish.'
    }, 200);
  }
};

function jsonResponse(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json',
      ...CORS_HEADERS
    }
  });
}

function logRedacted(requestId, code, message) {
  // Redact all sensitive fields (FEN, prompts, responses, tokens) from worker logs
  console.log(`[AI-Worker][${requestId}][${code}] ${message}`);
}
