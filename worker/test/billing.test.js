import { test, describe, before, after } from 'node:test';
import assert from 'node:assert';
import { verifyPlayPurchase } from '../src/billing.js';

describe('Google Play Purchase Verification Tests (Server-side)', () => {
  const dummySku = 'premium_lifetime';
  const dummyPackage = 'com.pro.chess_review';
  const dummyCreds = JSON.stringify({
    client_email: 'test@example.com',
    private_key: '-----BEGIN PRIVATE KEY-----\ndmFsaWRiYXNlNjQ=\n-----END PRIVATE KEY-----'
  });

  let originalImportKey;
  let originalSign;

  before(() => {
    originalImportKey = globalThis.crypto.subtle.importKey;
    originalSign = globalThis.crypto.subtle.sign;

    // Stub WebCrypto methods so we don't need a valid RSA key string to test the OAuth flow structure
    globalThis.crypto.subtle.importKey = async () => ({ type: 'private' });
    globalThis.crypto.subtle.sign = async () => new Uint8Array([1, 2, 3]);
  });

  after(() => {
    globalThis.crypto.subtle.importKey = originalImportKey;
    globalThis.crypto.subtle.sign = originalSign;
  });

  const mockFetch = async (url, options) => {
    if (url === 'https://oauth2.googleapis.com/token') {
      return new Response(JSON.stringify({ access_token: 'mock_access_token' }), { status: 200 });
    }
    if (url.includes('/tokens/valid_token')) {
      return new Response(JSON.stringify({ purchaseState: 0 }), { status: 200 });
    }
    if (url.includes('/tokens/canceled_token')) {
      return new Response(JSON.stringify({ purchaseState: 1 }), { status: 200 });
    }
    if (url.includes('/tokens/pending_token')) {
      return new Response(JSON.stringify({ purchaseState: 2 }), { status: 200 });
    }
    if (url.includes('/tokens/invalid_token')) {
      return new Response(JSON.stringify({ error: 'invalid' }), { status: 404 });
    }
    if (url.includes('/tokens/error_token')) {
      return new Response('Internal error', { status: 500 });
    }
    return new Response('Not found', { status: 404 });
  };

  test('Missing parameters fails verification', async () => {
    const result = await verifyPlayPurchase(null, dummySku, dummyPackage, dummyCreds, mockFetch);
    assert.strictEqual(result.valid, false);
    assert.strictEqual(result.error, 'MISSING_PARAMETERS');
  });

  test('Missing credentials fails closed', async () => {
    const result = await verifyPlayPurchase('token', dummySku, dummyPackage, null, mockFetch);
    assert.strictEqual(result.valid, false);
    assert.strictEqual(result.error, 'MISSING_CREDENTIALS');
  });

  test('Valid token returns valid: true', async () => {
    const result = await verifyPlayPurchase('valid_token', dummySku, dummyPackage, dummyCreds, mockFetch);
    assert.strictEqual(result.valid, true);
  });

  test('Canceled token returns PURCHASE_CANCELED', async () => {
    const result = await verifyPlayPurchase('canceled_token', dummySku, dummyPackage, dummyCreds, mockFetch);
    assert.strictEqual(result.valid, false);
    assert.strictEqual(result.error, 'PURCHASE_CANCELED');
  });

  test('Pending token returns PURCHASE_PENDING', async () => {
    const result = await verifyPlayPurchase('pending_token', dummySku, dummyPackage, dummyCreds, mockFetch);
    assert.strictEqual(result.valid, false);
    assert.strictEqual(result.error, 'PURCHASE_PENDING');
  });

  test('Invalid token (404) returns PURCHASE_INVALID', async () => {
    const result = await verifyPlayPurchase('invalid_token', dummySku, dummyPackage, dummyCreds, mockFetch);
    assert.strictEqual(result.valid, false);
    assert.strictEqual(result.error, 'PURCHASE_INVALID');
  });

  test('Upstream 500 error returns PROVIDER_ERROR_500', async () => {
    const result = await verifyPlayPurchase('error_token', dummySku, dummyPackage, dummyCreds, mockFetch);
    assert.strictEqual(result.valid, false);
    assert.strictEqual(result.error, 'PROVIDER_ERROR_500');
  });

  test('Auth failure mapping', async () => {
    const badFetch = async (url) => new Response('Error', { status: 401 });
    const result = await verifyPlayPurchase('valid_token', dummySku, dummyPackage, dummyCreds, badFetch);
    assert.strictEqual(result.valid, false);
    assert.strictEqual(result.error, 'AUTH_ERROR');
  });
});
