type StatusPillProps = {
  value: string | null | undefined
}

const toneMap: Record<string, string> = {
  RUNNING: 'bg-emerald-100 text-emerald-800',
  COMPLETED: 'bg-slate-900 text-stone-50',
  BLOCKED: 'bg-red-100 text-red-800',
  FAILED: 'bg-amber-100 text-amber-800',
  PENDING_APPROVAL: 'bg-orange-100 text-orange-800',
  PASS: 'bg-emerald-100 text-emerald-800',
  WARN: 'bg-amber-100 text-amber-800',
  FAIL: 'bg-red-100 text-red-800',
  APPROVED_OVERRIDE: 'bg-sky-100 text-sky-800',
  REJECTED: 'bg-red-100 text-red-800',
  WARNED: 'bg-amber-100 text-amber-800',
  BLOCKED_ACTION: 'bg-red-100 text-red-800',
}

export function StatusPill({ value }: StatusPillProps) {
  const text = value ?? 'UNKNOWN'
  const tone = toneMap[text] ?? 'bg-stone-200 text-stone-700'

  return (
    <span
      className={`inline-flex rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.22em] ${tone}`}
    >
      {text.replaceAll('_', ' ')}
    </span>
  )
}
