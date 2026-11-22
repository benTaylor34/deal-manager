import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api, type DealStatus } from '../api';
import { useCurrentUser } from '../auth';
import { ErrorBanner, StatusBadge } from '../components';
import { STATUS_LABEL, formatMoney, formatPercent } from '../format';

const STATUSES = Object.keys(STATUS_LABEL) as DealStatus[];

export function DealListPage() {
  const { user } = useCurrentUser();
  const [status, setStatus] = useState<DealStatus | ''>(user.role === 'PRICING_COMMITTEE' ? 'PENDING_COMMITTEE' : '');
  const deals = useQuery({ queryKey: ['deals', status], queryFn: () => api.deals(status || undefined) });

  return (
    <section>
      <div className="page-head">
        <h1>Deals</h1>
        <div className="actions">
          <select value={status} onChange={(e) => setStatus(e.target.value as DealStatus | '')} aria-label="Filter by status">
            <option value="">All statuses</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {STATUS_LABEL[s]}
              </option>
            ))}
          </select>
          {user.role === 'RM' && (
            <Link to="/deals/new" className="button primary">
              New deal
            </Link>
          )}
        </div>
      </div>

      <ErrorBanner error={deals.error} />
      {deals.isLoading && <p className="muted">Loading…</p>}
      {deals.data && deals.data.length === 0 && <p className="muted">No deals yet.</p>}

      {deals.data && deals.data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Deal</th>
                <th>Client</th>
                <th>Status</th>
                <th className="num">Discount</th>
                <th className="num">Annual revenue (proposed)</th>
                <th>Term</th>
              </tr>
            </thead>
            <tbody>
              {deals.data.map((d) => (
                <tr key={d.id}>
                  <td>
                    <Link to={`/deals/${d.id}`}>{d.name}</Link>
                  </td>
                  <td>{d.client.name}</td>
                  <td>
                    <StatusBadge status={d.status} />
                  </td>
                  <td className="num">{formatPercent(d.summary.discountRate)}</td>
                  <td className="num">{formatMoney(d.summary.annualProposedRevenue)}</td>
                  <td>
                    {d.startDate} → {d.endDate}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
