/**
 * Schema definitions and validation for Chess Review AI Gateway.
 */

const ALLOWED_REQUEST_KEYS = new Set([
  'version',
  'fen',
  'question',
  'playedMove',
  'engineEvidence'
]);

const ALLOWED_EVIDENCE_KEYS = new Set([
  'uci',
  'san',
  'scoreCp',
  'mateIn',
  'depth',
  'pvSan',
  'pvUci'
]);

export function validateRequestSchema(body) {
  if (typeof body !== 'object' || body === null || Array.isArray(body)) {
    return { valid: false, error: 'Request body must be a JSON object' };
  }

  // Reject unknown top-level keys
  const keys = Object.keys(body);
  for (const key of keys) {
    if (!ALLOWED_REQUEST_KEYS.has(key)) {
      return { valid: false, error: `Unknown field in request body: ${key}` };
    }
  }

  // Check version
  if (body.version !== '1' && body.version !== '2') {
    return { valid: false, error: 'Unsupported schema version. Must be "1" or "2"' };
  }

  // FEN validation
  if (typeof body.fen !== 'string' || body.fen.trim().length === 0 || body.fen.length > 120) {
    return { valid: false, error: 'Invalid or missing "fen" string (max 120 chars)' };
  }

  // Question validation
  if (typeof body.question !== 'string' || body.question.trim().length === 0 || body.question.length > 500) {
    return { valid: false, error: 'Invalid or missing "question" string (max 500 chars)' };
  }

  // PlayedMove validation
  if (typeof body.playedMove !== 'string' || (body.version === '1' && body.playedMove.trim().length === 0) || body.playedMove.length > 20) {
    return { valid: false, error: 'Invalid or missing "playedMove" string (max 20 chars)' };
  }

  // EngineEvidence validation
  if (!Array.isArray(body.engineEvidence) || body.engineEvidence.length > 5) {
    return { valid: false, error: '"engineEvidence" must be an array of up to 5 items' };
  }

  for (let i = 0; i < body.engineEvidence.length; i++) {
    const item = body.engineEvidence[i];
    if (typeof item !== 'object' || item === null || Array.isArray(item)) {
      return { valid: false, error: `engineEvidence[${i}] must be an object` };
    }

    const itemKeys = Object.keys(item);
    for (const ik of itemKeys) {
      if (!ALLOWED_EVIDENCE_KEYS.has(ik)) {
        return { valid: false, error: `Unknown field in engineEvidence[${i}]: ${ik}` };
      }
    }

    if (typeof item.uci !== 'string' || item.uci.length > 10) {
      return { valid: false, error: `Invalid "uci" in engineEvidence[${i}]` };
    }
    if (typeof item.san !== 'string' || item.san.length > 10) {
      return { valid: false, error: `Invalid "san" in engineEvidence[${i}]` };
    }
    if (item.scoreCp !== null && typeof item.scoreCp !== 'number') {
      return { valid: false, error: `Invalid "scoreCp" in engineEvidence[${i}]` };
    }
    if (item.mateIn !== null && typeof item.mateIn !== 'number') {
      return { valid: false, error: `Invalid "mateIn" in engineEvidence[${i}]` };
    }
    if (typeof item.depth !== 'number' || item.depth < 1) {
      return { valid: false, error: `Invalid "depth" in engineEvidence[${i}]` };
    }
    // Version 2 carries a compact principal variation, including its first move.
    if (body.version === '2' || item.pvSan !== undefined || item.pvUci !== undefined) {
      const validSan = Array.isArray(item.pvSan) && item.pvSan.length >= 1 && item.pvSan.length <= 12 &&
        item.pvSan.every(move => typeof move === 'string' && /^(?:O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?)[+#]?$/.test(move));
      const validUci = Array.isArray(item.pvUci) && item.pvUci.length >= 1 && item.pvUci.length <= 12 &&
        item.pvUci.every(move => typeof move === 'string' && /^[a-h][1-8][a-h][1-8][qrbn]?$/.test(move));
      if (!validSan || !validUci || item.pvSan.length !== item.pvUci.length ||
          item.pvSan[0] !== item.san || item.pvUci[0] !== item.uci) {
        return { valid: false, error: `Invalid principal variation in engineEvidence[${i}]` };
      }
    }
  }

  return { valid: true };
}

/**
 * Validates that candidate moves mentioned in the explanation text exist in the supplied engine evidence.
 */
export function extractAndValidateCandidateGrounding(explanationText, engineEvidence) {
  const allowedMoves = new Set();
  const normalize = move => move.replace(/[+#]$/, '');
  for (const ev of engineEvidence) {
    if (ev.san) allowedMoves.add(normalize(ev.san));
    if (ev.uci) allowedMoves.add(ev.uci);
    for (const move of ev.pvSan || []) allowedMoves.add(normalize(move));
    for (const move of ev.pvUci || []) allowedMoves.add(move);
  }

  // Find SAN-like chess move tokens in text (e.g., e4, Nf3, O-O, Qxd5+)
  const moveRegex = /\b(?:[a-h][1-8][a-h][1-8][qrbn]?|O-O(?:-O)?|[KQRBN]?[a-h]?[1-8]?x?[a-h][1-8](?:=[QRBN])?)\b[+#]?/g;
  const matches = explanationText.match(moveRegex) || [];

  const groundedCandidates = [];
  const ungroundedMoves = [];

  for (const m of matches) {
    if (allowedMoves.has(normalize(m))) {
      if (!groundedCandidates.includes(m)) {
        groundedCandidates.push(m);
      }
    } else {
      if (!ungroundedMoves.includes(m)) {
        ungroundedMoves.push(m);
      }
    }
  }

  return {
    groundedCandidates,
    hasUngroundedMoves: ungroundedMoves.length > 0,
    ungroundedMoves
  };
}
