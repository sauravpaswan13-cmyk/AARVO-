const OTP_ENDPOINT = 'https://control.msg91.com/api/v5/otp';
const LEGACY_OTP_ENDPOINT = 'https://api.msg91.com/api/sendotp.php';

export async function sendPhoneOtp({ phone, otp }) {
  const authKey = process.env.MSG91_AUTH_KEY;
  const templateId = process.env.MSG91_TEMPLATE_ID;
  const senderId = process.env.MSG91_SENDER_ID;
  if (!authKey || (!templateId && !senderId)) {
    const error = new Error('OTP_PROVIDER_NOT_CONFIGURED');
    error.code = 'OTP_PROVIDER_NOT_CONFIGURED';
    throw error;
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 10000);
  let response;
  try {
    if (templateId) {
      const params = new URLSearchParams({ template_id: templateId, mobile: `91${phone}`, authkey: authKey, otp, otp_length: '6', otp_expiry: '10' });
      response = await fetch(`${OTP_ENDPOINT}?${params.toString()}`, { method: 'POST', headers: { accept: 'application/json', 'Content-Type': 'application/json' }, body: '{}', signal: controller.signal });
    } else {
      const params = new URLSearchParams({ authkey: authKey, mobile: `91${phone}`, message: `Your AARVO verification code is ${otp}. It expires in 10 minutes.`, sender: senderId, otp, otp_length: '6', otp_expiry: '10' });
      response = await fetch(`${LEGACY_OTP_ENDPOINT}?${params.toString()}`, { method: 'GET', headers: { accept: 'application/json' }, signal: controller.signal });
    }
  } catch (cause) {
    const error = new Error(cause?.name === 'AbortError' ? 'OTP_PROVIDER_TIMEOUT' : 'OTP_DELIVERY_FAILED');
    error.code = error.message;
    error.cause = cause;
    throw error;
  } finally { clearTimeout(timeout); }

  const raw = await response.text();
  let data;
  try { data = JSON.parse(raw); } catch { data = { message: raw }; }
  if (!response.ok || String(data?.type || '').toLowerCase() !== 'success') {
    const error = new Error('OTP_DELIVERY_FAILED');
    error.providerResponse = data;
    throw error;
  }
  return data;
}
