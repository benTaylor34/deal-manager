import { authHeader } from './auth';

// Money arrives as strings (e.g. "1234.5000") so the browser never rounds it.
export type Money = string;

export type DealStatus = 'DRAFT' | 'PENDING_COMMITTEE' | 'APPROVED' | 'REJECTED';
export type ApprovalAuthority = 'RM' | 'PRICING_COMMITTEE';

export interface Client {
  id: number;
  name: string;
  segment: string;
}

export interface PricingLine {
  id: number;
  service: string;
  standardPrice: Money;
  proposedPrice: Money;
  committedVolume: number;
}

export interface DealSummary {
  annualStandardRevenue: Money;
  annualProposedRevenue: Money;
  revenueGivenUp: Money;
  discountRate: string; // fraction: "0.15" = 15%
}

export interface Deal {
  id: number;
  client: Client;
  name: string;
  status: DealStatus;
  startDate: string;
  endDate: string;
  requiredAuthority: ApprovalAuthority | null;
  version: number;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  pricingLines: PricingLine[];
  summary: DealSummary;
}

export interface AuditEvent {
  id: number;
  actor: string;
  action: string;
  fromStatus: DealStatus | null;
  toStatus: DealStatus | null;
  reason: string | null;
  details: Record<string, unknown> | null;
  occurredAt: string;
}

export interface PricingLineInput {
  service: string;
  standardPrice: string;
  proposedPrice: string;
  committedVolume: number;
}

export interface CreateDealInput {
  clientId: number;
  name: string;
  startDate: string;
  endDate: string;
  pricingLines: PricingLineInput[];
}

/** RFC 9457 problem detail from the backend, plus the HTTP status. */
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly title: string,
    detail: string,
    public readonly fieldErrors: Record<string, string> = {},
  ) {
    super(detail);
  }

  get isStale() {
    return this.status === 412 || (this.status === 409 && this.title === 'Concurrent update');
  }
}

async function request<T>(path: string, init: RequestInit = {}, version?: number): Promise<T> {
  const headers: Record<string, string> = {
    Accept: 'application/json',
    Authorization: authHeader(),
  };
  if (init.body) headers['Content-Type'] = 'application/json';
  if (version !== undefined) headers['If-Match'] = `"${version}"`;

  const res = await fetch(`/api${path}`, { ...init, headers });
  if (!res.ok) {
    let problem: { title?: string; detail?: string; errors?: Record<string, string> } = {};
    try {
      problem = await res.json();
    } catch {
      // non-JSON error body
    }
    throw new ApiError(res.status, problem.title ?? res.statusText, problem.detail ?? `Request failed (${res.status})`, problem.errors);
  }
  return res.json() as Promise<T>;
}

const json = (body: unknown) => JSON.stringify(body ?? {});

export const api = {
  clients: () => request<Client[]>('/clients'),
  deals: (status?: DealStatus) => request<Deal[]>(`/deals${status ? `?status=${status}` : ''}`),
  deal: (id: number) => request<Deal>(`/deals/${id}`),
  audit: (id: number) => request<AuditEvent[]>(`/deals/${id}/audit`),
  createDeal: (input: CreateDealInput) => request<Deal>('/deals', { method: 'POST', body: json(input) }),
  addPricingLine: (deal: Deal, line: PricingLineInput) =>
    request<Deal>(`/deals/${deal.id}/pricing-lines`, { method: 'POST', body: json(line) }, deal.version),
  act: (deal: Deal, action: 'submit' | 'approve' | 'reject' | 'revise', comment?: string) =>
    request<Deal>(`/deals/${deal.id}/${action}`, { method: 'POST', body: json({ comment }) }, deal.version),
};
