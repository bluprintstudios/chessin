import { test, describe } from 'node:test';
import assert from 'node:assert';
import worker from '../src/index.js';
import { providerRoutingFor } from '../src/openrouter.js';

describe('Chess Review AI Worker Gateway Tests (OpenRouter)', () => {
  const validBody = {
    version: '1',
    fen: 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    question: 'Why is e4 recommended here?',
    playedMove: 'e4',
    engineEvidence: [
      {
        uci: 'e2e4',
        san: 'e4',
        scoreCp: 45,
        mateIn: null,
        depth: 18
      }
    ]
  };

  const validEnv = {
    AI_SERVICE_ENABLED: 'true',
    OPENROUTER_API_KEY: 'test-openrouter-key-12345',
    REQUIRE_VERIFIED_ENTITLEMENT: 'false',
    MODEL_NAME: 'openai/gpt-4.1-mini',
    GOOGLE_SERVICE_ACCOUNT_JSON: JSON.stringify({
      client_email: 'test@example.com',
      private_key: '-----BEGIN PRIVATE KEY-----\ndmFsaWRiYXNlNjQ=\n-----END PRIVATE KEY-----'
    })
  };

  const validHeaders = {
    'Content-Type': 'application/json'
  };

  test('Free router is available only when explicitly enabled for local preview', () => {
    assert.deepStrictEqual(providerRoutingFor('openrouter/free', true), {
      allow_fallbacks: false
    });
    assert.deepStrictEqual(providerRoutingFor('openrouter/free', false), {
      only: ['openai'],
      allow_fallbacks: false
    });
  });

  test('CORS OPTIONS preflight returns 204 with CORS headers', async () => {
    const req = new Request('https://worker.local/', { method: 'OPTIONS' });
    const res = await worker.fetch(req, validEnv, {});
    assert.strictEqual(res.status, 204);
    assert.strictEqual(res.headers.get('Access-Control-Allow-Origin'), '*');
  });

  test('Rejects non-POST methods with 405', async () => {
    const req = new Request('https://worker.local/', { method: 'GET' });
    const res = await worker.fetch(req, validEnv, {});
    assert.strictEqual(res.status, 405);
  });

  test('Service disabled (AI_SERVICE_ENABLED = false) returns 503', async () => {
    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify(validBody)
    });
    const res = await worker.fetch(req, { ...validEnv, AI_SERVICE_ENABLED: 'false' }, {});
    assert.strictEqual(res.status, 503);
    const data = await res.json();
    assert.strictEqual(data.error, 'SERVICE_UNAVAILABLE');
  });

  test('Missing OPENROUTER_API_KEY returns 503', async () => {
    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify(validBody)
    });
    const res = await worker.fetch(req, { ...validEnv, OPENROUTER_API_KEY: '' }, {});
    assert.strictEqual(res.status, 503);
  });

  test('Missing entitlement token when required returns 401', async () => {
    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(validBody)
    });
    const res = await worker.fetch(req, { ...validEnv, REQUIRE_VERIFIED_ENTITLEMENT: 'true' }, {});
    assert.strictEqual(res.status, 401);
    const data = await res.json();
    assert.strictEqual(data.error, 'UNAUTHORIZED');
  });

  test('Circuit breaker active returns 429', async () => {
    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify(validBody)
    });
    const res = await worker.fetch(req, { ...validEnv, CIRCUIT_BREAKER_ACTIVE: 'true' }, {});
    assert.strictEqual(res.status, 429);
    const data = await res.json();
    assert.strictEqual(data.error, 'QUOTA_EXHAUSTED');
  });

  test('Payload exceeding size limit returns 413', async () => {
    const bigBody = { ...validBody, question: 'x'.repeat(12000) };
    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify(bigBody)
    });
    const res = await worker.fetch(req, validEnv, {});
    assert.strictEqual(res.status, 413);
  });

  test('Malformed JSON payload returns 400', async () => {
    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: '{ invalid json }'
    });
    const res = await worker.fetch(req, validEnv, {});
    assert.strictEqual(res.status, 400);
    const data = await res.json();
    assert.strictEqual(data.error, 'INVALID_JSON');
  });

  test('Unknown field in request body returns 400', async () => {
    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify({ ...validBody, promptText: 'hacking prompt' })
    });
    const res = await worker.fetch(req, validEnv, {});
    assert.strictEqual(res.status, 400);
    const data = await res.json();
    assert.strictEqual(data.error, 'INVALID_SCHEMA');
  });

  test('Unsupported version in body returns 400', async () => {
    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify({ ...validBody, version: '999' })
    });
    const res = await worker.fetch(req, validEnv, {});
    assert.strictEqual(res.status, 400);
  });

  test('Upstream provider 429 returns 429', async () => {
    const mockFetch = async () => new Response('Too Many Requests', { status: 429 });
    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify(validBody)
    });
    const res = await worker.fetch(req, validEnv, {}, mockFetch);
    assert.strictEqual(res.status, 429);
  });

  test('Upstream provider 500 returns 502', async () => {
    const mockFetch = async () => new Response('Internal Server Error', { status: 500 });
    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify(validBody)
    });
    const res = await worker.fetch(req, validEnv, {}, mockFetch);
    assert.strictEqual(res.status, 502);
  });

  test('Upstream provider timeout returns 504', async () => {
    const mockFetch = async () => {
      const err = new Error('The operation was aborted');
      err.name = 'AbortError';
      throw err;
    };
    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify(validBody)
    });
    const res = await worker.fetch(req, validEnv, {}, mockFetch);
    assert.strictEqual(res.status, 504);
  });

  test('Upstream model returning non-JSON raw text fails closed with 502', async () => {
    const mockFetch = async () => new Response(JSON.stringify({
      choices: [{ message: { content: 'Plain text without JSON formatting' } }]
    }), { status: 200, headers: { 'Content-Type': 'application/json' } });

    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify(validBody)
    });
    const res = await worker.fetch(req, validEnv, {}, mockFetch);
    assert.strictEqual(res.status, 502);
    const data = await res.json();
    assert.strictEqual(data.error, 'MALFORMED_PROVIDER_RESPONSE');
  });

  test('Upstream model missing explanation key fails closed with 502', async () => {
    const mockFetch = async () => new Response(JSON.stringify({
      choices: [{ message: { content: JSON.stringify({ answer: 'missing explanation field' }) } }]
    }), { status: 200, headers: { 'Content-Type': 'application/json' } });

    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify(validBody)
    });
    const res = await worker.fetch(req, validEnv, {}, mockFetch);
    assert.strictEqual(res.status, 502);
    const data = await res.json();
    assert.strictEqual(data.error, 'MALFORMED_PROVIDER_RESPONSE');
  });

  test('Upstream model returning wrong types or extra properties fails closed with 502', async () => {
    const mockFetch = async () => new Response(JSON.stringify({
      choices: [{ message: { content: JSON.stringify({ explanation: 'e4 controls center', extraProperty: 'unsupported' }) } }]
    }), { status: 200, headers: { 'Content-Type': 'application/json' } });

    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify(validBody)
    });
    const res = await worker.fetch(req, validEnv, {}, mockFetch);
    assert.strictEqual(res.status, 502);
    const data = await res.json();
    assert.strictEqual(data.error, 'MALFORMED_PROVIDER_RESPONSE');
  });

  test('Enforces OpenRouter provider routing to openai lowercase slug only with fallbacks disabled', async () => {
    const mockFetch = async (url, options) => {
      const payload = JSON.parse(options.body);
      assert.deepStrictEqual(payload.provider, {
        only: ['openai'],
        allow_fallbacks: false
      });

      return new Response(JSON.stringify({
        choices: [{ message: { content: JSON.stringify({ explanation: 'e4 is good.' }) } }]
      }), { status: 200, headers: { 'Content-Type': 'application/json' } });
    };

    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify(validBody)
    });
    const res = await worker.fetch(req, validEnv, {}, mockFetch);
    assert.strictEqual(res.status, 200);
  });

  test('Successful OpenRouter response with strict json_schema returns 200 and candidate grounding', async () => {
    const mockFetch = async (url, options) => {
      assert.strictEqual(url, 'https://openrouter.ai/api/v1/chat/completions');
      assert.strictEqual(options.headers['Authorization'], 'Bearer test-openrouter-key-12345');

      const payload = JSON.parse(options.body);
      assert.strictEqual(payload.model, 'openai/gpt-4.1-mini');
      assert.strictEqual(payload.response_format.type, 'json_schema');
      assert.strictEqual(payload.response_format.json_schema.strict, true);
      assert.deepStrictEqual(payload.provider, {
        only: ['openai'],
        allow_fallbacks: false
      });

      const mockOpenRouterJson = {
        id: 'gen-12345',
        choices: [
          {
            message: {
              role: 'assistant',
              content: JSON.stringify({ explanation: 'The move e4 controls central squares effectively and opens lines.' })
            }
          }
        ]
      };

      return new Response(JSON.stringify(mockOpenRouterJson), {
        status: 200,
        headers: { 'Content-Type': 'application/json' }
      });
    };

    const req = new Request('https://worker.local/', {
      method: 'POST',
      headers: validHeaders,
      body: JSON.stringify(validBody)
    });
    const res = await worker.fetch(req, validEnv, {}, mockFetch);
    assert.strictEqual(res.status, 200);

    const data = await res.json();
    assert.strictEqual(data.version, '1');
    assert.strictEqual(data.explanation, 'The move e4 controls central squares effectively and opens lines.');
    assert.deepStrictEqual(data.groundedCandidates, ['e4']);
    assert.ok(data.disclaimer.includes('Stockfish'));
  });
});
