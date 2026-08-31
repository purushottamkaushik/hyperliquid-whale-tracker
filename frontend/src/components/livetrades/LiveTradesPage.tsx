import { useMemo, useState } from 'react'
import { Activity } from 'lucide-react'
import { useBtcTradeFeed } from '../../hooks/useBtcTradeFeed'
import { Badge } from '../ui/Badge'
import { EmptyState } from '../ui/EmptyState'
import { CopyButton } from '../ui/CopyButton'
import { formatUsd, shortAddress } from '../../lib/format'
import { OrderBookDepth } from './OrderBookDepth'
import { MarkedWalletPositions } from './MarkedWalletPositions'
import { StreamStatusBadge } from './StreamStatusBadge'

function formatTime(iso: string): string {
  return new Date(iso).toLocaleTimeString('en-US', { hour12: false })
}

export function LiveTradesPage() {
  const { trades, status } = useBtcTradeFeed()
  const [minNotional, setMinNotional] = useState('')
  const [minSize, setMinSize] = useState('')

  const filtered = useMemo(() => {
    const minNotionalNum = Number(minNotional)
    const hasMinNotional = minNotional !== '' && !Number.isNaN(minNotionalNum) && minNotionalNum > 0
    const minSizeNum = Number(minSize)
    const hasMinSize = minSize !== '' && !Number.isNaN(minSizeNum) && minSizeNum > 0

    if (!hasMinNotional && !hasMinSize) return trades
    return trades.filter((t) => {
      if (hasMinNotional && Number(t.notionalUsd) < minNotionalNum) return false
      if (hasMinSize && Number(t.size) < minSizeNum) return false
      return true
    })
  }, [trades, minNotional, minSize])

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h2 className="text-lg font-semibold text-slate-100">Live BTC Market</h2>
        <p className="text-sm text-slate-500">
          Real-time BTC perpetual activity on Hyperliquid: market-wide order book depth and trade tape, plus live
          position updates for your marked wallets.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <OrderBookDepth />
        <MarkedWalletPositions />
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <h3 className="flex items-center gap-2 text-sm font-semibold text-slate-100">
          Trade Tape
          <StreamStatusBadge status={status} />
        </h3>

        <div className="flex flex-wrap items-center gap-3">
          <label className="inline-flex items-center gap-2 text-sm text-slate-400">
            Min size (BTC)
            <input
              type="number"
              min={0}
              step="any"
              value={minSize}
              onChange={(e) => setMinSize(e.target.value)}
              placeholder="e.g. 0.5"
              className="w-28 rounded-lg border border-border-subtle bg-surface-2 px-2.5 py-1.5 text-sm text-slate-200 placeholder:text-slate-500 focus:border-accent focus:outline-none"
            />
          </label>
          <label className="inline-flex items-center gap-2 text-sm text-slate-400">
            Min notional (USD)
            <input
              type="number"
              min={0}
              value={minNotional}
              onChange={(e) => setMinNotional(e.target.value)}
              placeholder="e.g. 100000"
              className="w-36 rounded-lg border border-border-subtle bg-surface-2 px-2.5 py-1.5 text-sm text-slate-200 placeholder:text-slate-500 focus:border-accent focus:outline-none"
            />
          </label>
        </div>
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          icon={Activity}
          title={trades.length === 0 ? 'Waiting for trades…' : 'No trades match your filter'}
          description={
            trades.length === 0
              ? 'New BTC futures trades will appear here in real time as they happen.'
              : 'Try a lower minimum size or notional.'
          }
        />
      ) : (
        <div className="overflow-x-auto rounded-xl border border-border-subtle bg-surface-2">
          <table className="w-full min-w-[820px] text-sm">
            <thead>
              <tr className="border-b border-border-subtle text-left text-xs uppercase tracking-wide text-slate-500">
                <th className="px-4 py-3 font-medium">Time</th>
                <th className="px-4 py-3 font-medium">Side</th>
                <th className="px-4 py-3 text-right font-medium">Price</th>
                <th className="px-4 py-3 text-right font-medium">Size (BTC)</th>
                <th className="px-4 py-3 text-right font-medium">Notional</th>
                <th className="px-4 py-3 font-medium">Taker</th>
                <th className="px-4 py-3 font-medium">Maker</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((t) => (
                <tr key={t.tradeId} className="border-b border-border-subtle/60 text-slate-300 last:border-b-0">
                  <td className="px-4 py-2 font-mono text-xs text-slate-400">{formatTime(t.time)}</td>
                  <td className="px-4 py-2">
                    <Badge variant={t.side === 'BUY' ? 'long' : 'short'}>{t.side}</Badge>
                  </td>
                  <td className="px-4 py-2 text-right font-mono">{formatUsd(t.price, true)}</td>
                  <td className="px-4 py-2 text-right font-mono">{t.size}</td>
                  <td className="px-4 py-2 text-right font-mono">{formatUsd(t.notionalUsd, true)}</td>
                  <td className="px-4 py-2">
                    <div className="flex items-center gap-1.5">
                      <span className="font-mono text-xs" title={t.takerAddress}>
                        {shortAddress(t.takerAddress)}
                      </span>
                      <CopyButton value={t.takerAddress} />
                    </div>
                  </td>
                  <td className="px-4 py-2">
                    <div className="flex items-center gap-1.5">
                      <span className="font-mono text-xs" title={t.makerAddress}>
                        {shortAddress(t.makerAddress)}
                      </span>
                      <CopyButton value={t.makerAddress} />
                    </div>
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
