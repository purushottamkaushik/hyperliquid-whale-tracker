import { RefreshCw, ScanSearch } from 'lucide-react'
import { useScanNow, useWhaleStatus } from '../../hooks/useWhales'
import { formatRelativeTime, formatUsd } from '../../lib/format'
import { Spinner } from '../ui/Spinner'

export function ScanStatusBar() {
  const { data: status } = useWhaleStatus()
  const scanNow = useScanNow()

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border-subtle bg-surface-1 px-4 py-3">
      <div className="flex flex-wrap items-center gap-x-6 gap-y-1 text-xs text-slate-400">
        <span>
          Last scan{' '}
          <span className="font-medium text-slate-200">
            {formatRelativeTime(status?.lastScanAt)}
          </span>
        </span>
        <span>
          Candidates <span className="font-medium text-slate-200">{status?.lastCandidateCount ?? '—'}</span>
        </span>
        <span>
          Newly discovered{' '}
          <span className="font-medium text-slate-200">{status?.lastDiscoveredCount ?? '—'}</span>
        </span>
        <span className="hidden lg:inline">
          Min account value{' '}
          <span className="font-medium text-slate-200">{formatUsd(status?.minAccountValue)}</span>
        </span>
        <span className="hidden lg:inline">
          Max leverage{' '}
          <span className="font-medium text-slate-200">
            {status?.maxLeverage ? `${status.maxLeverage}x` : '—'}
          </span>
        </span>
      </div>

      <button
        type="button"
        onClick={() => scanNow.mutate()}
        disabled={scanNow.isPending}
        className="inline-flex items-center gap-1.5 rounded-lg bg-accent/10 px-3 py-1.5 text-xs font-medium text-accent transition-colors hover:bg-accent/20 disabled:opacity-60"
      >
        {scanNow.isPending ? <Spinner size={13} /> : <ScanSearch size={13} />}
        Scan now
      </button>
    </div>
  )
}

export function RefreshingIndicator({ isFetching }: { isFetching: boolean }) {
  if (!isFetching) return null
  return <RefreshCw size={12} className="animate-spin text-slate-500" />
}
