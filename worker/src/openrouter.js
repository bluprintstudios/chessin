/**
 * Upstream OpenRouter API Client for Cloudflare Worker
 * Production model ID: openai/gpt-4.1-mini. The free router is permitted only for a
 * local development Worker that explicitly sets ALLOW_FREE_ROUTER=true.
 */

const FREE_ROUTER_MODEL = 'openrouter/free';

export function providerRoutingFor(modelName, allowFreeRouter) {
  if (allowFreeRouter && modelName === FREE_ROUTER_MODEL) {
    // The OpenRouter free router itself selects a compatible free provider. Never use this
    // configuration in a deployed production environment.
    return { allow_fallbacks: false };
  }
  return {
    only: ['openai'],
    allow_fallbacks: false
  };
}

export async function callOpenRouterApi({ apiKey, modelName, allowFreeRouter = false, body, timeoutMs = 8000, fetchImpl = fetch }) {
  const url = 'https://openrouter.ai/api/v1/chat/completions';

  const evidenceText = body.engineEvidence.map((ev, idx) => {
    const scoreText = ev.mateIn !== null
      ? (ev.mateIn === 0 ? 'Checkmate' : `${ev.mateIn > 0 ? 'White' : 'Black'} mates in ${Math.abs(ev.mateIn)}`)
      : (ev.scoreCp === null ? 'Score unavailable' : `${ev.scoreCp} cp (White perspective)`);
    const continuation = ev.pvSan?.length ? ev.pvSan.join(' ') : ev.san;
    return `Line ${idx + 1}: Move ${ev.san} (${ev.uci}), Eval: ${scoreText}, Depth: ${ev.depth}, Supplied continuation (may be truncated): ${continuation}`;
  }).join('\n');

  const systemInstruction = `You are a helpful chess coach assistant.
Your job is to explain the provided Stockfish evaluation and candidate lines to a player asking a question about a position.
CRITICAL CONSTRAINTS:
1. Stockfish engine output is the authoritative source of truth for all evaluations, scores, and candidate moves.
2. Do NOT invent new move scores, principal variations, or ratings.
3. Keep your explanation concise, encouraging, and focused directly on the player's question and supplied engine lines.
4. Centipawn scores are White-relative: positive favors White, negative favors Black. Mate labels name the winning side.
5. The FEN and candidate lines describe the position AFTER the played move (or the starting position if no move is supplied). Without before-move evidence, do not claim a measured loss or classify the played move.
6. Explain only supplied continuations. Do not extend truncated lines or infer an unseen tactical sequence. If evidence is insufficient, say so.`;

  const promptText = `Position (FEN): ${body.fen}
Played Move: ${body.playedMove || '(starting position; no move played)'}
User Question: ${body.question}

Stockfish Engine Evidence:
${evidenceText}

Provide a concise, grounded explanation:`;

  const openRouterPayload = {
    model: modelName,
    messages: [
      { role: 'system', content: systemInstruction },
      { role: 'user', content: promptText }
    ],
    temperature: 0.2,
    max_tokens: 300,
    response_format: {
      type: 'json_schema',
      json_schema: {
        name: 'coach_explanation_response',
        strict: true,
        schema: {
          type: 'object',
          properties: {
            explanation: {
              type: 'string',
              description: 'Concise, grounded chess coach explanation.'
            }
          },
          required: ['explanation'],
          additionalProperties: false
        }
      }
    },
    provider: providerRoutingFor(modelName, allowFreeRouter)
  };

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetchImpl(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${apiKey}`,
        'HTTP-Referer': 'https://chessreview.app',
        'X-Title': 'Chess Review App'
      },
      body: JSON.stringify(openRouterPayload),
      signal: controller.signal
    });

    clearTimeout(timer);

    if (!response.ok) {
      const statusCode = response.status;
      if (statusCode === 429) {
        return { error: 'PROVIDER_RATE_LIMITED', status: 429 };
      }
      return { error: `PROVIDER_ERROR_${statusCode}`, status: 502 };
    }

    const resJson = await response.json();
    const rawContent = resJson?.choices?.[0]?.message?.content;

    if (!rawContent || typeof rawContent !== 'string') {
      return { error: 'MALFORMED_PROVIDER_RESPONSE', status: 502 };
    }

    // Fail closed: strictly parse and validate JSON output according to schema
    try {
      const parsed = JSON.parse(rawContent);
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        return { error: 'MALFORMED_PROVIDER_RESPONSE', status: 502 };
      }

      const keys = Object.keys(parsed);
      if (keys.length !== 1 || keys[0] !== 'explanation') {
        return { error: 'MALFORMED_PROVIDER_RESPONSE', status: 502 };
      }

      if (typeof parsed.explanation !== 'string' || parsed.explanation.trim().length === 0) {
        return { error: 'MALFORMED_PROVIDER_RESPONSE', status: 502 };
      }

      return { text: parsed.explanation };
    } catch {
      // Never fall back to raw provider text; fail closed on invalid JSON
      return { error: 'MALFORMED_PROVIDER_RESPONSE', status: 502 };
    }
  } catch (err) {
    clearTimeout(timer);
    if (err.name === 'AbortError') {
      return { error: 'PROVIDER_TIMEOUT', status: 504 };
    }
    return { error: 'PROVIDER_CONNECTION_FAILED', status: 502 };
  }
}
