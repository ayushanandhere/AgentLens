import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { api } from '../api/client'
import { SectionCard } from '../components/SectionCard'
import { StatusPill } from '../components/StatusPill'

const defaultPolicy = {
  name: 'Human approval for shell access',
  description: 'Pause traces before shell actions execute',
  policyType: 'REQUIRE_APPROVAL',
  scope: 'GLOBAL',
  severity: 'BLOCK',
  config: JSON.stringify({ for_tools: ['execShell'], approvers: ['admin@agentlens.dev'] }, null, 2),
}

const defaultDryRun = {
  agentId: '',
  model: 'gpt-4o',
  promptText: 'Deploy the new worker revision to production.',
  eventName: 'execShell',
}

export function PoliciesPage() {
  const queryClient = useQueryClient()
  const policiesQuery = useQuery({
    queryKey: ['policies'],
    queryFn: api.listPolicies,
    refetchInterval: 5000,
  })

  const [policyForm, setPolicyForm] = useState(defaultPolicy)
  const [dryRunForm, setDryRunForm] = useState(defaultDryRun)

  const createMutation = useMutation({
    mutationFn: async () =>
      api.createPolicy({
        ...policyForm,
        config: JSON.parse(policyForm.config),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['policies'] })
      setPolicyForm(defaultPolicy)
    },
  })

  const disableMutation = useMutation({
    mutationFn: api.disablePolicy,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['policies'] }),
  })

  const dryRunMutation = useMutation({
    mutationFn: async () =>
      api.evaluatePolicies({
        agentId: dryRunForm.agentId,
        model: dryRunForm.model,
        promptText: dryRunForm.promptText,
        events: [
          {
            eventType: 'TOOL_CALL',
            eventName: dryRunForm.eventName,
            inputData: { command: 'deploy.sh' },
            outputData: { status: 'pending' },
          },
        ],
      }),
  })

  const policies = policiesQuery.data?.content ?? []
  const activeCount = useMemo(() => policies.filter((policy) => policy.enabled).length, [policies])

  return (
    <div className="grid gap-6 xl:grid-cols-[0.92fr_1.08fr]">
      <SectionCard eyebrow="Governance registry" title="Policy catalogue" action={<span className="eyebrow">{activeCount} active policies</span>}>
        <div className="grid gap-4">
          {policies.map((policy) => (
            <article key={policy.id} className="rounded-[1.75rem] border border-[var(--line)] bg-white/80 p-5">
              <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                <div>
                  <p className="eyebrow">{policy.policyType}</p>
                  <h3 className="mt-2 font-display text-3xl">{policy.name}</h3>
                  <p className="mt-3 text-sm leading-6 text-[var(--muted)]">{policy.description}</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <StatusPill value={policy.enabled ? 'RUNNING' : 'BLOCKED'} />
                  <StatusPill value={policy.severity} />
                </div>
              </div>
              <pre className="mt-4 overflow-auto rounded-2xl bg-stone-950 p-4 text-xs text-stone-100">
                {JSON.stringify(policy.config, null, 2)}
              </pre>
              <button
                className="mt-4 rounded-full border border-[var(--line-strong)] px-4 py-2 text-xs font-semibold uppercase tracking-[0.2em] text-[var(--ink)] transition hover:bg-[var(--accent)] hover:text-stone-50"
                onClick={() => disableMutation.mutate(policy.id)}
                type="button"
              >
                Disable
              </button>
            </article>
          ))}
        </div>
      </SectionCard>

      <div className="grid gap-6">
        <SectionCard eyebrow="Create policy" title="Author new guardrails">
          <div className="grid gap-3">
            {[
              ['name', 'Name'],
              ['description', 'Description'],
              ['policyType', 'Policy type'],
              ['scope', 'Scope'],
              ['severity', 'Severity'],
            ].map(([key, label]) => (
              <label key={key} className="grid gap-2 text-sm">
                <span className="eyebrow">{label}</span>
                <input
                  className="rounded-2xl border border-[var(--line)] bg-stone-50 px-4 py-3 outline-none transition focus:border-[var(--line-strong)]"
                  value={policyForm[key as keyof typeof policyForm]}
                  onChange={(event) => setPolicyForm((current) => ({ ...current, [key]: event.target.value }))}
                />
              </label>
            ))}
            <label className="grid gap-2 text-sm">
              <span className="eyebrow">Config JSON</span>
              <textarea
                className="min-h-[180px] rounded-3xl border border-[var(--line)] bg-stone-50 px-4 py-3 font-mono text-xs outline-none transition focus:border-[var(--line-strong)]"
                value={policyForm.config}
                onChange={(event) => setPolicyForm((current) => ({ ...current, config: event.target.value }))}
              />
            </label>
            <button
              type="button"
              onClick={() => createMutation.mutate()}
              className="mt-2 rounded-full bg-[var(--accent)] px-5 py-3 text-xs font-semibold uppercase tracking-[0.24em] text-stone-50"
            >
              {createMutation.isPending ? 'Creating' : 'Create policy'}
            </button>
          </div>
        </SectionCard>

        <SectionCard eyebrow="Dry run" title="Simulate policy impact">
          <div className="grid gap-3">
            <label className="grid gap-2 text-sm">
              <span className="eyebrow">Agent ID</span>
              <input
                className="rounded-2xl border border-[var(--line)] bg-stone-50 px-4 py-3 outline-none transition focus:border-[var(--line-strong)]"
                value={dryRunForm.agentId}
                onChange={(event) => setDryRunForm((current) => ({ ...current, agentId: event.target.value }))}
              />
            </label>
            <label className="grid gap-2 text-sm">
              <span className="eyebrow">Prompt</span>
              <textarea
                className="min-h-[120px] rounded-3xl border border-[var(--line)] bg-stone-50 px-4 py-3 outline-none transition focus:border-[var(--line-strong)]"
                value={dryRunForm.promptText}
                onChange={(event) => setDryRunForm((current) => ({ ...current, promptText: event.target.value }))}
              />
            </label>
            <label className="grid gap-2 text-sm">
              <span className="eyebrow">Tool event</span>
              <input
                className="rounded-2xl border border-[var(--line)] bg-stone-50 px-4 py-3 outline-none transition focus:border-[var(--line-strong)]"
                value={dryRunForm.eventName}
                onChange={(event) => setDryRunForm((current) => ({ ...current, eventName: event.target.value }))}
              />
            </label>
            <button
              type="button"
              onClick={() => dryRunMutation.mutate()}
              className="rounded-full border border-[var(--line-strong)] px-5 py-3 text-xs font-semibold uppercase tracking-[0.24em]"
            >
              Run simulation
            </button>
          </div>
          {dryRunMutation.data ? (
            <div className="mt-5 grid gap-4">
              <div className="flex items-center gap-2">
                <p className="eyebrow">Overall verdict</p>
                <StatusPill value={dryRunMutation.data.overallVerdict} />
              </div>
              {dryRunMutation.data.stages.map((stage) => (
                <article key={stage.stage} className="rounded-[1.5rem] border border-[var(--line)] bg-white/80 p-4">
                  <div className="flex items-center justify-between">
                    <p className="eyebrow">{stage.stage}</p>
                    <StatusPill value={stage.verdict} />
                  </div>
                  <pre className="mt-3 overflow-auto rounded-2xl bg-stone-950 p-4 text-xs text-stone-100">
                    {JSON.stringify(stage.violations, null, 2)}
                  </pre>
                </article>
              ))}
            </div>
          ) : null}
        </SectionCard>
      </div>
    </div>
  )
}
