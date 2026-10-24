import { test } from 'node:test';
import assert from 'node:assert/strict';
import { validateRequestSchema, extractAndValidateCandidateGrounding } from '../src/schema.js';
import { callOpenRouterApi } from '../src/openrouter.js';

const request = () => ({
  version: '2',
  fen: 'rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1',
  question: 'What is the continuation?',
  playedMove: 'e4',
  engineEvidence: [{ uci: 'e7e5', san: 'e5', scoreCp: -15, mateIn: null,
    depth: 18, pvSan: ['e5', 'Nf3', 'Nc6'], pvUci: ['e7e5', 'g1f3', 'b8c6'] }],
});

test('v2 accepts compact continuations and a starting position without a played move', () => {
  assert.equal(validateRequestSchema(request()).valid, true);
  assert.equal(validateRequestSchema({ ...request(), playedMove: '' }).valid, true);
});

test('v2 rejects absent, oversized, mismatched, or malformed continuations', () => {
  for (const change of [
    { pvSan: undefined }, { pvUci: [] }, { pvSan: ['d5', 'Nf3', 'Nc6'] },
    { pvUci: ['e7e5'] }, { pvSan: ['e5', 'invent a tactic', 'Nc6'] },
    { pvUci: ['e7e5', 'z9z8', 'b8c6'] },
    { pvSan: Array(13).fill('e5'), pvUci: Array(13).fill('e7e5') },
  ]) {
    const body = request();
    Object.assign(body.engineEvidence[0], change);
    assert.equal(validateRequestSchema(body).valid, false);
  }
});

test('grounding accepts supplied later moves and rejects unseen continuations', () => {
  assert.equal(extractAndValidateCandidateGrounding('e5 Nf3 Nc6', request().engineEvidence).hasUngroundedMoves, false);
  assert.equal(extractAndValidateCandidateGrounding('e5 Nf3 Bb5', request().engineEvidence).hasUngroundedMoves, true);
  const evidence = [{ san: 'Qh7#', uci: 'h5h7', pvSan: ['Qh7#'], pvUci: ['h5h7'] }];
  assert.equal(extractAndValidateCandidateGrounding('Qh7#', evidence).hasUngroundedMoves, false);
});

test('provider receives continuations, White perspective, and after-move limitations', async () => {
  let prompt;
  const result = await callOpenRouterApi({ apiKey: 'test-only', modelName: 'openai/test', body: request(),
    fetchImpl: async (_url, options) => {
      prompt = JSON.parse(options.body).messages.map(message => message.content).join('\n');
      return Response.json({ choices: [{ message: { content: JSON.stringify({ explanation: 'Consider e5 Nf3 Nc6.' }) } }] });
    },
  });
  assert.equal(result.error, undefined);
  assert.match(prompt, /e5 Nf3 Nc6/);
  assert.match(prompt, /-15 cp \(White perspective\)/);
  assert.match(prompt, /AFTER the played move/);
  assert.match(prompt, /Without before-move evidence/);
});

test('provider mate evidence names the winning side instead of a negative move count', async () => {
  const body = request();
  body.engineEvidence[0].mateIn = -3;
  body.engineEvidence[0].scoreCp = null;
  let prompt;
  await callOpenRouterApi({ apiKey: 'test-only', modelName: 'openai/test', body,
    fetchImpl: async (_url, options) => {
      prompt = JSON.parse(options.body).messages[1].content;
      return Response.json({ choices: [{ message: { content: JSON.stringify({ explanation: 'Black can force mate.' }) } }] });
    },
  });
  assert.match(prompt, /Black mates in 3/);
  assert.doesNotMatch(prompt, /Mate in -3/);
});
