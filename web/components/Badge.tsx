import React from 'react';
import {
  CheckCircle,
  Clock,
  AlertTriangle,
  XCircle,
  MinusCircle,
  Loader2,
  Shield,
  FileText,
  type LucideIcon,
} from 'lucide-react';

type BadgeVariant =
  | 'active'
  | 'pending'
  | 'expired'
  | 'suspended'
  | 'cancelled'
  | 'draft'
  | 'info'
  | 'warning'
  | 'success'
  | 'error';

const variantConfig: Record<BadgeVariant, { bg: string; text: string; icon: LucideIcon; label: string }> = {
  active: { bg: 'bg-emerald-500/20', text: 'text-emerald-300', icon: CheckCircle, label: '● Active' },
  pending: { bg: 'bg-amber-500/20', text: 'text-amber-300', icon: Clock, label: '◌ Pending' },
  expired: { bg: 'bg-red-500/20', text: 'text-red-300', icon: XCircle, label: '✕ Expired' },
  suspended: { bg: 'bg-orange-500/20', text: 'text-orange-300', icon: MinusCircle, label: '⊘ Suspended' },
  cancelled: { bg: 'bg-slate-500/20', text: 'text-slate-400', icon: XCircle, label: '✕ Cancelled' },
  draft: { bg: 'bg-slate-500/20', text: 'text-slate-300', icon: FileText, label: '◉ Draft' },
  info: { bg: 'bg-blue-500/20', text: 'text-blue-300', icon: Shield, label: 'ℹ Info' },
  warning: { bg: 'bg-amber-500/20', text: 'text-amber-300', icon: AlertTriangle, label: '⚠ Warning' },
  success: { bg: 'bg-emerald-500/20', text: 'text-emerald-300', icon: CheckCircle, label: '✓ Success' },
  error: { bg: 'bg-red-500/20', text: 'text-red-300', icon: XCircle, label: '✕ Error' },
};

type BadgeProps = {
  variant: BadgeVariant;
  label?: string;
  className?: string;
};

/**
 * Status badge with icon + text. License/order status is never indicated by color alone.
 */
export function Badge({ variant, label, className = '' }: BadgeProps) {
  const config = variantConfig[variant];
  const Icon = config.icon;
  const displayLabel = label ?? config.label;

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold ${config.bg} ${config.text} ${className}`}
      role="status"
    >
      <Icon className="h-3 w-3" aria-hidden="true" />
      <span>{displayLabel}</span>
    </span>
  );
}

// Utility to map status strings to badge variants
export function statusToBadgeVariant(status: string): BadgeVariant {
  const map: Record<string, BadgeVariant> = {
    ACTIVE: 'active',
    PUBLISHED: 'active',
    PAID: 'active',
    PAID_MANUALLY: 'active',
    OPEN: 'pending',
    IN_PROGRESS: 'pending',
    PENDING_PAYMENT: 'pending',
    WAITING_CUSTOMER: 'warning',
    DRAFT: 'draft',
    UNSIGNED_DRAFT: 'draft',
    EXPIRED: 'expired',
    SUSPENDED: 'suspended',
    CANCELLED: 'cancelled',
    REFUNDED: 'cancelled',
    RESOLVED: 'success',
    CLOSED: 'info',
    RETIRED: 'expired',
  };
  return map[status] ?? 'info';
}
