import React, { useEffect, useState } from 'react';

interface SplashScreenProps {
  onFinish: () => void;
  duration?: number; // 1300ms = 1.3 seconds
}

export const SplashScreen: React.FC<SplashScreenProps> = ({
  onFinish,
  duration = 1300,
}) => {
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const startTime = Date.now();
    const interval = setInterval(() => {
      const elapsed = Date.now() - startTime;
      const pct = Math.min(100, Math.round((elapsed / duration) * 100));
      setProgress(pct);

      if (elapsed >= duration) {
        clearInterval(interval);
        onFinish();
      }
    }, 16);

    return () => clearInterval(interval);
  }, [duration, onFinish]);

  return (
    <div
      id="aarvo-splash-screen"
      className="fixed inset-0 z-50 flex items-center justify-center bg-[#F7F5FF] select-none overflow-hidden"
    >
      {/* Mobile Frame with matching #F7F5FF lavender background, no corner frames */}
      <div className="relative w-full h-full max-w-[440px] max-h-[920px] bg-[#F7F5FF] flex flex-col items-center justify-between p-8 sm:p-12 overflow-hidden">
        {/* Top spacing */}
        <div className="w-full h-12" />

        {/* Center Brand Lockup */}
        <div className="relative z-10 flex flex-col items-center justify-center my-auto text-center">
          {/* Static 3D Ribbon 'A' Logo - matching Login Screen logo */}
          <div className="relative w-44 h-44 sm:w-52 sm:h-52 mb-6 flex items-center justify-center">
            <img
              src="/src/assets/aarvo/aarvo_logo_transparent.png"
              alt="AARVO Logo"
              loading="eager"
              decoding="sync"
              className="w-full h-full object-contain filter drop-shadow-[0_12px_28px_rgba(255,140,0,0.18)]"
            />
          </div>

          {/* AARVO Wordmark with flowing soft Orange Flash light effect */}
          <h1 className="text-4xl sm:text-5xl font-black tracking-tight font-display select-none orange-flash-text">
            AARVO
          </h1>

          {/* Tagline */}
          <p className="mt-2 text-xs font-semibold tracking-[0.24em] text-[#706A80] uppercase select-none">
            SHOP SMART &bull; LIVE BETTER
          </p>

          {/* Pill Progress Bar & LOADING... text */}
          <div className="mt-12 sm:mt-14 w-52 sm:w-60 flex flex-col items-center">
            <div className="w-full h-2 bg-[#E6E0F8] rounded-full overflow-hidden p-[1px]">
              <div
                className="h-full rounded-full bg-gradient-to-r from-[#2563EB] via-[#7C3AED] to-[#FF7A00] transition-all ease-linear"
                style={{ width: `${progress}%` }}
              />
            </div>

            <span className="mt-4 text-[11px] sm:text-xs font-semibold tracking-[0.32em] text-[#706A80] uppercase">
              LOADING...
            </span>
          </div>
        </div>

        {/* Bottom spacing */}
        <div className="w-full h-12" />
      </div>
    </div>
  );
};
