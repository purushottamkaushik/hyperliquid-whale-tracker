import { useMemo, useState } from 'react'
import { Search, Waves, X } from 'lucide-react'
import { useWhales } from '../../hooks/useWhales'
import { WhaleRow } from './WhaleRow'
import { ScanStatusBar, RefreshingIndicator } from './ScanStatusBar'
import { WhalePositionsSummary } from './WhalePositionsSummary'
import { EmptyState } from '../ui/EmptyState'
import { Spinner } from '../ui/Spinner'

type ProfitFilter = 'all' | 'profitable' | 'losing'

const profitFilters: { id: ProfitFilter; label: string }[] = [
  { id: 'all', label: 'All' },
  { id: 'profitable', label: 'Profitable' },
  { id: 'losing', label: 'Losing' },
]

export function WhaleTrackerPage() {
  const { data: whales, isLoading, isError, isFetching } = useWhales()
  const [query, setQuery] = useState('')
  const [profitFilter, setProfitFilter] = useState<ProfitFilter>('all')

  const sorted = [...(whales ?? [])].sort((a, b) => {
    const pnlA = a.allTimePnl ? Number(a.allTimePnl) : -Infinity
    const pnlB = b.allTimePnl ? Number(b.allTimePnl) : -Infinity
    return pnlB - pnlA
  })

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return sorted.filter((w) => {
      if (q && !w.address.toLowerCase().includes(q) && !(w.label ?? '').toLowerCase().includes(q)) {
        return false
      }
      if (profitFilter === 'all' || w.allTimePnl === null) {
        return profitFilter === 'all'
      }
      const profitable = Number(w.allTimePnl) >= 0
      return profitFilter === 'profitable' ? profitable : !profitable
    })
  }, [sorted, query, profitFilter])

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-100">
            Whale Tracker
            <RefreshingIndicator isFetching={isFetching} />
          </h2>
          <p className="text-sm text-slate-500">
            Consistently profitable Hyperliquid wallets, auto-discovered and tracked.
          </p>
        </div>
      </div>

      <ScanStatusBar />

      <WhalePositionsSummary />

      <div className="flex flex-wrap items-center gap-2">
        <div className="relative flex-1 sm:max-w-xs">
          <Search size={14} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Filter by address or label"
            className="w-full rounded-lg border border-border-subtle bg-surface-2 py-2 pl-8 pr-8 text-sm text-slate-100 placeholder:text-slate-500 focus:border-accent focus:outline-none"
          />
          {query && (
            <button
              type="button"
              onClick={() => setQuery('')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
              title="Clear filter"
            >
              <X size={14} />
            </button>
          )}
        </div>

        <div className="flex gap-1 rounded-lg border border-border-subtle bg-surface-2 p-1">
          {profitFilters.map(({ id, label }) => (
            <button
              key={id}
              type="button"
              onClick={() => setProfitFilter(id)}
              className={`rounded-md px-2.5 py-1 text-xs font-medium transition-colors ${
                profitFilter === id ? 'bg-surface-3 text-slate-100' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        {!isLoading && !isError && (
          <span className="text-xs text-slate-500">
            {filtered.length} of {sorted.length}
          </span>
        )}
      </div>

      {isLoading && (
        <div className="flex items-center justify-center py-16 text-slate-500">
          <Spinner size={22} />
        </div>
      )}

      {isError && (
        <EmptyState icon={Waves} title="Couldn't load whales" description="Check the API and try again." />
      )}

      {!isLoading && !isError && sorted.length === 0 && (
        <EmptyState
          icon={Waves}
          title="No whales tracked yet"
          description="Run a scan, or wait for the next scheduled scan to auto-discover profitable wallets."
        />
      )}

      {!isLoading && !isError && sorted.length > 0 && filtered.length === 0 && (
        <EmptyState icon={Waves} title="No whales match your filter" description="Try a different search or filter." />
      )}

      {!isLoading && filtered.length > 0 && (
        <div className="overflow-x-auto rounded-xl border border-border-subtle bg-surface-2">
          <table className="w-full min-w-[720px] text-sm">
            <thead>
              <tr className="border-b border-border-subtle text-left text-xs uppercase tracking-wide text-slate-500">
                <th className="px-4 py-3 font-medium">Whale</th>
                <th className="px-4 py-3 font-medium">Tracked since</th>
                <th className="px-4 py-3 text-right font-medium">Account value</th>
                <th className="px-4 py-3 text-right font-medium">30d PnL</th>
                <th className="px-4 py-3 text-right font-medium">Profit</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((whale) => (
                <WhaleRow key={whale.id} whale={whale} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
