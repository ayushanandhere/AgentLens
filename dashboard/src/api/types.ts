export type PageResponse<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export type TraceSummary = {
  id: string
  agentId: string
  agentName: string
  model: string
  status: string
  policyResult: string
  totalTokens: number
  estimatedCost: number
  latencyMs: number
  groundingScore: number
  startedAt: string
}

export type TraceDetail = TraceSummary & {
  traceId: string
  promptText: string
  responseText: string
  temperature: number
  inputTokens: number
  outputTokens: number
  completedAt: string | null
  ttftMs: number | null
  blockedReason: string | null
  tenantId: string | null
  sessionId: string | null
  metadata: Record<string, unknown> | null
  events: TraceEvent[]
}

export type TraceEvent = {
  id: string
  traceId: string
  eventType: string
  eventName: string
  inputData: Record<string, unknown> | null
  outputData: Record<string, unknown> | null
  status: string
  durationMs: number | null
  errorMessage: string | null
  sequenceNum: number
  timestamp: string
}

export type TraceTimeline = {
  traceId: string
  agentId: string
  agentName: string
  status: string
  policyResult: string
  startedAt: string
  completedAt: string | null
  totalDurationMs: number
  items: TraceTimelineItem[]
}

export type TraceTimelineItem = {
  eventId: string
  eventType: string
  eventName: string
  status: string
  timestamp: string
  relativeStartMs: number
  durationMs: number | null
  errorMessage: string | null
  inputData: Record<string, unknown> | null
  outputData: Record<string, unknown> | null
}

export type Policy = {
  id: string
  name: string
  description: string
  policyType: string
  config: Record<string, unknown>
  scope: string
  scopeId: string | null
  enabled: boolean
  severity: string
  createdAt: string
  updatedAt: string
}

export type Violation = {
  id: string
  traceId: string
  policyId: string
  policyName: string
  violationType: string
  severity: string
  details: Record<string, unknown>
  actionTaken: string
  resolvedBy: string | null
  createdAt: string
}

export type AnalyticsSummary = {
  totalTraces: number
  completedTraces: number
  failedTraces: number
  blockedTraces: number
  avgLatencyMs: number
  avgCostUsd: number
  totalCostUsd: number
  avgGroundingScore: number
  violationRatePercent: number
}

export type CostBreakdownResponse = {
  entries: Array<{
    key: string
    label: string
    totalCost: number
    totalTokens: number
    traceCount: number
  }>
}

export type LatencyResponse = {
  p50: number
  p95: number
  p99: number
  avg: number
  min: number
  max: number
}

export type ViolationTrendsResponse = {
  byType: Array<{ type: string; count: number }>
  bySeverity: Array<{ severity: string; count: number }>
  byDay: Array<{ day: string; count: number }>
}

export type TopAgentsResponse = {
  mostActive: Array<{ agentId: string; agentName: string; value: number }>
  mostExpensive: Array<{ agentId: string; agentName: string; value: number }>
  mostViolations: Array<{ agentId: string; agentName: string; value: number }>
}

export type PolicyStageResponse = {
  stage: string
  verdict: string
  violations: Array<{
    policyName: string
    policyType: string
    severity: string
    actionTaken: string
    details: Record<string, unknown>
  }>
}

export type EvaluatePoliciesResponse = {
  overallVerdict: string
  stages: PolicyStageResponse[]
}
