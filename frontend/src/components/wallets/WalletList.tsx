import { Fragment, useMemo, useState } from 'react'
import {
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Eye,
  EyeOff,
  ExternalLink,
  Star,
  Trash2,
  Wallet,
} from 'lucide-react'
import type { WalletOverviewResponse } from '../../api/types'
import { useDeleteWallet, useSetActive, useSetMarked } from '../../hooks/useWallets'
import { Badge } from '../ui/Badge'
import { EmptyState } from '../ui/EmptyState'
import { CopyButton } from '../ui/CopyButton'
import { Pnl } from '../ui/Pnl'
import { RelativeTime } from '../ui/RelativeTime'
import { formatDateTime, shortAddress } from '../../lib/format'

const PAGE_SIZE = 25

interface WalletListProps {
  wallets: WalletOverviewResponse[]
  selectedId: number | null
  onSelectTransactions: (wallet: WalletOverviewResponse) => void
  onOpenCoinglass: (address: string) => void
}

type SortField =
  | 'chain'
  | 'address'
  | 'label'
  | 'createdAt'
  | 'totalPnl'
  | 'totalPnlBtc'
  | 'unrealizedPnl'
  | 'lastSyncedAt'
type SortDirection = 'asc' | 'desc'

const columns: { field: SortField; label: string; align?: 'right' }[] = [
  { field: 'chain', label: 'Chain' },
  { field: 'address', label: 'Address' },
  { field: 'label', label: 'Label' },
  { field: 'createdAt', label: 'Tracked since' },
  { field: 'totalPnl', label: 'Total profit (USD)', align: 'right' },
  { field: 'totalPnlBtc', label: 'Total profit (BTC)', align: 'right' },
  { field: 'unrealizedPnl', label: 'Unrealized PnL (USD)', align: 'right' },
  { field: 'lastSyncedAt', label: 'Last synced' },
]

function toNumber(value: string | null | undefined): number | null {
  if (value === null || value === undefined) return null
  const n = Number(value)
  return Number.isNaN(n) ? null : n
}

function SortButton({
  field,
  label,
  align,
  activeField,
  direction,
  onSort,
}: {
  field: SortField
  label: string
  align?: 'right'
  activeField: SortField
  direction: SortDirection
  onSort: (field: SortField) => void
}) {
  const active = activeField === field
  const Icon = active ? (direction === 'asc' ? ArrowUp : ArrowDown) : ArrowUpDown
  return (
    <button
      type="button"
      onClick={() => onSort(field)}
      className={`inline-flex items-center gap-1 transition-colors hover:text-slate-200 ${
        active ? 'text-slate-200' : 'text-slate-500'
      } ${align === 'right' ? 'flex-row-reverse' : ''}`}
    >
      {label}
      <Icon size={12} className={active ? '' : 'opacity-40'} />
    </button>
  )
}

