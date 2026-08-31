import { useMemo, useState } from 'react'
import { Scale } from 'lucide-react'
import type { PositionSide } from '../../api/types'
import { useWhalePositions } from '../../hooks/useWhales'
import { Badge } from '../ui/Badge'
import { Pnl } from '../ui/Pnl'
import { Spinner } from '../ui/Spinner'
import { EmptyState } from '../ui/EmptyState'
import { formatBtc, formatUsd } from '../../lib/format'
import { RefreshingIndicator } from './ScanStatusBar'

type SideFilter = 'ALL' | PositionSide

const sideFilters: { id: SideFilter; label: string }[] = [
  { id: 'ALL', label: 'All' },
  { id: 'LONG', label: 'Long' },
  { id: 'SHORT', label: 'Short' },
]

/** Long/short split meter: two segments, 2px gap, rounded outer ends, zero-size side hidden. */
function SentimentMeter({ longPct, shortPct }: { longPct: number; shortPct: number }) {
  return (
    <div className="flex h-3 w-full gap-[2px] overflow-hidden rounded-full bg-surface-3">
      {longPct > 0 && (
        <div
          className={`h-full bg-profit ${shortPct > 0 ? 'rounded-l-full' : 'rounded-full'}`}
          style={{ width: `${longPct}%` }}
        />
      )}
      {shortPct > 0 && (
        <div
          className={`h-full bg-loss ${longPct > 0 ? 'rounded-r-full' : 'rounded-full'}`}
          style={{ width: `${shortPct}%` }}
        />
      )}
    </div>
  )
}

export function WhalePositionsSummary() {
  const { data, isLoading, isError, isFetching } = useWhalePositions()

  return (
    <div className="flex flex-col gap-4 rounded-xl border border-border-subtle bg-surface-1 px-4 py-4">
      <div>
        <h2 className="flex items-center gap-2 text-sm font-semibold text-slate-100">
          <Scale size={15} className="text-slate-400" />
          BTC Position Sentiment
          <RefreshingIndicator isFetching={isFetching} />
        </h2>
        <p className="text-xs text-slate-500">
          Long vs short split across tracked whales&apos; open BTC positions on Hyperliquid.
        </p>
      </div>

      {isLoading && (
        <div className="flex items-center justify-center py-6 text-slate-500">
          <Spinner size={20} />
        </div>
      )}

      {isError && (
        <EmptyState icon={Scale} title="Couldn't load positions" description="Check the API and try again." />
      )}

      {!isLoading && !isError && data && data.positions.length === 0 && (
        <EmptyState
          icon={Scale}
          title="No open BTC positions"
          description="None of the tracked whales currently hold a BTC position on Hyperliquid."
        />
      )}

      {!isLoading && !isError && data && data.positions.length > 0 && (
        <PositionBreakdown data={data} />
      )}
    </div>
  )
}

function PositionBreakdown({ data }: { data: NonNullable<ReturnType<typeof useWhalePositions>['data']> }) {
  const { summary, positions } = data
  const [sideFilter, setSideFilter] = useState<SideFilter>('ALL')
  const totalSize = Number(summary.longBtcSize) + Number(summary.shortBtcSize)
  const longPct = totalSize > 0 ? (Number(summary.longBtcSize) / totalSize) * 100 : 0
  const shortPct = totalSize > 0 ? 100 - longPct : 0

  const filteredPositions = useMemo(
    () => (sideFilter === 'ALL' ? positions : positions.filter((p) => p.side === sideFilter)),
    [positions, sideFilter],
  )

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col gap-2">
        <SentimentMeter longPct={longPct} shortPct={shortPct} />
        <div className="flex items-center justify-between text-xs">
          <div className="flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full bg-profit" />
            <span className="font-medium text-slate-200">Long</span>
            <span className="text-slate-500">
              {longPct.toFixed(0)}% · {formatBtc(summary.longBtcSize)} · {summary.longCount} whale
              {summary.longCount === 1 ? '' : 's'}
            </span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="text-slate-500">
              {summary.shortCount} whale{summary.shortCount === 1 ? '' : 's'} · {formatBtc(summary.shortBtcSize)} ·{' '}
              {shortPct.toFixed(0)}%
            </span>
            <span className="font-medium text-slate-200">Short</span>
            <span className="h-2 w-2 rounded-full bg-loss" />
          </div>
        </div>
      </div>

      {summary.majoritySide && (
        <p className="text-xs text-slate-500">
          Whales are net{' '}
          <span className={summary.majoritySide === 'LONG' ? 'font-semibold text-profit' : 'font-semibold text-loss'}>
            {summary.majoritySide === 'LONG' ? 'long' : 'short'}
          </span>{' '}
          — {summary.majorityBtcSharePct}% of tracked BTC exposure ({formatUsd(summary.longNotionalUsd)} long vs{' '}
          {formatUsd(summary.shortNotionalUsd)} short by notional).
        </p>
      )}

      <div className="flex items-center justify-between gap-2">
        <div className="flex gap-1 rounded-lg border border-border-subtle bg-surface-2 p-1">
          {sideFilters.map(({ id, label }) => (
            <button
              key={id}
              type="button"
              onClick={() => setSideFilter(id)}
              className={`rounded-md px-2.5 py-1 text-xs font-medium transition-colors ${
                sideFilter === id ? 'bg-surface-3 text-slate-100' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
        <span className="text-xs text-slate-500">
          {filteredPositions.length} of {positions.length}
        </span>
      </div>

      {filteredPositions.length === 0 ? (
        <EmptyState icon={Scale} title="No positions match this filter" />
      ) : (
        <div className="overflow-x-auto rounded-lg border border-border-subtle">
          <table className="w-full min-w-[640px] text-sm">
            <thead>
              <tr className="border-b border-border-subtle text-left text-xs uppercase tracking-wide text-slate-500">
                <th className="px-3 py-2 font-medium">Whale</th>
                <th className="px-3 py-2 font-medium">Side</th>
                <th className="px-3 py-2 text-right font-medium">Leverage</th>
                <th className="px-3 py-2 text-right font-medium">Size</th>
                <th className="px-3 py-2 text-right font-medium">Notional</th>
                <th className="px-3 py-2 text-right font-medium">Unrealized PnL</th>
              </tr>
            </thead>
            <tbody>
              {filteredPositions.map((p) => (
                  <tr key={p.walletId} className="border-b border-border-subtle/60 last:border-b-0">
                    <td className="px-3 py-2">
                      <span className="break-all font-mono text-xs text-slate-200">{p.address}</span>
                      {p.label && <p className="truncate text-xs text-slate-500">{p.label}</p>}
                    </td>
                    <td className="px-3 py-2">
                      <Badge variant={p.side === 'LONG' ? 'long' : 'short'}>{p.side}</Badge>
                    </td>
                    <td className="px-3 py-2 text-right font-mono text-xs text-slate-200">
                      {p.leverage ? `${p.leverage}x` : '—'}
                    </td>
                    <td className="px-3 py-2 text-right font-mono text-xs text-slate-200">{formatBtc(p.size)}</td>
                    <td className="px-3 py-2 text-right font-mono text-xs text-slate-200">
                      {formatUsd(p.notionalUsd)}
                    </td>
                    <td className="px-3 py-2 text-right">
                      <Pnl value={p.unrealizedPnl} size="sm" />
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
