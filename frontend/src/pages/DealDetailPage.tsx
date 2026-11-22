import { useState, type FormEvent } from 'react';
import { useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, type Deal, type PricingLineInput } from '../api';
import { useCurrentUser } from '../auth';
import { ErrorBanner, StatusBadge, SummaryCard } from '../components';
import { formatDateTime, formatUnitPrice } from '../format';
import { PricingLineFields, emptyLine } from './PricingLineFields';

type Action = 'submit' | 'approve' | 'reject' | 'revise';

export function DealDetailPage() {
  const id = Number(useParams().id);
  const { user } = useCurrentUser();
  const queryClient = useQueryClient();
  const deal = useQuery({ queryKey: ['deal', id], queryFn: () => api.deal(id) });
  const audit = useQuery({ queryKey: ['audit', id], queryFn: () => api.audit(id) });

  const [comment, setComment] = useState('');
  const [newLine, setNewLine] = useState<PricingLineInput>(emptyLine());

  const onChanged = (updated: Deal) => {
    queryClient.setQueryData(['deal', id], updated);
    queryClient.invalidateQueries({ queryKey: ['audit', id] });
    queryClient.invalidateQueries({ queryKey: ['deals'] });
  };

  const act = useMutation({
    mutationFn: ({ action }: { action: Action }) => api.act(deal.data!, action, comment.trim() || undefined),
    onSuccess: (updated) => {
      setComment('');
      onChanged(updated);
    },
  });

  const addLine = useMutation({
    mutationFn: () => api.addPricingLine(deal.data!, newLine),
    onSuccess: (updated) => {
      setNewLine(emptyLine());
      onChanged(updated);
    },
  });

  const reload = () => {
    act.reset();
    addLine.reset();
    deal.refetch();
    audit.refetch();
  };

  if (deal.isLoading) return <p className="muted">Loading…</p>;
  if (deal.error) return <ErrorBanner error={deal.error} />;
  const d = deal.data!;

  const isRm = user.role === 'RM';
  const isCommittee = user.role === 'PRICING_COMMITTEE';
  const canEditLines = isRm && d.status === 'DRAFT';
  const canDecide = isCommittee && d.status === 'PENDING_COMMITTEE';

  const onAddLine = (e: FormEvent) => {
    e.preventDefault();
    addLine.mutate();
  };

  return (
    <section>
      <div className="page-head">
        <div>
          <h1>{d.name}</h1>
          <p className="muted">
            {d.client.name} · {d.startDate} → {d.endDate} · created by {d.createdBy} · v{d.version}
          </p>
        </div>
        <StatusBadge status={d.status} />
      </div>

      <ErrorBanner error={act.error ?? addLine.error} onReload={reload} />

      <SummaryCard summary={d.summary} />

      <h2>Pricing lines</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Service</th>
              <th className="num">Standard</th>
              <th className="num">Proposed</th>
              <th className="num">Annual volume</th>
            </tr>
          </thead>
          <tbody>
            {d.pricingLines.map((l) => (
              <tr key={l.id}>
                <td>{l.service}</td>
                <td className="num">{formatUnitPrice(l.standardPrice)}</td>
                <td className="num">{formatUnitPrice(l.proposedPrice)}</td>
                <td className="num">{l.committedVolume.toLocaleString()}</td>
              </tr>
            ))}
            {d.pricingLines.length === 0 && (
              <tr>
                <td colSpan={4} className="muted">
                  No pricing lines yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {canEditLines && (
        <form className="line-grid add-line" onSubmit={onAddLine}>
          <PricingLineFields line={newLine} onChange={setNewLine} />
          <button type="submit" disabled={addLine.isPending}>
            Add line
          </button>
        </form>
      )}

      {(isRm && (d.status === 'DRAFT' || d.status === 'REJECTED')) || canDecide ? (
        <div className="decision">
          <h2>{canDecide ? 'Committee decision' : 'Next step'}</h2>
          <label>
            Comment {canDecide ? '(required to reject)' : '(optional)'}
            <textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={3} maxLength={2000} />
          </label>
          <div className="form-actions">
            {isRm && d.status === 'DRAFT' && (
              <button className="primary" disabled={act.isPending || d.pricingLines.length === 0} onClick={() => act.mutate({ action: 'submit' })}>
                Submit for approval
              </button>
            )}
            {isRm && d.status === 'REJECTED' && (
              <button disabled={act.isPending} onClick={() => act.mutate({ action: 'revise' })}>
                Revise (back to draft)
              </button>
            )}
            {canDecide && (
              <>
                <button className="primary" disabled={act.isPending} onClick={() => act.mutate({ action: 'approve' })}>
                  Approve
                </button>
                <button className="danger" disabled={act.isPending || !comment.trim()} onClick={() => act.mutate({ action: 'reject' })}>
                  Reject
                </button>
              </>
            )}
          </div>
        </div>
      ) : null}

      <h2>Audit trail</h2>
      <ol className="timeline">
        {audit.data?.map((e) => (
          <li key={e.id}>
            <div>
              <strong>{e.action.replaceAll('_', ' ').toLowerCase()}</strong> by {e.actor}
              {e.fromStatus && e.toStatus && e.fromStatus !== e.toStatus && (
                <span className="muted">
                  {' '}
                  ({e.fromStatus} → {e.toStatus})
                </span>
              )}
            </div>
            {e.reason && <div className="reason">“{e.reason}”</div>}
            <div className="muted small">{formatDateTime(e.occurredAt)}</div>
          </li>
        ))}
      </ol>
    </section>
  );
}
