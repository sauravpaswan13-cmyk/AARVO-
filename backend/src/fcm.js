import { createSign } from 'node:crypto';

const b64url = (value) => Buffer.from(value).toString('base64url');

function serviceAccount() {
  if (!process.env.FIREBASE_SERVICE_ACCOUNT_JSON) return null;
  try {
    const raw = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON);
    if (!raw.project_id || !raw.client_email || !raw.private_key) return null;
    return raw;
  } catch {
    return null;
  }
}

async function accessToken(account) {
  const now = Math.floor(Date.now() / 1000);
  const header = b64url(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
  const claim = b64url(JSON.stringify({
    iss: account.client_email,
    scope: 'https://www.googleapis.com/auth/firebase.messaging',
    aud: 'https://oauth2.googleapis.com/token',
    iat: now,
    exp: now + 3600
  }));
  const unsigned = header + '.' + claim;
  const signer = createSign('RSA-SHA256');
  signer.update(unsigned);
  const assertion = unsigned + '.' + signer.sign(account.private_key, 'base64url');
  const response = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
      assertion
    })
  });
  if (!response.ok) throw new Error('FCM_OAUTH_TOKEN_FAILED');
  const json = await response.json();
  if (!json.access_token) throw new Error('FCM_OAUTH_TOKEN_MISSING');
  return json.access_token;
}

export async function sendRiderPush({ token, title, body, orderId, notificationId }) {
  const account = serviceAccount();
  if (!account || !token) return { sent: false, configured: false };

  const access = await accessToken(account);
  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(account.project_id)}/messages:send`,
    {
      method: 'POST',
      headers: {
        authorization: `Bearer ${access}`,
        'content-type': 'application/json'
      },
      body: JSON.stringify({
        message: {
          token,
          notification: { title, body },
          data: {
            type: 'RIDER_ORDER',
            orderId: String(orderId || ''),
            notificationId: String(notificationId || '')
          },
          android: {
            priority: 'HIGH',
            notification: { channel_id: 'aarvo_rider' }
          }
        }
      })
    }
  );
  if (!response.ok) {
    const detail = await response.text();
    const error = new Error('FCM_SEND_FAILED');
    error.detail = detail.slice(0, 1000);
    throw error;
  }
  return { sent: true, configured: true };
}

export function fcmConfigured() {
  return Boolean(serviceAccount());
}
