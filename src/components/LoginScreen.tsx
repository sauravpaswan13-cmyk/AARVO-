import React, { useState } from 'react';

interface LoginScreenProps {
  onSuccess?: () => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ onSuccess }) => {
  const [phone, setPhone] = useState('');
  const [otpMode, setOtpMode] = useState(false);
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  // Exact design tokens from com.aarvo.AarvoScreenDesign
  // authPurple: #4B16D8, authDeepPurple: #32108E, authOrange: #FF7A00
  // authPageBackground: #F7F5FF, authSoftBackground: #F0ECFF

  const handleSendOtp = (e: React.FormEvent) => {
    e.preventDefault();
    if (phone.length !== 10) {
      setError('Enter a valid 10-digit mobile number.');
      return;
    }
    setError('');
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setOtpMode(true);
    }, 600);
  };

  const handleVerifyOtp = (e: React.FormEvent) => {
    e.preventDefault();
    if (otp.length < 4) {
      setError('Please enter the OTP.');
      return;
    }
    setError('');
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      setIsLoggedIn(true);
      setTimeout(() => {
        onSuccess?.();
      }, 500);
    }, 700);
  };

  return (
    <div className="min-h-screen bg-[#F7F5FF] flex flex-col items-center justify-between p-5 sm:p-7 select-none">
      <div className="w-full max-w-[420px] flex flex-col flex-1 justify-between">
        {/* Top Header: Logo + AARVO Wordmark (from PhoneAuthActivity) */}
        <div>
          <div className="flex items-center gap-3 pt-2">
            <div className="w-12 h-12 flex items-center justify-center">
              <img
                src="/src/assets/aarvo/aarvo_logo_transparent.png"
                alt="AARVO logo"
                className="w-full h-full object-contain mix-blend-multiply"
              />
            </div>
            <div className="flex flex-col">
              <span className="text-[27px] font-extrabold text-[#FF7A00] tracking-wide leading-none font-display">
                AARVO
              </span>
              <span className="text-[11px] text-[#77718B] mt-0.5">
                Shop Smart • Live Better
              </span>
            </div>
          </div>

          {/* Heading */}
          <div className="mt-9 text-left">
            <h1 className="text-[30px] font-extrabold text-[#171329] leading-tight tracking-tight font-display">
              {!otpMode ? 'Login to AARVO' : 'Verify your mobile'}
            </h1>
            <p className="text-[14px] text-[#706A80] mt-1.5 font-normal">
              {!otpMode
                ? 'Login securely with your mobile number'
                : 'Enter the OTP sent to your mobile number'}
            </p>
          </div>

          {/* Auth Card (from PhoneAuthActivity) */}
          <div className="mt-6 bg-white rounded-[28px] p-[22px] shadow-[0_8px_30px_rgba(75,22,216,0.06)] border border-purple-50">
            {!isLoggedIn ? (
              !otpMode ? (
                /* Step 1: Mobile Number Entry */
                <form onSubmit={handleSendOtp} className="space-y-4">
                  <label className="text-[14px] font-bold text-[#28223B] block">
                    Mobile number
                  </label>

                  <div className="flex items-center h-[62px] rounded-[18px] bg-[#F0ECFF] border border-[#4B16D8]/15 px-3">
                    <div className="w-[72px] pl-2 flex flex-col justify-center">
                      <span className="text-[10px] font-bold text-[#817A93]">INDIA</span>
                      <span className="text-[17px] font-extrabold text-[#32108E] font-mono leading-none">
                        +91
                      </span>
                    </div>

                    <div className="w-[1px] h-[34px] bg-[#D8D1EE] mx-2" />

                    <input
                      type="tel"
                      maxLength={10}
                      autoFocus
                      placeholder="Enter 10-digit mobile number"
                      value={phone}
                      onChange={(e) => {
                        setPhone(e.target.value.replace(/\D/g, '').slice(0, 10));
                        if (error) setError('');
                      }}
                      className="flex-1 bg-transparent text-[14px] font-semibold text-[#211A32] placeholder-[#9A94A7] outline-none font-mono"
                    />
                  </div>

                  {error && (
                    <p className="text-xs text-rose-500 font-medium">{error}</p>
                  )}

                  <button
                    type="submit"
                    disabled={phone.length !== 10 || loading}
                    className="w-full h-[56px] rounded-[17px] bg-[#4B16D8] hover:bg-[#32108E] disabled:bg-[#D8D2E7] text-white font-extrabold text-[16px] transition-all flex items-center justify-center shadow-md disabled:cursor-not-allowed"
                  >
                    {loading ? (
                      <span className="inline-block w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    ) : (
                      'Continue'
                    )}
                  </button>

                  <p className="text-[11px] text-[#827B90] leading-relaxed pt-1">
                    New to AARVO? Your account is created securely after verification.
                  </p>
                </form>
              ) : (
                /* Step 2: OTP Verification */
                <form onSubmit={handleVerifyOtp} className="space-y-4">
                  <div className="flex items-center justify-between">
                    <label className="text-[14px] font-bold text-[#28223B]">
                      One-time password
                    </label>
                    <span className="text-[13px] text-[#706A80] font-semibold font-mono">
                      +91 {phone}
                    </span>
                  </div>

                  <div className="h-[60px] rounded-[18px] bg-[#F0ECFF] flex items-center px-4">
                    <input
                      type="text"
                      maxLength={6}
                      autoFocus
                      placeholder="Enter OTP (e.g. 123456)"
                      value={otp}
                      onChange={(e) => {
                        setOtp(e.target.value.replace(/\D/g, '').slice(0, 6));
                        if (error) setError('');
                      }}
                      className="w-full bg-transparent text-[18px] font-bold text-center tracking-[0.3em] text-[#211A32] placeholder-[#9A94A7] outline-none font-mono"
                    />
                  </div>

                  {error && (
                    <p className="text-xs text-rose-500 font-medium">{error}</p>
                  )}

                  <button
                    type="submit"
                    disabled={otp.length < 4 || loading}
                    className="w-full h-[56px] rounded-[17px] bg-[#4B16D8] hover:bg-[#32108E] disabled:bg-[#D8D2E7] text-white font-extrabold text-[16px] transition-all flex items-center justify-center shadow-md disabled:cursor-not-allowed"
                  >
                    {loading ? (
                      <span className="inline-block w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    ) : (
                      'Verify & Login'
                    )}
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setOtpMode(false);
                      setOtp('');
                    }}
                    className="w-full text-center text-xs font-bold text-[#4B16D8] hover:underline pt-1"
                  >
                    Change Mobile Number
                  </button>
                </form>
              )
            ) : (
              /* Success State */
              <div className="py-8 text-center space-y-2">
                <div className="w-12 h-12 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center mx-auto text-xl font-bold">
                  ✓
                </div>
                <h3 className="text-lg font-bold text-[#171329]">Login Verified</h3>
                <p className="text-xs text-[#706A80]">
                  Welcome to AARVO Marketplace (+91 {phone})
                </p>
              </div>
            )}
          </div>

          {/* Guest Entry Button (from PhoneAuthActivity) */}
          {!otpMode && !isLoggedIn && (
            <button
              type="button"
              onClick={() => {
                setIsLoggedIn(true);
                onSuccess?.();
              }}
              className="mt-3.5 w-full h-[56px] rounded-[17px] border border-[#4B16D8]/65 bg-transparent hover:bg-purple-50 text-[#32108E] font-bold text-[15px] transition-colors"
            >
              Continue as Guest
            </button>
          )}
        </div>

        {/* Footer (from PhoneAuthActivity) */}
        <div className="pt-6 pb-2 text-center">
          <div className="flex items-center justify-center gap-3 text-[11px] font-bold text-[#5F586D]">
            <span>✓ Secure</span>
            <span className="text-[#FF7A00]">•</span>
            <span>✓ Fast</span>
            <span className="text-[#FF7A00]">•</span>
            <span>✓ Trusted</span>
          </div>
          <p className="text-[11px] text-[#8A8495] mt-2">
            Your mobile number is securely protected
          </p>
        </div>
      </div>
    </div>
  );
};
