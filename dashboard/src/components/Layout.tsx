import { ShieldCheck, Radar, ScrollText, Scale, Siren, Waypoints } from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { dashboardConnection } from '../api/client'

const navigation = [
  { to: '/', label: 'Traces', icon: Radar },
  { to: '/trace/placeholder', label: 'Trace Detail', icon: Waypoints, hidden: true },
  { to: '/policies', label: 'Policies', icon: Scale },
  { to: '/violations', label: 'Violations', icon: Siren },
  { to: '/analytics', label: 'Analytics', icon: ScrollText },
]

export function Layout() {
  return (
    <div className="min-h-screen bg-[var(--paper)] text-[var(--ink)]">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(circle_at_top_left,rgba(200,63,28,0.12),transparent_28%),radial-gradient(circle_at_bottom_right,rgba(28,76,121,0.12),transparent_34%)]" />
      <div className="relative mx-auto flex min-h-screen max-w-[1500px] flex-col gap-6 px-4 py-4 lg:flex-row lg:px-6">
        <aside className="panel flex w-full flex-col gap-8 lg:sticky lg:top-4 lg:h-[calc(100vh-2rem)] lg:w-[320px]">
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="grid h-12 w-12 place-items-center rounded-2xl border border-[var(--line-strong)] bg-[var(--accent)] text-stone-50">
                <ShieldCheck size={24} />
              </div>
              <div>
                <p className="eyebrow">Agent Observability</p>
                <h1 className="font-display text-4xl leading-none">AgentLens</h1>
              </div>
            </div>
            <p className="max-w-xs text-sm leading-6 text-[var(--muted)]">
              An editorial control room for agent traces, policy decisions, approval queues, and spend posture.
            </p>
          </div>

          <nav className="grid gap-2">
            {navigation.filter((item) => !item.hidden).map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  `group flex items-center justify-between rounded-2xl border px-4 py-3 transition ${
                    isActive
                      ? 'border-[var(--line-strong)] bg-[var(--panel-strong)]'
                      : 'border-transparent bg-stone-50/70 hover:border-[var(--line)]'
                  }`
                }
              >
                <span className="flex items-center gap-3">
                  <Icon size={16} className="text-[var(--accent)]" />
                  <span className="font-medium">{label}</span>
                </span>
                <span className="text-xs uppercase tracking-[0.24em] text-[var(--muted)] group-hover:text-[var(--ink)]">
                  Open
                </span>
              </NavLink>
            ))}
          </nav>

          <div className="mt-auto rounded-3xl border border-[var(--line)] bg-stone-50/90 p-5">
            <p className="eyebrow">Connection</p>
            <p className="mt-2 font-display text-2xl">Local operator session</p>
            <p className="mt-3 text-sm text-[var(--muted)]">
              Base URL: <span className="font-mono text-xs text-[var(--ink)]">{dashboardConnection.baseUrl}</span>
            </p>
            <p className="mt-2 text-sm text-[var(--muted)]">
              Key alias: <span className="font-mono text-xs text-[var(--ink)]">{dashboardConnection.apiKey}</span>
            </p>
          </div>
        </aside>

        <main className="flex-1 pb-8">
          <div className="mb-6 overflow-hidden rounded-[2rem] border border-[var(--line)] bg-[linear-gradient(135deg,rgba(247,243,236,0.95),rgba(255,255,255,0.65))] p-6 shadow-[0_24px_80px_rgba(32,23,17,0.08)]">
            <p className="eyebrow">Control Room</p>
            <div className="mt-4 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
              <div>
                <h2 className="font-display text-5xl leading-none md:text-6xl">Policy-aware traces, framed like a field report.</h2>
                <p className="mt-4 max-w-2xl text-sm leading-6 text-[var(--muted)]">
                  Every page is tuned for one job: show what the agent did, why governance reacted, and where operators need to intervene next.
                </p>
              </div>
              <div className="grid gap-2 text-right text-xs uppercase tracking-[0.24em] text-[var(--muted)]">
                <span>Polling every 5s</span>
                <span>Operator-only surface</span>
              </div>
            </div>
          </div>
          <Outlet />
        </main>
      </div>
    </div>
  )
}
