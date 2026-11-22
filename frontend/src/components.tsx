import type { ApiError, DealStatus, DealSummary } from './api';
import { STATUS_LABEL, formatMoney, formatPercent } from './format';

export function StatusBadge({ status }: { status: DealStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{STATUS_LABEL[status]}</span>;
}

export function SummaryCard({ summary }: { summary: DealSummary }) {
  const overLimit = Number(summary.discountRate) > 0.15;
  return (
    <div className="summary-card">
      <div>
        <span className="label">Annual revenue at standard</span>
        <strong>{formatMoney(summary.annualStandardRevenue)}</strong>
      </div>
      <div>
        <span className="label">Annual revenue at proposed</span>
        <strong>{formatMoney(summary.annualProposedRevenue)}</strong>
      </div>
      <div>
        <span className="label">Discount</span>
        <strong className={overLimit ? 'text-warn' : ''}>{formatPercent(summary.discountRate)}</strong>
        <span className="hint">{overLimit ? 'Above 15%: Pricing Committee' : 'Within RM authority (≤ 15%)'}</span>
      </div>
      <div>
        <span className="label">Revenue given up</span>
        <strong>{formatMoney(summary.revenueGivenUp)}</strong>
      </div>
    </div>
  );
}

export function ErrorBanner({ error, onReload }: { error: unknown; onReload?: () => void }) {
  if (!error) return null;
  const apiError = error as ApiError;
  if (apiError.isStale) {
    return (
      <div className="banner banner-error" role="alert">
        Someone else changed this deal since you opened it.{' '}
        {onReload && (
          <button type="button" className="link" onClick={onReload}>
            Reload the latest version
          </button>
        )}
      </div>
    );
  }
  const fieldErrors = apiError.fieldErrors ?? {};
  return (
    <div className="banner banner-error" role="alert">
      <strong>{apiError.title ?? 'Error'}:</strong> {apiError.message}
      {Object.keys(fieldErrors).length > 0 && (
        <ul>
          {Object.entries(fieldErrors).map(([field, msg]) => (
            <li key={field}>
              {field}: {msg}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
