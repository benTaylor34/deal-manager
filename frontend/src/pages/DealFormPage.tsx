import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, type PricingLineInput } from '../api';
import { ErrorBanner } from '../components';
import { PricingLineFields, emptyLine } from './PricingLineFields';

export function DealFormPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const clients = useQuery({ queryKey: ['clients'], queryFn: api.clients });

  const [clientId, setClientId] = useState('');
  const [name, setName] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [lines, setLines] = useState<PricingLineInput[]>([emptyLine()]);

  const create = useMutation({
    mutationFn: api.createDeal,
    onSuccess: (deal) => {
      queryClient.invalidateQueries({ queryKey: ['deals'] });
      navigate(`/deals/${deal.id}`);
    },
  });

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    create.mutate({ clientId: Number(clientId), name, startDate, endDate, pricingLines: lines });
  };

  const updateLine = (i: number, line: PricingLineInput) => setLines(lines.map((l, j) => (j === i ? line : l)));
  const removeLine = (i: number) => setLines(lines.filter((_, j) => j !== i));

  return (
    <section>
      <h1>New deal</h1>
      <ErrorBanner error={create.error} />
      <form onSubmit={onSubmit} className="form">
        <div className="grid-2">
          <label>
            Client
            <select value={clientId} onChange={(e) => setClientId(e.target.value)} required>
              <option value="" disabled>
                Choose a client…
              </option>
              {clients.data?.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.segment})
                </option>
              ))}
            </select>
          </label>
          <label>
            Deal name
            <input value={name} onChange={(e) => setName(e.target.value)} maxLength={200} required />
          </label>
          <label>
            Start date
            <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} required />
          </label>
          <label>
            End date
            <input type="date" value={endDate} min={startDate} onChange={(e) => setEndDate(e.target.value)} required />
          </label>
        </div>

        <h2>Pricing lines</h2>
        <div className="line-grid line-grid-head" aria-hidden>
          <span>Service</span>
          <span>Standard price</span>
          <span>Proposed price</span>
          <span>Annual volume</span>
          <span />
        </div>
        {lines.map((line, i) => (
          <div className="line-grid" key={i}>
            <PricingLineFields line={line} onChange={(l) => updateLine(i, l)} />
            <button type="button" className="ghost" onClick={() => removeLine(i)} disabled={lines.length === 1} aria-label="Remove line">
              ✕
            </button>
          </div>
        ))}
        <button type="button" className="ghost" onClick={() => setLines([...lines, emptyLine()])}>
          + Add line
        </button>

        <div className="form-actions">
          <button type="submit" className="primary" disabled={create.isPending}>
            {create.isPending ? 'Saving…' : 'Save draft'}
          </button>
        </div>
      </form>
    </section>
  );
}
