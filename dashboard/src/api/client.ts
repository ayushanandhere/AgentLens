import type {
  AnalyticsSummary,
  CostBreakdownResponse,
  EvaluatePoliciesResponse,
  LatencyResponse,
  PageResponse,
  Policy,
  TopAgentsResponse,
  TraceDetail,
  TraceSummary,
  TraceTimeline,
  Violation,
  ViolationTrendsResponse,
} from './types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const OPERATOR_API_KEY = import.meta.env.VITE_OPERATOR_API_KEY ?? 'operator-local-key'

type RequestOptions = RequestInit & {
  query?: Record<string, string | number | boolean | undefined>
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const url = new URL(path, API_BASE_URL)
  if (options.query) {
    Object.entries(options.query).forEach(([key, value]) => {
      if (value !== undefined && value !== '') {
        url.searchParams.set(key, String(value))
      }
    })
  }

  const response = await fetch(url.toString(), {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'X-API-Key': OPERATOR_API_KEY,
      ...(options.headers ?? {}),
    },
  })

  if (!response.ok) {
    const body = await response.text()
    throw new Error(body || `Request failed with ${response.status}`)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

export const api = {
  listTraces: () => request<PageResponse<TraceSummary>>('/api/v1/traces', { query: { page: 0, size: 20, sort: 'startedAt,desc' } }),
  getTraceDetail: (traceId: string) => request<TraceDetail>(`/api/v1/traces/${traceId}`),
  getTraceTimeline: (traceId: string) => request<TraceTimeline>(`/api/v1/traces/${traceId}/timeline`),
  listPolicies: () => request<PageResponse<Policy>>('/api/v1/policies', { query: { page: 0, size: 50 } }),
  createPolicy: (payload: unknown) => request<Policy>('/api/v1/policies', { method: 'POST', body: JSON.stringify(payload) }),
  disablePolicy: (policyId: string) => request<void>(`/api/v1/policies/${policyId}`, { method: 'DELETE' }),
  evaluatePolicies: (payload: unknown) =>
    request<EvaluatePoliciesResponse>('/api/v1/policies/evaluate', { method: 'POST', body: JSON.stringify(payload) }),
  listViolations: () =>
    request<PageResponse<Violation>>('/api/v1/violations', { query: { page: 0, size: 50, sort: 'createdAt,desc' } }),
  approveViolation: (violationId: string, resolvedBy: string) =>
    request<Violation>(`/api/v1/violations/${violationId}/approve`, {
      method: 'POST',
      body: JSON.stringify({ resolvedBy }),
    }),
  rejectViolation: (violationId: string, resolvedBy: string, reason: string) =>
    request<Violation>(`/api/v1/violations/${violationId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ resolvedBy, reason }),
    }),
  getSummary: () => request<AnalyticsSummary>('/api/v1/analytics/summary'),
  getCostBreakdown: (groupBy: 'model' | 'agent') =>
    request<CostBreakdownResponse>('/api/v1/analytics/cost', { query: { groupBy } }),
  getLatency: () => request<LatencyResponse>('/api/v1/analytics/latency'),
  getViolationTrends: () => request<ViolationTrendsResponse>('/api/v1/analytics/violations'),
  getTopAgents: () => request<TopAgentsResponse>('/api/v1/analytics/top-agents', { query: { limit: 5 } }),
}

export const dashboardConnection = {
  baseUrl: API_BASE_URL,
  apiKey: OPERATOR_API_KEY,
}
