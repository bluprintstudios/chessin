/**
 * Google Play Developer API Purchase Verification
 */

async function getGoogleAccessToken(credentialsJson, fetchImpl) {
  const creds = JSON.parse(credentialsJson);
  const header = { alg: 'RS256', typ: 'JWT' };
  const iat = Math.floor(Date.now() / 1000);
  const exp = iat + 3600;
  const payload = {
    iss: creds.client_email,
    scope: 'https://www.googleapis.com/auth/androidpublisher',
    aud: 'https://oauth2.googleapis.com/token',
    exp,
    iat
  };

  const base64UrlEncode = (obj) => btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  const encodedHeader = base64UrlEncode(header);
  const encodedPayload = base64UrlEncode(payload);
  const dataToSign = `${encodedHeader}.${encodedPayload}`;

  const pemHeader = '-----BEGIN PRIVATE KEY-----';
  const pemFooter = '-----END PRIVATE KEY-----';
  // Use a regex to strip newlines safely
  const pem = creds.private_key.replace(pemHeader, '').replace(pemFooter, '').replace(/\r?\n/g, '').trim();
  const binaryDer = Uint8Array.from(atob(pem), c => c.charCodeAt(0));

  const cryptoKey = await crypto.subtle.importKey(
    'pkcs8',
    binaryDer.buffer,
    { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' },
    false,
    ['sign']
  );

  const signature = await crypto.subtle.sign(
    'RSASSA-PKCS1-v1_5',
    cryptoKey,
    new TextEncoder().encode(dataToSign)
  );

  const encodedSignature = btoa(String.fromCharCode(...new Uint8Array(signature)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');

  const jwt = `${dataToSign}.${encodedSignature}`;

  const response = await fetchImpl('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${jwt}`
  });

  if (!response.ok) throw new Error('Failed to fetch access token');
  const data = await response.json();
  return data.access_token;
}

export async function verifyPlayPurchase(token, sku, packageName, credentialsJson, fetchImpl = fetch) {
  if (!token || !sku || !packageName) {
    return { valid: false, error: 'MISSING_PARAMETERS' };
  }
  if (!credentialsJson) {
    return { valid: false, error: 'MISSING_CREDENTIALS' };
  }

  let accessToken;
  try {
    accessToken = await getGoogleAccessToken(credentialsJson, fetchImpl);
  } catch (err) {
    return { valid: false, error: 'AUTH_ERROR' };
  }

  const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${packageName}/purchases/products/${sku}/tokens/${token}`;

  try {
    const response = await fetchImpl(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'Accept': 'application/json'
      }
    });

    if (!response.ok) {
      if (response.status === 404 || response.status === 400) return { valid: false, error: 'PURCHASE_INVALID' };
      return { valid: false, error: `PROVIDER_ERROR_${response.status}` };
    }

    const data = await response.json();

    // purchaseState: 0 = Purchased, 1 = Canceled, 2 = Pending
    if (data.purchaseState === 0) {
      return { valid: true };
    } else if (data.purchaseState === 1) {
      return { valid: false, error: 'PURCHASE_CANCELED' };
    } else if (data.purchaseState === 2) {
      return { valid: false, error: 'PURCHASE_PENDING' };
    }

    return { valid: false, error: 'PURCHASE_INVALID' };
  } catch (err) {
    return { valid: false, error: 'CONNECTION_ERROR' };
  }
}
