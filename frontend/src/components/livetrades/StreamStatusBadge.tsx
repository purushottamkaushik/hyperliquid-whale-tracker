import type { StreamStatus } from '../../hooks/useBtcTradeFeed'

export function StreamStatusBadge({ status }: { status: StreamStatus }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border px-2 py-0.5 text-[11px] font-medium uppercase tracking-wide ${
        status === 'open' ? 'border-profit/30 bg-profit/10 text-profit' : 'border-border-subtle bg-surface-3 text-slate-400'
      }`}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${status === 'open' ? 'bg-profit' : 'bg-slate-500'}`} />
      {status === 'open' ? 'Live' : status === 'reconnecting' ? 'Reconnecting' : 'Connecting'}
    </span>
  )
}
