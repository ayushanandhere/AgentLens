import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { api } from '../api/client'
import { SectionCard } from '../components/SectionCard'
import { StatusPill } from '../components/StatusPill'

export function ViolationsPage() {
  const queryClient = useQueryClient()
  const [approverId, setApproverId] = useState(import.meta.env.VITE_APPROVER_ID ?? 'admin@agentlens.dev')
  const violationsQuery = useQuery({
    queryKey: ['violations'],
    queryFn: api.listViolations,
    refetchInterval: 5000,
  })

  const approveMutation = useMutation({
    mutationFn: (violationId: string) => api.approveViolation(violationId, approverId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['violations'] }),
  })

  const rejectMutation = useMutation({
    mutationFn: (violationId: string) => api.rejectViolation(violationId, approverId, 'Rejected from dashboard review'),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['violations'] }),
  })

  const violations = useMemo(() => {
    const content = violationsQuery.data?.content ?? []
    return [...content].sort((left, right) => {
      if (left.actionTaken === 'PENDING_APPROVAL' && right.actionTaken !== 'PENDING_APPROVAL') return -1
      if (left.actionTaken !== 'PENDING_APPROVAL' && right.actionTaken === 'PENDING_APPROVAL') return 1
      return right.createdAt.localeCompare(left.createdAt)
    })
  }, [violationsQuery.data])

  return (
    <SectionCard eyebrow="Intervention queue" title="Violations and overrides">
      <div className="mb-5 flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
        <p className="text-sm text-[var(--muted)]">Pending approvals are pinned to the top of the queue.</p>
        <label className="grid gap-2 text-sm md:w-[320px]">
          <span className="eyebrow">Approver identity</span>
          <input
            className="rounded-2xl border border-[var(--line)] bg-stone-50 px-4 py-3 outline-none transition focus:border-[var(--line-strong)]"
            value={approverId}
            onChange={(event) => setApproverId(event.target.value)}
          />
        </label>
      </div>
      <div className="grid gap-4">
        {violations.map((violation) => {
          const pending = violation.actionTaken === 'PENDING_APPROVAL'
          return (
            <article key={violation.id} className="rounded-[1.75rem] border border-[var(--line)] bg-white/80 p-5">
              <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                <div>
                  <p className="eyebrow">{violation.policyName}</p>
                  <h3 className="mt-2 font-display text-3xl">{violation.violationType}</h3>
                  <p className="mt-3 text-sm text-[var(--muted)]">Trace ID: {violation.traceId}</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <StatusPill value={violation.severity} />
                  <StatusPill value={violation.actionTaken} />
                </div>
              </div>
              <pre className="mt-4 overflow-auto rounded-2xl bg-stone-950 p-4 text-xs text-stone-100">
                {JSON.stringify(violation.details, null, 2)}
              </pre>
              {pending ? (
                <div className="mt-4 flex flex-wrap gap-3">
                  <button
                    type="button"
                    onClick={() => approveMutation.mutate(violation.id)}
                    className="rounded-full bg-[var(--accent)] px-5 py-3 text-xs font-semibold uppercase tracking-[0.2em] text-stone-50"
                  >
                    Approve
                  </button>
                  <button
                    type="button"
                    onClick={() => rejectMutation.mutate(violation.id)}
                    className="rounded-full border border-[var(--line-strong)] px-5 py-3 text-xs font-semibold uppercase tracking-[0.2em]"
                  >
                    Reject
                  </button>
                </div>
              ) : (
                <p className="mt-4 text-sm text-[var(--muted)]">Resolved by {violation.resolvedBy ?? 'system'}.</p>
              )}
            </article>
          )
        })}
      </div>
    </SectionCard>
  )
}
