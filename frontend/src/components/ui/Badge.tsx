import type { ReactNode } from 'react'

const variants = {
  neutral: 'bg-surface-3 text-slate-300 border-border-subtle',
  accent: 'bg-sky-500/10 text-accent border-sky-500/30',
  whale: 'bg-violet-500/10 text-violet-300 border-violet-500/30',
  long: 'bg-profit/10 text-profit border-profit/30',
  short: 'bg-loss/10 text-loss border-loss/30',
} as const

interface BadgeProps {
  children: ReactNode
  variant?: keyof typeof variants
}

export function Badge({ children, variant = 'neutral' }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[11px] font-medium uppercase tracking-wide ${variants[variant]}`}
    >
      {children}
    </span>
  )
}
