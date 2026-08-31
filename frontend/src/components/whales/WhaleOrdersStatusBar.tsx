import { ScanSearch } from 'lucide-react'
import { useScanWhaleOrdersNow, useWhaleOrderStatus } from '../../hooks/useWhaleOrders'
import { formatRelativeTime, formatUsd } from '../../lib/format'
import { Spinner } from '../ui/Spinner'
import { Badge } from '../ui/Badge'

const EXCHANGE_LABELS: Record<string, string> = {
  BINANCE: 'Binance',
  OKX: 'OKX',
  BYBIT: 'Bybit',
  HYPERLIQUID: 'Hyperliquid',
}

export function WhaleOrdersStatusBar() {
  const { data: status } = useWhaleOrderStatus()
  const scanNow = useScanWhaleOrdersNow()
  const perExchange = Object.entries(status?.activeCountByExchange ?? {})
  const errors = Object.entries(status?.exchangeErrors ?? {})

  return (
    <div className="flex flex-col gap-2 rounded-xl border border-border-subtle bg-surface-1 px-4 py-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-x-6 gap-y-1 text-xs text-slate-400">
          <span>
            Last scan <span className="font-medium text-slate-200">{formatRelativeTime(status?.lastScanAt)}</span>
          </span>
          <span>
            Threshold <span className="font-medium text-slate-200">{formatUsd(status?.minNotionalUsd)}+</span>
          </span>
          <span>
            Active <span className="font-medium text-slate-200">{status?.activeCount ?? '—'}</span>
          </span>
          {perExchange.map(([exchange, count]) => (
            <span key={exchange} className="hidden lg:inline">
              {EXCHANGE_LABELS[exchange] ?? exchange} <span className="font-medium text-slate-200">{count}</span>
            </span>
          ))}
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

      {errors.length > 0 && (
        <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500">
          {errors.map(([exchange, message]) => (
            <Badge key={exchange} variant="short">
              {EXCHANGE_LABELS[exchange] ?? exchange}: {message}
            </Badge>
          ))}
        </div>
      )}
    </div>
  )
}
