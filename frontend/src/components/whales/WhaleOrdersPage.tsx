import { useEffect, useRef, useState } from 'react'
import { Layers } from 'lucide-react'
import { useScanWhaleOrdersNow, useWhaleOrders } from '../../hooks/useWhaleOrders'
import { WhaleOrdersStatusBar } from './WhaleOrdersStatusBar'
import { RefreshingIndicator } from './ScanStatusBar'
import { Badge } from '../ui/Badge'
import { EmptyState } from '../ui/EmptyState'
import { RelativeTime } from '../ui/RelativeTime'
import { Spinner } from '../ui/Spinner'
import { formatBtc, formatUsd } from '../../lib/format'

const EXCHANGE_LABELS: Record<string, string> = {
  BINANCE: 'Binance',
  OKX: 'OKX',
  BYBIT: 'Bybit',
  HYPERLIQUID: 'Hyperliquid',
}

const exchangeOptions = [
  { id: '', label: 'All exchanges' },
  { id: 'BINANCE', label: 'Binance' },
  { id: 'OKX', label: 'OKX' },
  { id: 'BYBIT', label: 'Bybit' },
  { id: 'HYPERLIQUID', label: 'Hyperliquid' },
]

const sideOptions = [
  { id: '', label: 'Both sides' },
  { id: 'BUY', label: 'Buy (bids)' },
  { id: 'SELL', label: 'Sell (asks)' },
]

export function WhaleOrdersPage() {
  const [exchange, setExchange] = useState('')
  const [side, setSide] = useState('')
  const [includeRemoved, setIncludeRemoved] = useState(false)

  // Force a scan on first mount rather than waiting for the next scheduled tick, so the page
  // isn't empty on first load just because the background scan hasn't run yet.
  const scanNow = useScanWhaleOrdersNow()
  const triggeredInitialScan = useRef(false)
  useEffect(() => {
    if (triggeredInitialScan.current) return
    triggeredInitialScan.current = true
    scanNow.mutate()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const { data: orders, isLoading, isError, isFetching } = useWhaleOrders({
    exchange: exchange || undefined,
    side: side || undefined,
    includeRemoved,
  })

  const rows = orders ?? []

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-100">
          Whale Orders
          <RefreshingIndicator isFetching={isFetching} />
        </h2>
        <p className="text-sm text-slate-500">
          Large resting BTC limit orders tracked across exchange order books over time - Coinglass "large
          orderbook statistics" style, not the live/executed trade tape on the Live Trades tab.
        </p>
      </div>

      <WhaleOrdersStatusBar />

      <div className="flex flex-wrap items-center gap-2">
        <select
          value={exchange}
          onChange={(e) => setExchange(e.target.value)}
          className="rounded-lg border border-border-subtle bg-surface-2 px-2.5 py-1.5 text-sm text-slate-200 focus:border-accent focus:outline-none"
        >
          {exchangeOptions.map((o) => (
            <option key={o.id} value={o.id}>
              {o.label}
            </option>
          ))}
        </select>

        <select
          value={side}
          onChange={(e) => setSide(e.target.value)}
          className="rounded-lg border border-border-subtle bg-surface-2 px-2.5 py-1.5 text-sm text-slate-200 focus:border-accent focus:outline-none"
        >
          {sideOptions.map((o) => (
            <option key={o.id} value={o.id}>
              {o.label}
            </option>
          ))}
        </select>

        <label className="inline-flex items-center gap-2 text-sm text-slate-400">
          <input
            type="checkbox"
            checked={includeRemoved}
            onChange={(e) => setIncludeRemoved(e.target.checked)}
            className="h-3.5 w-3.5 rounded border-border-subtle accent-accent"
          />
          Show removed orders
        </label>

        {!isLoading && !isError && <span className="text-xs text-slate-500">{rows.length}</span>}
      </div>

      {(isLoading || (scanNow.isPending && rows.length === 0)) && (
        <div className="flex items-center justify-center py-16 text-slate-500">
          <Spinner size={22} />
        </div>
      )}

      {isError && (
        <EmptyState icon={Layers} title="Couldn't load whale orders" description="Check the API and try again." />
      )}

      {!isLoading && !isError && !scanNow.isPending && rows.length === 0 && (
        <EmptyState
          icon={Layers}
          title="No whale orders tracked yet"
          description="No resting order currently clears the whale threshold on the exchanges checked - try Scan now, or widen the filters above."
        />
      )}

      {!isLoading && rows.length > 0 && (
        <div className="overflow-x-auto rounded-xl border border-border-subtle bg-surface-2">
          <table className="w-full min-w-[780px] text-sm">
            <thead>
              <tr className="border-b border-border-subtle text-left text-xs uppercase tracking-wide text-slate-500">
                <th className="px-4 py-3 font-medium">Exchange</th>
                <th className="px-4 py-3 font-medium">Side</th>
                <th className="px-4 py-3 text-right font-medium">Price</th>
                <th className="px-4 py-3 text-right font-medium">Size</th>
                <th className="px-4 py-3 text-right font-medium">Notional</th>
                <th className="px-4 py-3 font-medium">First seen</th>
                <th className="px-4 py-3 font-medium">Last seen</th>
                <th className="px-4 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((o) => (
                <tr key={o.id} className="border-b border-border-subtle/60 text-slate-300 last:border-b-0">
                  <td className="px-4 py-2">{EXCHANGE_LABELS[o.exchange] ?? o.exchange}</td>
                  <td className="px-4 py-2">
                    <Badge variant={o.side === 'BUY' ? 'long' : 'short'}>{o.side}</Badge>
                  </td>
                  <td className="px-4 py-2 text-right font-mono">{formatUsd(o.price, true)}</td>
                  <td className="px-4 py-2 text-right font-mono">{formatBtc(o.size)}</td>
                  <td className="px-4 py-2 text-right font-mono">{formatUsd(o.notionalUsd)}</td>
                  <td className="px-4 py-2 text-xs text-slate-400">
                    <RelativeTime value={o.firstSeenAt} />
                  </td>
                  <td className="px-4 py-2 text-xs text-slate-400">
                    <RelativeTime value={o.status === 'ACTIVE' ? o.lastSeenAt : o.removedAt} />
                  </td>
                  <td className="px-4 py-2">
                    <Badge variant={o.status === 'ACTIVE' ? 'accent' : 'neutral'}>{o.status}</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
