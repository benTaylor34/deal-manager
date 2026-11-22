import type { DealStatus } from './api';

// Display-only formatting. All arithmetic happens on the server in BigDecimal.
const currency = new Intl.NumberFormat('en-CA', { style: 'currency', currency: 'CAD' });
const unitPrice = new Intl.NumberFormat('en-CA', {
  style: 'currency',
  currency: 'CAD',
  minimumFractionDigits: 2,
  maximumFractionDigits: 4,
});

export const formatMoney = (value: string) => currency.format(Number(value));
export const formatUnitPrice = (value: string) => unitPrice.format(Number(value));
export const formatPercent = (fraction: string) => `${(Number(fraction) * 100).toFixed(2)}%`;
export const formatDateTime = (iso: string) => new Date(iso).toLocaleString();

export const STATUS_LABEL: Record<DealStatus, string> = {
  DRAFT: 'Draft',
  PENDING_COMMITTEE: 'Pending committee',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
};
