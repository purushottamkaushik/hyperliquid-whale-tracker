import { Star } from 'lucide-react'
import { useWalletPositionFeed } from '../../hooks/useWalletPositionFeed'
import { Badge } from '../ui/Badge'
import { EmptyState } from '../ui/EmptyState'
import { Pnl } from '../ui/Pnl'
import { CopyButton } from '../ui/CopyButton'
import { shortAddress } from '../../lib/format'
import { StreamStatusBadge } from './StreamStatusBadge'

/** Live open BTC positions for marked Hyperliquid wallets, pushed over WebSocket as they change. */
export function MarkedWalletPositions() {
  const { positions, status } = useWalletPositionFeed()

  return (
    <div className="flex flex-col gap-3 rounded-xl border border-border-subtle bg-surface-2 p-4">
      <h3 className="flex items-center gap-2 text-sm font-semibold text-slate-100">
        Marked Wallets — Live Positions
        <StreamStatusBadge status={status} />
      </h3>

      {positions.length === 0 ? (
        <EmptyState
          icon={Star}
          title="No open BTC positions among marked wallets"
          description="Mark Hyperliquid wallets on the Wallets screen to track their positions here live."
        />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full table-fixed text-sm">
            <thead>
              <tr className="border-b border-border-subtle text-left text-xs uppercase tracking-wide text-slate-500">
                <th className="w-[26%] py-2 pr-2 font-medium">Wallet</th>
                <th className="w-[12%] py-2 pr-2 font-medium">Side</th>
                <th className="w-[19%] py-2 pr-2 text-right font-medium">Size</th>
                <th className="w-[20%] py-2 pr-2 text-right font-medium">Entry</th>
                <th className="w-[23%] py-2 text-right font-medium">PnL</th>
              </tr>
            </thead>
            <tbody>
              {positions.map((p) => (
                <tr key={p.address} className="border-b border-border-subtle/60 text-slate-300 last:border-b-0">
                  <td className="py-2 pr-2">
                    <div className="flex items-center gap-1.5 overflow-hidden">
                      <span className="truncate font-mono text-xs" title={p.address}>
                        {shortAddress(p.address)}
                      </span>
                      <CopyButton value={p.address} />
                    </div>
                  </td>
                  <td className="py-2 pr-2">
                    <Badge variant={p.side === 'LONG' ? 'long' : 'short'}>{p.side}</Badge>
                  </td>
                  <td className="truncate py-2 pr-2 text-right font-mono" title={p.size ?? undefined}>
                    {p.size}
                  </td>
                  <td className="truncate py-2 pr-2 text-right font-mono" title={p.entryPrice ?? undefined}>
                    {p.entryPrice}
                  </td>
                  <td className="py-2 text-right">
                    <Pnl value={p.unrealizedPnl} size="sm" compact />
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
