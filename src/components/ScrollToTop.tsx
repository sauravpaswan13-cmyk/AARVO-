import React, { useState, useEffect } from 'react';
import { ArrowUp } from 'lucide-react';
import { haptics } from '../utils/haptics';

interface ScrollToTopProps {
  /** Optional container ref if scrolling inside a specific overflow element instead of window */
  containerRef?: React.RefObject<HTMLElement | null>;
  /** Threshold in pixels after which the button becomes visible (default: 300) */
  threshold?: number;
  className?: string;
}

export const ScrollToTop: React.FC<ScrollToTopProps> = ({
  containerRef,
  threshold = 300,
  className = '',
}) => {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const checkScroll = () => {
      const scrollPosition = containerRef?.current
        ? containerRef.current.scrollTop
        : window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0;

      setIsVisible(scrollPosition > threshold);
    };

    // Initial check
    checkScroll();

    // Attach scroll listener to either the provided container or window
    const target = containerRef?.current ?? window;
    target.addEventListener('scroll', checkScroll, { passive: true });

    return () => {
      target.removeEventListener('scroll', checkScroll);
    };
  }, [containerRef, threshold]);

  const handleScrollToTop = () => {
    haptics.light();

    if (containerRef?.current) {
      containerRef.current.scrollTo({
        top: 0,
        behavior: 'smooth',
      });
    }

    // Scroll window as well to ensure smooth scroll across all viewports
    window.scrollTo({
      top: 0,
      behavior: 'smooth',
    });
  };

  if (!isVisible) {
    return null;
  }

  return (
    <button
      type="button"
      onClick={handleScrollToTop}
      aria-label="Scroll to top of page"
      title="Scroll to top"
      className={`fixed bottom-20 right-4 sm:right-6 md:right-[max(1.5rem,calc(50%-205px))] z-30 w-11 h-11 rounded-full bg-[#2418D8] dark:bg-[#3D2DE0] hover:bg-[#1A10B3] dark:hover:bg-[#4E3EF5] text-white flex items-center justify-center shadow-lg hover:shadow-xl shadow-[#2418D8]/30 border border-white/20 dark:border-white/10 backdrop-blur-xs transition-all duration-300 ease-out animate-in fade-in zoom-in-75 active:scale-90 group cursor-pointer ${className}`}
    >
      <ArrowUp className="w-5 h-5 stroke-[2.5] group-hover:-translate-y-0.5 transition-transform duration-200" />
    </button>
  );
};
