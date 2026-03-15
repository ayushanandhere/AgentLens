import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'
import { api } from '../api/client'
import { SectionCard } from '../components/SectionCard'
import { StatusPill } from '../components/StatusPill'

function renderJson(value: unknown) {
  return JSON.stringify(value ?? {}, null, 2)
}

export function TraceDetailPage() {
  const { traceId = '' } = useParams()
  const detailQuery = useQuery({
    queryKey: ['trace-detail', traceId],
    queryFn: () => api.getTraceDetail(traceId),
    enabled: Boolean(traceId),
    refetchInterval: 5000,
  })
  const timelineQuery = useQuery({
    queryKey: ['trace-timeline', traceId],
    queryFn: () => api.getTraceTimeline(traceId),
    enabled: Boolean(traceId),
    refetchInterval: 5000,
  })

  const detail = detailQuery.data
  const timeline = timelineQuery.data

  if (!traceId) {
    return <SectionCard eyebrow="Trace detail" title="Select a trace" />
  }

  return (
    <div className="grid gap-6">
      <SectionCard eyebrow="Trace detail" title={detail?.agentName ?? 'Trace dossier'}>
        <div className="grid gap-4 lg:grid-cols-4">
          <article className="rounded-[1.75rem] border border-[var(--line)] bg-white/80 p-5">
            <p className="eyebrow">Trace status</p>
            <div className="mt-3 flex flex-wrap gap-2">
              <StatusPill value={detail?.status} />
              <StatusPill value={detail?.policyResult} />
            </div>
          </article>
          <article className="rounded-[1.75rem] border border-[var(--line)] bg-white/80 p-5">
            <p className="eyebrow">Token footprint</p>
            <p className="mt-3 font-display text-4xl">{detail?.totalTokens ?? 0}</p>
          </article>
          <article className="rounded-[1.75rem] border border-[var(--line)] bg-white/80 p-5">
            <p className="eyebrow">Latency</p>
            <p className="mt-3 font-display text-4xl">{detail?.latencyMs ?? 0}ms</p>
          </article>
          <article className="rounded-[1.75rem] border border-[var(--line)] bg-white/80 p-5">
            <p className="eyebrow">Blocked reason</p>
            <p className="mt-3 text-sm leading-6 text-[var(--muted)]">{detail?.blockedReason ?? 'No active block recorded.'}</p>
          </article>
        </div>
      </SectionCard>

      <div className="grid gap-6 xl:grid-cols-[0.92fr_1.08fr]">
        <SectionCard eyebrow="Prompt and response" title="Execution transcript">
          <div className="grid gap-4">
            <article className="rounded-[1.75rem] border border-[var(--line)] bg-stone-50 p-5">
              <p className="eyebrow">Prompt</p>
              <p className="mt-3 whitespace-pre-wrap text-sm leading-7">{detail?.promptText ?? 'No prompt captured.'}</p>
            </article>
            <article className="rounded-[1.75rem] border border-[var(--line)] bg-stone-50 p-5">
              <p className="eyebrow">Response</p>
              <p className="mt-3 whitespace-pre-wrap text-sm leading-7">{detail?.responseText ?? 'Trace not completed yet.'}</p>
            </article>
          </div>
        </SectionCard>

        <SectionCard eyebrow="Timeline" title="Event chronology">
          <div className="relative ml-3 border-l border-dashed border-[var(--line-strong)] pl-6">
            {timeline?.items.map((item) => (
              <article key={item.eventId} className="relative mb-6 rounded-[1.75rem] border border-[var(--line)] bg-white/85 p-5">
                <div className="absolute -left-[2.2rem] top-7 h-4 w-4 rounded-full border-4 border-[var(--paper)] bg-[var(--accent)]" />
                <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                  <div>
                    <p className="eyebrow">{item.eventType}</p>
                    <h3 className="mt-2 font-display text-3xl">{item.eventName}</h3>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <StatusPill value={item.status} />
                    <span className="eyebrow">{item.relativeStartMs}ms from start</span>
                  </div>
                </div>
                <div className="mt-4 grid gap-4 lg:grid-cols-2">
                  <pre className="overflow-auto rounded-2xl bg-stone-950 p-4 text-xs text-stone-100">{renderJson(item.inputData)}</pre>
                  <pre className="overflow-auto rounded-2xl bg-stone-950 p-4 text-xs text-stone-100">{renderJson(item.outputData)}</pre>
                </div>
                {item.errorMessage ? <p className="mt-4 text-sm text-red-700">{item.errorMessage}</p> : null}
              </article>
            ))}
          </div>
        </SectionCard>
      </div>
    </div>
  )
}
