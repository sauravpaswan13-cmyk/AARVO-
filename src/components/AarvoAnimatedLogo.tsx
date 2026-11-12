import React, { useState, useEffect, useRef } from 'react';
import { haptics } from '../utils/haptics';

interface AarvoAnimatedLogoProps {
  onClick?: () => void;
  className?: string;
  size?: 'sm' | 'md' | 'lg';
}

export const AarvoAnimatedLogo: React.FC<AarvoAnimatedLogoProps> = ({
  onClick,
  className = '',
  size = 'md',
}) => {
  // Animation phases: 'idle' | 'squash' | 'jump' | 'write' | 'glow'
  const [phase, setPhase] = useState<'idle' | 'squash' | 'jump' | 'write' | 'glow'>('idle');
  const [hasStarted, setHasStarted] = useState(false);
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  const runAnimationSequence = () => {
    // 1. Squash trolley & slide it back/left (0ms)
    setPhase('squash');
    haptics.light();

    // 2. Ribbon Logo jumps up out of trolley (320ms)
    setTimeout(() => {
      setPhase('jump');
      haptics.selection();
    }, 320);

    // 3. Ribbon lands between trolley and text, orange AARVO writes with fade (850ms)
    setTimeout(() => {
      setPhase('write');
      haptics.impact();
    }, 850);

    // 4. Glow showcase hold (1600ms)
    setTimeout(() => {
      setPhase('glow');
    }, 1600);

    // 5. Return to settled presentation (7500ms)
    setTimeout(() => {
      setPhase('idle');
    }, 7500);
  };

  useEffect(() => {
    // User request: "user wahi rahe to login karne ke 5 second baad wo dikhe"
    timerRef.current = setTimeout(() => {
      setHasStarted(true);
      runAnimationSequence();
    }, 5000);

    // User request: "Uske Baad Har 40 Seconds Par Waisa Hee Hota Rahe"
    intervalRef.current = setInterval(() => {
      runAnimationSequence();
    }, 40000);

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, []);

  const handleManualClick = () => {
    haptics.selection();
    runAnimationSequence();
    if (onClick) onClick();
  };

  // Size scaling
  const trolleySize = size === 'sm' ? 24 : size === 'lg' ? 36 : 30;
  const ribbonSize = size === 'sm' ? 24 : size === 'lg' ? 36 : 30;
  const textSize = size === 'sm' ? 'text-lg' : size === 'lg' ? 'text-3xl' : 'text-2xl sm:text-[26px]';

  return (
    <div
      onClick={handleManualClick}
      className={`inline-flex items-center select-none cursor-pointer group relative py-1 px-1.5 transition-transform active:scale-95 ${className}`}
      title="AARVO Official Brand Logo (Tap to replay animation)"
      aria-label="AARVO Logo"
    >
      {/* Container with dynamic gap to make room when ribbon icon jumps and lands */}
      <div className="flex items-center relative">
        
        {/* =================================================================== */}
        {/* 1. ORANGE SHOPPING TROLLEY (Compresses down & slides back)          */}
        {/* =================================================================== */}
        <div
          className="relative shrink-0 transition-all duration-350 ease-out z-10"
          style={{
            transform:
              phase === 'squash'
                ? 'translateX(-12px) scaleY(0.78) scaleX(1.16) rotate(-3deg)'
                : phase === 'jump'
                ? 'translateX(-10px) scaleY(0.92) scaleX(1.04)'
                : phase === 'write' || phase === 'glow'
                ? 'translateX(-4px) scale(1)'
                : 'translateX(0px) scale(1)',
            transformOrigin: 'bottom center',
          }}
        >
          {/* Orange Shopping Trolley SVG matching reference */}
          <svg
            width={trolleySize}
            height={trolleySize}
            viewBox="0 0 40 40"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            className="filter drop-shadow-[0_2px_4px_rgba(255,87,34,0.25)]"
          >
            {/* Basket wireframe */}
            <path
              d="M7 9H11.5L16.2 24.5C16.4 25.2 17 25.7 17.8 25.7H31.5C32.3 25.7 32.9 25.2 33.1 24.5L36.2 13.2C36.4 12.4 35.8 11.7 35 11.7H13.2"
              stroke="#FF5722"
              strokeWidth="2.8"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
            {/* Inner Basket Grid lines */}
            <path
              d="M15.5 16H34M17.5 20.8H32M21 12V24M27 12V24"
              stroke="#FF7043"
              strokeWidth="1.8"
              strokeLinecap="round"
            />
            {/* Wheel Left */}
            <circle
              cx="18.5"
              cy="31.5"
              r="3.2"
              fill="#FF5722"
              className={phase === 'squash' ? 'animate-pulse' : ''}
            />
            <circle cx="18.5" cy="31.5" r="1.3" fill="white" />
            {/* Wheel Right */}
            <circle
              cx="29.5"
              cy="31.5"
              r="3.2"
              fill="#FF5722"
              className={phase === 'squash' ? 'animate-pulse' : ''}
            />
            <circle cx="29.5" cy="31.5" r="1.3" fill="white" />
          </svg>

          {/* Spark dust particle effect on squash */}
          {phase === 'squash' && (
            <span className="absolute -left-1.5 bottom-0 text-[10px] animate-ping text-orange-500">
              ✨
            </span>
          )}
        </div>

        {/* =================================================================== */}
        {/* 2. REAL AARVO 3D RIBBON LOGO (Jumps out of trolley, lands between)   */}
        {/* =================================================================== */}
        <div
          className="relative shrink-0 flex items-center justify-center transition-all duration-400 z-20"
          style={{
            width: ribbonSize,
            height: ribbonSize,
            marginLeft: phase === 'idle' ? '2px' : '4px',
            marginRight: phase === 'idle' ? '4px' : '8px',
            transform:
              phase === 'squash'
                ? 'translateY(2px) scale(0.65) opacity-40'
                : phase === 'jump'
                ? 'translateY(-30px) scale(1.3) rotate(14deg)'
                : phase === 'write'
                ? 'translateY(0px) scale(1.08) rotate(-2deg)'
                : phase === 'glow'
                ? 'translateY(0px) scale(1.02) rotate(0deg)'
                : 'translateY(0px) scale(1) rotate(0deg)',
            opacity: phase === 'squash' ? 0.6 : 1,
            filter:
              phase === 'jump' || phase === 'write' || phase === 'glow'
                ? 'drop-shadow(0 4px 12px rgba(255,100,0,0.55)) drop-shadow(0 0 8px rgba(255,200,0,0.6))'
                : 'drop-shadow(0 2px 6px rgba(255,140,0,0.35))',
            transition:
              phase === 'jump'
                ? 'transform 0.45s cubic-bezier(0.175, 0.885, 0.32, 1.275), filter 0.3s'
                : phase === 'write'
                ? 'transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1)'
                : 'transform 0.3s ease-out',
          }}
        >
          <img
            src="/src/assets/aarvo/aarvo_logo_transparent.png"
            alt="AARVO Real Ribbon Icon"
            className="w-full h-full object-contain pointer-events-none"
            draggable={false}
          />

          {/* Landing Sparkles / Burst effect */}
          {(phase === 'jump' || phase === 'write') && (
            <div className="absolute -inset-1 pointer-events-none flex items-center justify-center">
              <span className="absolute -top-2 left-0 text-[10px] animate-bounce text-amber-400">
                ⭐
              </span>
              <span className="absolute -bottom-1 -right-1 text-[10px] animate-ping text-orange-400">
                ✨
              </span>
            </div>
          )}
        </div>

        {/* =================================================================== */}
        {/* 3. ORANGE "AARVO" TEXT (Writes & Fades in immediately after Jump)   */}
        {/* =================================================================== */}
        <div className="relative overflow-visible flex items-center">
          <span
            className={`font-black tracking-wider font-display select-none transition-all duration-500 ${textSize}`}
            style={{
              color: '#FF5722',
              letterSpacing: '0.04em',
              textShadow:
                phase === 'glow' || phase === 'write'
                  ? '0 2px 10px rgba(255, 87, 34, 0.45), 0 0 16px rgba(255, 179, 0, 0.35)'
                  : '0 1px 3px rgba(255, 87, 34, 0.25)',
              filter: phase === 'write' ? 'brightness(1.2)' : 'none',
            }}
          >
            {/* Animated letters with typewriter fade reveal */}
            {['A', 'A', 'R', 'V', 'O'].map((char, index) => {
              const isWriting = phase === 'write';
              return (
                <span
                  key={index}
                  className="inline-block transition-all duration-300"
                  style={{
                    animation: isWriting
                      ? `aarvoLetterFade 0.45s ease-out ${index * 0.08}s both`
                      : 'none',
                    transform:
                      phase === 'write'
                        ? 'translateY(0px) scale(1)'
                        : 'translateY(0) scale(1)',
                  }}
                >
                  {char}
                </span>
              );
            })}
          </span>

          {/* Shimmer light sweep on write/glow */}
          {(phase === 'write' || phase === 'glow') && (
            <div
              className="absolute inset-0 bg-gradient-to-r from-transparent via-white/40 to-transparent pointer-events-none -skew-x-12 animate-in fade-in"
              style={{
                animation: 'aarvoShimmerSweep 1.2s ease-in-out',
              }}
            />
          )}
        </div>
      </div>

      {/* Embedded CSS animations for letter write-in & shimmer */}
      <style>{`
        @keyframes aarvoLetterFade {
          0% {
            opacity: 0;
            transform: translateY(6px) scale(0.85);
            filter: blur(2px);
          }
          60% {
            opacity: 1;
            transform: translateY(-2px) scale(1.1);
            filter: blur(0px);
          }
          100% {
            opacity: 1;
            transform: translateY(0px) scale(1);
            filter: blur(0px);
          }
        }
        @keyframes aarvoShimmerSweep {
          0% {
            transform: translateX(-100%) skewX(-12deg);
            opacity: 0;
          }
          50% {
            opacity: 0.8;
          }
          100% {
            transform: translateX(120%) skewX(-12deg);
            opacity: 0;
          }
        }
      `}</style>
    </div>
  );
};