/** Inline BTC position + open orders detail shown when a Hyperliquid row is expanded. */
function BtcActivityDetail({ wallet }: { wallet: WalletOverviewResponse }) {
  const position = wallet.positions[0]
  const orders = wallet.openOrders

  if (!position && orders.length === 0) {
    return <p className="px-6 py-4 text-xs text-slate-500">No open BTC position or orders right now.</p>
  }

  return (
    <div className="flex flex-col gap-4 px-6 py-4">
      {position && (
        <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-xs text-slate-300">
          <span className="text-slate-500">BTC position</span>
          <Badge variant={position.side === 'LONG' ? 'long' : 'short'}>{position.side}</Badge>
          <span>
            Size <span className="font-mono text-slate-200">{position.size}</span>
          </span>
          <span>
            Entry <span className="font-mono text-slate-200">{position.entryPrice ?? '—'}</span>
          </span>
          <span>
            Notional <span className="font-mono text-slate-200">{position.positionValue ?? '—'}</span>
          </span>
          <span>
            Leverage <span className="font-mono text-slate-200">{position.leverage ? `${position.leverage}x` : '—'}</span>
          </span>
          <Pnl value={position.unrealizedPnl} size="sm" />
        </div>
      )}

      {orders.length > 0 && (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[560px] text-xs">
            <thead>
              <tr className="border-b border-border-subtle text-left uppercase tracking-wide text-slate-500">
                <th className="py-1.5 pr-4 font-medium">Time</th>
                <th className="py-1.5 pr-4 font-medium">Side</th>
                <th className="py-1.5 pr-4 font-medium">Type</th>
                <th className="py-1.5 pr-4 font-medium">Price</th>
                <th className="py-1.5 pr-4 font-medium">Size</th>
                <th className="py-1.5 pr-4 font-medium">Original</th>
                <th className="py-1.5 font-medium">Reduce only</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((o) => (
                <tr key={o.orderId} className="border-b border-border-subtle/40 text-slate-300 last:border-b-0">
                  <td className="py-1.5 pr-4">{formatDateTime(o.time)}</td>
                  <td className="py-1.5 pr-4">{o.side}</td>
                  <td className="py-1.5 pr-4">{o.orderType}</td>
                  <td className="py-1.5 pr-4 font-mono">{o.price}</td>
                  <td className="py-1.5 pr-4 font-mono">{o.size}</td>
                  <td className="py-1.5 pr-4 font-mono">{o.originalSize}</td>
                  <td className="py-1.5">{o.reduceOnly ? 'Yes' : 'No'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export function WalletList({ wallets, selectedId, onSelectTransactions, onOpenCoinglass }: WalletListProps) {
  const deleteWallet = useDeleteWallet()
  const setMarked = useSetMarked()
  const setActive = useSetActive()
  const [sortField, setSortField] = useState<SortField>('createdAt')
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc')
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [page, setPage] = useState(1)
  const [prevWalletCount, setPrevWalletCount] = useState(wallets.length)

  function toggleSort(field: SortField) {
    if (field === sortField) {
      setSortDirection((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortField(field)
      setSortDirection('asc')
    }
    setPage(1)
  }

  // Wallets tracked automatically can number in the hundreds+, so this list is paged rather than
  // rendered in full - jump back to page 1 whenever the filtered set's size changes (a new
  // search/profit filter) so a stale page number doesn't leave the view empty. Compared by
  // length, not array reference, so the routine background refetch (same wallets, new array)
  // doesn't reset the page a user is currently reading. Adjusted during render (React's
  // recommended pattern for this) rather than an effect, to avoid an extra render pass.
  if (wallets.length !== prevWalletCount) {
    setPrevWalletCount(wallets.length)
    setPage(1)
  }

  const sorted = useMemo(() => {
    const dir = sortDirection === 'asc' ? 1 : -1
    return [...wallets].sort((a, b) => {
      switch (sortField) {
        case 'chain':
          return a.chain.localeCompare(b.chain) * dir
        case 'address':
          return a.address.localeCompare(b.address) * dir
        case 'label':
          return (a.label ?? '').localeCompare(b.label ?? '') * dir
        case 'createdAt':
          return (new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()) * dir
        case 'totalPnl': {
          const pa = toNumber(a.totalPnl)
          const pb = toNumber(b.totalPnl)
          if (pa === null) return pb === null ? 0 : 1
          if (pb === null) return -1
          return (pa - pb) * dir
        }
        case 'totalPnlBtc': {
          const pa = toNumber(a.totalPnlBtc)
          const pb = toNumber(b.totalPnlBtc)
          if (pa === null) return pb === null ? 0 : 1
          if (pb === null) return -1
          return (pa - pb) * dir
        }
        case 'unrealizedPnl': {
          const pa = toNumber(a.unrealizedPnl)
          const pb = toNumber(b.unrealizedPnl)
          if (pa === null) return pb === null ? 0 : 1
          if (pb === null) return -1
          return (pa - pb) * dir
        }
        case 'lastSyncedAt': {
          const ta = a.lastSyncedAt ? new Date(a.lastSyncedAt).getTime() : null
          const tb = b.lastSyncedAt ? new Date(b.lastSyncedAt).getTime() : null
          if (ta === null) return tb === null ? 0 : 1
          if (tb === null) return -1
          return (ta - tb) * dir
        }
        default:
          return 0
      }
    })
  }, [wallets, sortField, sortDirection])

  const pageCount = Math.max(1, Math.ceil(sorted.length / PAGE_SIZE))
  const currentPage = Math.min(page, pageCount)
  const pageStart = (currentPage - 1) * PAGE_SIZE
  const paged = sorted.slice(pageStart, pageStart + PAGE_SIZE)

  if (wallets.length === 0) {
    return (
      <EmptyState
        icon={Wallet}
        title="No wallets saved"
        description="Add a Bitcoin or Hyperliquid address above to start tracking it."
      />
    )
  }

  return (
    <div className="rounded-xl border border-border-subtle bg-surface-2">
      <div className="overflow-x-auto">
      <table className="w-full min-w-[960px] text-sm">
        <thead>
          <tr className="border-b border-border-subtle text-left text-xs uppercase tracking-wide text-slate-500">
            <th className="w-8 px-2 py-3" />
            {columns.map((col) => (
              <th key={col.field} className={`px-4 py-3 font-medium ${col.align === 'right' ? 'text-right' : ''}`}>
                <SortButton
                  field={col.field}
                  label={col.label}
                  align={col.align}
                  activeField={sortField}
                  direction={sortDirection}
                  onSort={toggleSort}
                />
              </th>
            ))}
            <th className="px-4 py-3 text-right font-medium">Actions</th>
          </tr>
        </thead>
        <tbody>
          {paged.map((w) => {
            const expandable = w.chain === 'HYPERLIQUID'
            const expanded = expandable && expandedId === w.walletId
            return (
              <Fragment key={w.walletId}>
                <tr
                  className={`border-b border-border-subtle/60 transition-colors last:border-b-0 hover:bg-surface-3/40 ${
                    selectedId === w.walletId ? 'bg-surface-3/60' : ''
                  } ${w.active ? '' : 'opacity-50'}`}
                >
                  <td className="px-2 py-3">
                    {expandable && (
                      <button
                        type="button"
                        onClick={() => setExpandedId(expanded ? null : w.walletId)}
                        className="flex h-5 w-5 items-center justify-center rounded text-slate-500 transition-colors hover:text-slate-200"
                        title={expanded ? 'Hide BTC position/orders' : 'Show BTC position/orders'}
                      >
                        {expanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                      </button>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap items-center gap-1.5">
                      <Badge variant={w.chain === 'HYPERLIQUID' ? 'accent' : 'neutral'}>{w.chain}</Badge>
                      {w.source === 'AUTO_WHALE' && <Badge variant="whale">Whale</Badge>}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-1.5">
                      <span className="font-mono text-xs text-slate-200" title={w.address}>
                        {shortAddress(w.address)}
                      </span>
                      <CopyButton value={w.address} />
                      {w.chain === 'HYPERLIQUID' && (
                        <button
                          type="button"
                          onClick={() => onOpenCoinglass(w.address)}
                          className="text-slate-500 transition-colors hover:text-accent"
                          title="View on Coinglass"
                        >
                          <ExternalLink size={13} />
                        </button>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-slate-400">{w.label ?? '—'}</td>
                  <td className="px-4 py-3 text-slate-400">{formatDateTime(w.createdAt)}</td>
                  <td className="px-4 py-3 text-right">
                    {w.chain === 'HYPERLIQUID' ? (
                      <Pnl value={w.totalPnl} size="sm" compact />
                    ) : (
                      <span className="text-slate-600">—</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right">
                    {w.chain === 'HYPERLIQUID' ? (
                      <Pnl value={w.totalPnlBtc} size="sm" unit="btc" />
                    ) : (
                      <span className="text-slate-600">—</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right">
                    {w.chain === 'HYPERLIQUID' ? (
                      <Pnl value={w.unrealizedPnl} size="sm" />
                    ) : (
                      <span className="text-slate-600">—</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-slate-400">
                    {w.chain === 'HYPERLIQUID' ? (
                      <span title={w.lastSyncedAt ?? undefined}>
                        <RelativeTime value={w.lastSyncedAt} />
                      </span>
                    ) : (
                      <span className="text-slate-600">—</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex shrink-0 items-center justify-end gap-2">
                      {w.chain === 'HYPERLIQUID' && (
                        <button
                          type="button"
                          onClick={() => setMarked.mutate({ id: w.walletId, marked: !w.marked })}
                          disabled={setMarked.isPending}
                          className={`rounded-lg border border-border-subtle p-1.5 transition-colors disabled:opacity-60 ${
                            w.marked
                              ? 'border-amber-500/40 text-amber-400 hover:text-amber-300'
                              : 'text-slate-400 hover:text-slate-200'
                          }`}
                          title={w.marked ? 'Unmark' : 'Mark as Hyperliquid whale'}
                        >
                          <Star size={14} fill={w.marked ? 'currentColor' : 'none'} />
                        </button>
                      )}
                      {w.chain === 'BTC' && (
                        <button
                          type="button"
                          onClick={() => onSelectTransactions(w)}
                          className="rounded-lg border border-border-subtle px-3 py-1.5 text-xs font-medium text-slate-300 transition-colors hover:border-accent hover:text-accent"
                        >
                          Transactions
                        </button>
                      )}
                      <button
                        type="button"
                        onClick={() => setActive.mutate({ id: w.walletId, active: !w.active })}
                        disabled={setActive.isPending}
                        className="rounded-lg border border-border-subtle p-1.5 text-slate-400 transition-colors hover:text-slate-200 disabled:opacity-60"
                        title={w.active ? 'Deactivate (soft delete)' : 'Reactivate'}
                      >
                        {w.active ? <EyeOff size={14} /> : <Eye size={14} />}
                      </button>
                      <button
                        type="button"
                        onClick={() => deleteWallet.mutate(w.walletId)}
                        disabled={deleteWallet.isPending}
                        className="rounded-lg border border-border-subtle p-1.5 text-slate-400 transition-colors hover:border-loss/50 hover:text-loss disabled:opacity-60"
                        title="Delete permanently (can never be tracked again)"
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
                {expanded && (
                  <tr className="border-b border-border-subtle/60 bg-surface-1/60 last:border-b-0">
                    <td colSpan={columns.length + 2}>
                      <BtcActivityDetail wallet={w} />
                    </td>
                  </tr>
                )}
              </Fragment>
            )
          })}
        </tbody>
      </table>
      </div>

      {pageCount > 1 && (
        <div className="flex items-center justify-between border-t border-border-subtle px-4 py-3 text-xs text-slate-400">
          <span>
            Showing {pageStart + 1}–{Math.min(pageStart + PAGE_SIZE, sorted.length)} of {sorted.length}
          </span>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              disabled={currentPage <= 1}
              className="flex h-7 w-7 items-center justify-center rounded-lg border border-border-subtle text-slate-400 transition-colors hover:text-slate-200 disabled:opacity-40"
              title="Previous page"
            >
              <ChevronLeft size={14} />
            </button>
            <span className="tabular-nums">
              Page {currentPage} of {pageCount}
            </span>
            <button
              type="button"
              onClick={() => setPage((p) => Math.min(pageCount, p + 1))}
              disabled={currentPage >= pageCount}
              className="flex h-7 w-7 items-center justify-center rounded-lg border border-border-subtle text-slate-400 transition-colors hover:text-slate-200 disabled:opacity-40"
              title="Next page"
            >
              <ChevronRight size={14} />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
