import type { PropsWithChildren, ReactNode } from 'react'

type SectionCardProps = PropsWithChildren<{
  title: string
  eyebrow?: string
  action?: ReactNode
}>

export function SectionCard({ title, eyebrow, action, children }: SectionCardProps) {
  return (
    <section className="panel">
      <header className="mb-5 flex flex-col gap-3 border-b border-[var(--line)] pb-4 md:flex-row md:items-end md:justify-between">
        <div>
          {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
          <h2 className="font-display text-3xl text-[var(--ink)]">{title}</h2>
        </div>
        {action}
      </header>
      {children}
    </section>
  )
}
