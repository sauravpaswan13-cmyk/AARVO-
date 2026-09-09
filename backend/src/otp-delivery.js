const OTP_ENDPOINT = 'https://control.msg91.com/api/v5/otp';
const LEGACY_OTP_ENDPOINT = 'https://api.msg91.com/api/sendotp.php';

function msg91Mobile(phone) {
  const digits = String(phone || '').replace(/\D/g, '');
  return digits.startsWith('91') && digits.length === 12 ? digits : `91${digits}`;
}

async function requestOtp({ endpoint, params, method, headers, body, signal }) {
  return fetch(`${endpoint}${method === 'GET' ? `?${params.toString()}` : `?${params.toString()}`}`, {
    method,
    headers,
    body,
    signal,
  });
}

export async function sendPhoneOtp({ phone, otp }) {
  // AARVO now uses the MSG91 Secure OTP Widget on the Android client.
  // In widget mode MSG91 generates, sends and verifies the OTP itself, so the
  // legacy server-side sender must not block auth with template/sender config.
  if (process.env.MSG91_WIDGET_MODE === 'true') {
    return { delivered: false, widget: true };
  }

  const authKey = process.env.MSG91_AUTH_KEY;
  const templateId = process.env.MSG91_TEMPLATE_ID;
  const senderId = process.env.MSG91_SENDER_ID;
  if (!authKey || (!templateId && !senderId)) {
    const error = new Error('OTP_PROVIDER_NOT_CONFIGURED');
    error.code = 'OTP_PROVIDER_NOT_CONFIGURED';
    throw error;
  }

  const mobile = msg91Mobile(phone);
  let lastError;

  for (let attempt = 1; attempt <= 2; attempt += 1) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 10000);
    try {
      let response;
      if (templateId) {
        const params = new URLSearchParams({
          template_id: templateId,
          mobile,
          authkey: authKey,
          otp: String(otp),
          otp_length: '6',
          otp_expiry: '10',
        });
        response = await requestOtp({
          endpoint: OTP_ENDPOINT,
          params,
          method: 'POST',
          headers: { accept: 'application/json', 'Content-Type': 'application/json' },
          body: '{}',
          signal: controller.signal,
        });
      } else {
        const params = new URLSearchParams({
          authkey: authKey,
          mobile,
          message: `Your AARVO verification code is ${otp}. It expires in 10 minutes.`,
          sender: senderId,
          otp: String(otp),
          otp_length: '6',
          otp_expiry: '10',
        });
        response = await requestOtp({
          endpoint: LEGACY_OTP_ENDPOINT,
          params,
          method: 'GET',
          headers: { accept: 'application/json' },
          signal: controller.signal,
        });
      }

      const raw = await response.text();
      let data;
      try { data = JSON.parse(raw); } catch { data = { message: raw }; }

      if (!response.ok || String(data?.type || '').toLowerCase() !== 'success') {
        const error = new Error('OTP_DELIVERY_FAILED');
        error.code = 'OTP_DELIVERY_FAILED';
        error.providerStatus = response.status;
        error.providerResponse = data;
        throw error;
      }

      return data;
    } catch (cause) {
      lastError = cause;
      const transient = cause?.name === 'AbortError' || cause?.code === 'ECONNRESET' || cause?.code === 'ETIMEDOUT' || cause?.message === 'fetch failed';
      if (!transient || attempt === 2) {
        if (cause?.message === 'OTP_DELIVERY_FAILED') throw cause;
        const error = new Error(cause?.name === 'AbortError' ? 'OTP_PROVIDER_TIMEOUT' : 'OTP_DELIVERY_FAILED');
        error.code = error.message;
        error.cause = cause;
        throw error;
      }
      await new Promise((resolve) => setTimeout(resolve, 400));
    } finally {
      clearTimeout(timeout);
    }
  }

  const error = new Error('OTP_DELIVERY_FAILED');
  error.code = 'OTP_DELIVERY_FAILED';
  error.cause = lastError;
  throw error;
}
