import React from 'react';
import { CheckCircle2, Info, AlertTriangle, XCircle, X } from 'lucide-react';

export type ToastVariant = 'success' | 'info' | 'warning' | 'error';

export interface ToastItem {
  id: string;
  message: string;
  title?: string;
  variant: ToastVariant;
  actionLabel?: string;
  onAction?: () => void;
}

interface ToastContainerProps {
  toasts: ToastItem[];
  onDismiss: (id: string) => void;
}

const variantStyles: Record<
  ToastVariant,
  {
    container: string;
    iconBg: string;
    titleColor: string;
    Icon: React.ComponentType<{ className?: string }>;
  }
> = {
  success: {
    container:
      'bg-slate-900/95 dark:bg-[#211A17]/95 text-white border-emerald-500/40',
    iconBg: 'bg-emerald-500/20 text-emerald-400',
    titleColor: 'text-emerald-300',
    Icon: CheckCircle2,
  },
  info: {
    container:
      'bg-slate-900/95 dark:bg-[#211A17]/95 text-white border-orange-500/40',
    iconBg: 'bg-orange-500/20 text-[#FF8A65]',
    titleColor: 'text-[#FF8A65]',
    Icon: Info,
  },
  warning: {
    container:
      'bg-slate-900/95 dark:bg-[#211A17]/95 text-white border-amber-500/50',
    iconBg: 'bg-amber-500/20 text-amber-400',
    titleColor: 'text-amber-300',
    Icon: AlertTriangle,
  },
  error: {
    container:
      'bg-rose-950/95 text-rose-50 border-rose-500/60',
    iconBg: 'bg-rose-500/25 text-rose-300',
    titleColor: 'text-rose-200',
    Icon: XCircle,
  },
};

export const ToastContainer: React.FC<ToastContainerProps> = ({
  toasts,
  onDismiss,
}) => {
  if (toasts.length === 0) return null;

  return (
    <div
      aria-live="polite"
      aria-atomic="false"
      className="fixed top-[104px] left-1/2 -translate-x-1/2 z-[70] w-[92%] max-w-[404px] pointer-events-none flex flex-col gap-2"
    >
      {toasts.map((toast) => {
        const style = variantStyles[toast.variant] || variantStyles.success;
        const Icon = style.Icon;
        return (
          <div
            key={toast.id}
            role="status"
            className={`pointer-events-auto flex items-center justify-between gap-2.5 px-3 py-2.5 rounded-2xl border shadow-2xl backdrop-blur-md animate-in slide-in-from-top-2 fade-in duration-200 ${style.container}`}
          >
            <div className="flex items-center gap-2.5 min-w-0">
              <div
                className={`w-7 h-7 rounded-full flex items-center justify-center shrink-0 ${style.iconBg}`}
              >
                <Icon className="w-4 h-4 stroke-[2.2]" />
              </div>
              <div className="min-w-0 leading-tight">
                {toast.title && (
                  <p className={`text-[11px] font-black truncate ${style.titleColor}`}>
                    {toast.title}
                  </p>
                )}
                <p className="text-xs font-semibold text-white/95 line-clamp-2">
                  {toast.message}
                </p>
              </div>
            </div>

            <div className="flex items-center gap-1.5 shrink-0">
              {toast.actionLabel && toast.onAction && (
                <button
                  type="button"
                  onClick={() => {
                    toast.onAction?.();
                    onDismiss(toast.id);
                  }}
                  className="px-2.5 py-1 rounded-lg bg-[#FF5722] hover:bg-[#E64A19] text-white text-[11px] font-bold transition-colors cursor-pointer whitespace-nowrap"
                >
                  {toast.actionLabel}
                </button>
              )}
              <button
                type="button"
                onClick={() => onDismiss(toast.id)}
                aria-label="Dismiss notification"
                className="p-1 text-white/60 hover:text-white rounded-lg transition-colors cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
};
