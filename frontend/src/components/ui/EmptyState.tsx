import type { LucideIcon } from 'lucide-react'

interface EmptyStateProps {
  icon: LucideIcon
  title: string
  description?: string
}

export function EmptyState({ icon: Icon, title, description }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-border-subtle py-14 text-center">
      <Icon size={28} className="text-slate-600" strokeWidth={1.5} />
      <p className="text-sm font-medium text-slate-300">{title}</p>
      {description && <p className="max-w-xs text-xs text-slate-500">{description}</p>}
    </div>
  )
}
