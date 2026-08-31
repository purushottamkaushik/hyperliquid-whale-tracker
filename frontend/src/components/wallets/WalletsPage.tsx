import { useMemo, useState } from 'react'
import { Radar, Search, X } from 'lucide-react'
import { useWalletOverview } from '../../hooks/useWallets'
import { useMarkHighPnlWallets } from '../../hooks/useWhales'
import { AddWalletForm } from './AddWalletForm'
import { WalletList } from './WalletList'
import { ActivityPanel } from './ActivityPanel'
import { CoinglassPanel } from './CoinglassPanel'
import { Spinner } from '../ui/Spinner'
import { Modal } from '../ui/Modal'
import { formatUsd } from '../../lib/format'
import type { WalletOverviewResponse, WalletResponse } from '../../api/types'

function toWalletResponse(w: WalletOverviewResponse): WalletResponse {
  return {
    id: w.walletId,
    address: w.address,
    label: w.label,
    chain: w.chain,
    source: w.source,
    createdAt: w.createdAt,
    marked: w.marked,
    active: w.active,
  }
}

export function WalletsPage() {
  const { data: wallets, isLoading } = useWalletOverview()
  const [selected, setSelected] = useState<WalletResponse | null>(null)
  const [coinglassAddress, setCoinglassAddress] = useState<string | null>(null)
  const [profitableOnly, setProfitableOnly] = useState(false)
  const [markedOnly, setMarkedOnly] = useState(false)
  const [showInactive, setShowInactive] = useState(false)
  const [search, setSearch] = useState('')
  const markHighPnl = useMarkHighPnlWallets()

  const visibleWallets = useMemo(() => {
    let result = wallets ?? []
    if (!showInactive) {
      result = result.filter((w) => w.active)
    }
    if (profitableOnly) {
      result = result.filter((w) => w.totalPnl !== null && Number(w.totalPnl) > 0)
    }
    if (markedOnly) {
      result = result.filter((w) => w.chain === 'HYPERLIQUID' && w.marked)
    }
    const query = search.trim().toLowerCase()
    if (query) {
      result = result.filter(
        (w) => w.address.toLowerCase().includes(query) || (w.label ?? '').toLowerCase().includes(query),
      )
    }
    return result
  }, [wallets, profitableOnly, markedOnly, showInactive, search])

  return (
    <div className="flex flex-col gap-8">
      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold text-slate-100">Add wallet</h2>
        <AddWalletForm />
      </section>

      <section className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-slate-100">
            Saved wallets
            {wallets ? ` (${visibleWallets.length}${visibleWallets.length !== wallets.length ? ` of ${wallets.length}` : ''})` : ''}
          </h2>
          <div className="flex flex-wrap items-center gap-4">
            <div className="relative">
              <Search size={13} className="pointer-events-none absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-500" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search address or label"
                className="w-56 rounded-lg border border-border-subtle bg-surface-3 py-1.5 pl-8 pr-7 text-sm text-slate-200 placeholder:text-slate-500 focus:border-accent focus:outline-none"
              />
              {search && (
                <button
                  type="button"
                  onClick={() => setSearch('')}
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
                  title="Clear search"
                >
                  <X size={13} />
                </button>
              )}
            </div>
            <label className="inline-flex select-none items-center gap-2 text-sm text-slate-400">
              <input
                type="checkbox"
                checked={profitableOnly}
                onChange={(e) => setProfitableOnly(e.target.checked)}
                className="h-3.5 w-3.5 rounded border-border-subtle bg-surface-3 accent-accent"
              />
              Only show wallets with positive total profit
            </label>
            <label className="inline-flex select-none items-center gap-2 text-sm text-slate-400">
              <input
                type="checkbox"
                checked={markedOnly}
                onChange={(e) => setMarkedOnly(e.target.checked)}
                className="h-3.5 w-3.5 rounded border-border-subtle bg-surface-3 accent-accent"
              />
              Only show marked Hyperliquid wallets
            </label>
            <label className="inline-flex select-none items-center gap-2 text-sm text-slate-400">
              <input
                type="checkbox"
                checked={showInactive}
                onChange={(e) => setShowInactive(e.target.checked)}
                className="h-3.5 w-3.5 rounded border-border-subtle bg-surface-3 accent-accent"
              />
              Show deactivated wallets
            </label>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={() => markHighPnl.mutate()}
            disabled={markHighPnl.isPending}
            className="inline-flex items-center gap-1.5 rounded-lg border border-border-subtle bg-surface-2 px-3 py-1.5 text-sm font-medium text-slate-300 transition-colors hover:border-accent hover:text-accent disabled:opacity-60"
            title="Check every tracked Hyperliquid wallet's leaderboard total PnL and mark the large ones automatically"
          >
            {markHighPnl.isPending ? <Spinner size={14} /> : <Radar size={14} />}
            Scan &amp; mark
          </button>
          {markHighPnl.data && (
            <span className="text-xs text-slate-500">
              Checked {markHighPnl.data.walletsChecked} · marked {markHighPnl.data.newlyMarked} new (over{' '}
              {formatUsd(markHighPnl.data.minTotalPnl)})
              {markHighPnl.data.skippedNoLeaderboardData > 0 &&
                ` · ${markHighPnl.data.skippedNoLeaderboardData} skipped (not on the current leaderboard)`}
            </span>
          )}
          {markHighPnl.isError && (
            <span className="text-xs text-loss">
              Scan failed: {markHighPnl.error instanceof Error ? markHighPnl.error.message : 'unknown error'}
            </span>
          )}
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center py-14 text-slate-500">
            <Spinner size={20} />
          </div>
        ) : (
          <WalletList
            wallets={visibleWallets}
            selectedId={selected?.id ?? null}
            onSelectTransactions={(w) => setSelected(toWalletResponse(w))}
            onOpenCoinglass={setCoinglassAddress}
          />
        )}
      </section>

      {selected && (
        <Modal title={`Transactions — ${selected.address}`} onClose={() => setSelected(null)}>
          <ActivityPanel wallet={selected} />
        </Modal>
      )}

      <CoinglassPanel address={coinglassAddress} onClose={() => setCoinglassAddress(null)} />
    </div>
  )
}
