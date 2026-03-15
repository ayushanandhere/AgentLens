import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import { SectionCard } from '../components/SectionCard'
import { StatusPill } from '../components/StatusPill'

function money(value: number | null | undefined) {
  return `$${(value ?? 0).toFixed(4)}`
}

export function TracesPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['traces'],
    queryFn: api.listTraces,
    refetchInterval: 5000,
  })

  const traces = data?.content ?? []

  return (
    <SectionCard
      eyebrow="Recent traces"
      title="Live execution ledger"
      action={<span className="eyebrow">{isLoading ? 'Refreshing' : `${traces.length} traces loaded`}</span>}
    >
      <div className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="overflow-hidden rounded-[1.75rem] border border-[var(--line)]">
          <table className="w-full text-left text-sm">
            <thead className="bg-stone-100/80 text-xs uppercase tracking-[0.24em] text-[var(--muted)]">
              <tr>
                <th className="px-4 py-3">Agent</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Cost</th>
                <th className="px-4 py-3">Latency</th>
              </tr>
            </thead>
            <tbody>
              {traces.map((trace) => (
                <tr key={trace.id} className="border-t border-[var(--line)] bg-white/75">
                  <td className="px-4 py-4">
                    <Link to={`/traces/${trace.id}`} className="font-semibold text-[var(--ink)] underline-offset-4 hover:underline">
                      {trace.agentName}
                    </Link>
                    <p className="mt-1 text-xs text-[var(--muted)]">{trace.model}</p>
                  </td>
                  <td className="px-4 py-4">
                    <div className="flex flex-col gap-2">
                      <StatusPill value={trace.status} />
                      <StatusPill value={trace.policyResult} />
                    </div>
                  </td>
                  <td className="px-4 py-4 font-mono text-xs">{money(trace.estimatedCost)}</td>
                  <td className="px-4 py-4 font-mono text-xs">{trace.latencyMs ?? 0} ms</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="grid gap-4">
          {traces.slice(0, 3).map((trace) => (
            <article key={trace.id} className="rounded-[1.75rem] border border-[var(--line)] bg-white/80 p-5">
              <div className="flex items-center justify-between">
                <p className="eyebrow">{trace.agentName}</p>
                <StatusPill value={trace.status} />
              </div>
              <h3 className="mt-3 font-display text-3xl">{trace.model}</h3>
              <div className="mt-4 grid grid-cols-3 gap-3 text-sm">
                <div>
                  <p className="eyebrow">Tokens</p>
                  <p className="mt-1 font-semibold">{trace.totalTokens ?? 0}</p>
                </div>
                <div>
                  <p className="eyebrow">Grounding</p>
                  <p className="mt-1 font-semibold">{((trace.groundingScore ?? 0) * 100).toFixed(0)}%</p>
                </div>
                <div>
                  <p className="eyebrow">Cost</p>
                  <p className="mt-1 font-semibold">{money(trace.estimatedCost)}</p>
                </div>
              </div>
            </article>
          ))}
        </div>
      </div>
    </SectionCard>
  )
}
