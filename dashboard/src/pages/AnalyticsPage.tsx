import { useQueries } from '@tanstack/react-query'
import { Area, AreaChart, Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../api/client'
import { SectionCard } from '../components/SectionCard'

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <article className="rounded-[1.75rem] border border-[var(--line)] bg-white/80 p-5">
      <p className="eyebrow">{label}</p>
      <p className="mt-3 font-display text-4xl">{value}</p>
    </article>
  )
}

export function AnalyticsPage() {
  const [summary, costByModel, costByAgent, latency, violations, topAgents] = useQueries({
    queries: [
      { queryKey: ['summary'], queryFn: api.getSummary, refetchInterval: 5000 },
      { queryKey: ['cost-model'], queryFn: () => api.getCostBreakdown('model'), refetchInterval: 5000 },
      { queryKey: ['cost-agent'], queryFn: () => api.getCostBreakdown('agent'), refetchInterval: 5000 },
      { queryKey: ['latency'], queryFn: api.getLatency, refetchInterval: 5000 },
      { queryKey: ['violation-trends'], queryFn: api.getViolationTrends, refetchInterval: 5000 },
      { queryKey: ['top-agents'], queryFn: api.getTopAgents, refetchInterval: 5000 },
    ],
  })

  const topAgentSections: Array<{ label: string; items: Array<{ agentId: string; agentName: string; value: number }> }> = [
    { label: 'Most active', items: topAgents.data?.mostActive ?? [] },
    { label: 'Most expensive', items: topAgents.data?.mostExpensive ?? [] },
    { label: 'Most violations', items: topAgents.data?.mostViolations ?? [] },
  ]

  return (
    <div className="grid gap-6">
      <SectionCard eyebrow="Operational posture" title="Signal summary">
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <StatCard label="Total traces" value={String(summary.data?.totalTraces ?? 0)} />
          <StatCard label="Violation rate" value={`${(summary.data?.violationRatePercent ?? 0).toFixed(1)}%`} />
          <StatCard label="Average latency" value={`${Math.round(summary.data?.avgLatencyMs ?? 0)} ms`} />
          <StatCard label="Total cost" value={`$${(summary.data?.totalCostUsd ?? 0).toFixed(4)}`} />
        </div>
      </SectionCard>

      <div className="grid gap-6 xl:grid-cols-2">
        <SectionCard eyebrow="Cost" title="Spend by model">
          <div className="h-[320px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={costByModel.data?.entries ?? []}>
                <CartesianGrid strokeDasharray="4 4" stroke="#d7cabb" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                <YAxis tickFormatter={(value) => `$${value}`} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="totalCost" fill="#c83f1c" radius={[10, 10, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </SectionCard>

        <SectionCard eyebrow="Latency" title="Violation trendline">
          <div className="h-[320px]">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={violations.data?.byDay ?? []}>
                <defs>
                  <linearGradient id="signalFill" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="0%" stopColor="#c83f1c" stopOpacity={0.5} />
                    <stop offset="100%" stopColor="#c83f1c" stopOpacity={0.03} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="4 4" stroke="#d7cabb" />
                <XAxis dataKey="day" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Area dataKey="count" stroke="#c83f1c" fill="url(#signalFill)" strokeWidth={3} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </SectionCard>
      </div>

      <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <SectionCard eyebrow="Latency distribution" title="Percentile readout">
          <div className="grid gap-4 md:grid-cols-3">
            <StatCard label="P50" value={`${Math.round(latency.data?.p50 ?? 0)} ms`} />
            <StatCard label="P95" value={`${Math.round(latency.data?.p95 ?? 0)} ms`} />
            <StatCard label="P99" value={`${Math.round(latency.data?.p99 ?? 0)} ms`} />
          </div>
        </SectionCard>

        <SectionCard eyebrow="Top agents" title="Activity and spend leaders">
          <div className="grid gap-4 md:grid-cols-3">
            {topAgentSections.map(({ label, items }) => (
              <article key={label} className="rounded-[1.5rem] border border-[var(--line)] bg-white/80 p-4">
                <p className="eyebrow">{label}</p>
                <div className="mt-4 grid gap-3">
                  {items.map((item) => (
                    <div key={`${label}-${item.agentName}`} className="rounded-2xl bg-stone-100/80 px-4 py-3">
                      <p className="font-semibold">{item.agentName}</p>
                      <p className="text-xs uppercase tracking-[0.2em] text-[var(--muted)]">{item.value}</p>
                    </div>
                  ))}
                </div>
              </article>
            ))}
          </div>
          <div className="mt-6 h-[240px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={costByAgent.data?.entries ?? []}>
                <CartesianGrid strokeDasharray="4 4" stroke="#d7cabb" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="traceCount" fill="#123a57" radius={[10, 10, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </SectionCard>
      </div>
    </div>
  )
}
