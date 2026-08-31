import { ExternalLink, Waves } from 'lucide-react'
import type { WhaleWalletResponse } from '../../api/types'
import { Pnl } from '../ui/Pnl'
import { formatDateTime, formatUsd } from '../../lib/format'
import { CopyButton } from '../ui/CopyButton'

interface WhaleRowProps {
  whale: WhaleWalletResponse
}

/**
 * One tracked whale, rendered as a table row. Profit is the last (rightmost) column,
 * per the "profit on the right hand side" requirement.
 */
export function WhaleRow({ whale }: WhaleRowProps) {
  const explorerUrl = `https://app.hyperliquid.xyz/explorer/address/${whale.address}`

  return (
    <tr className="border-b border-border-subtle/60 transition-colors hover:bg-surface-2/60">
      <td className="px-4 py-3">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-violet-500/10 text-violet-300">
            <Waves size={15} strokeWidth={2} />
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-1.5">
              <span className="break-all font-mono text-sm text-slate-100">{whale.address}</span>
              <CopyButton value={whale.address} />
              <a
                href={explorerUrl}
                target="_blank"
                rel="noreferrer"
                className="shrink-0 text-slate-500 transition-colors hover:text-accent"
                title="View on Hyperliquid explorer"
              >
                <ExternalLink size={12} />
              </a>
            </div>
            <p className="truncate text-xs text-slate-500">{whale.label ?? 'Untracked label'}</p>
          </div>
        </div>
      </td>
      <td className="px-4 py-3 text-sm text-slate-400">{formatDateTime(whale.createdAt)}</td>
      <td className="px-4 py-3 text-right font-mono text-sm text-slate-200">
        {formatUsd(whale.accountValue)}
      </td>
      <td className="px-4 py-3 text-right">
        <Pnl value={whale.monthPnl} size="sm" />
      </td>
      <td className="px-4 py-3 text-right">
        <Pnl value={whale.allTimePnl} size="lg" showIcon />
      </td>
    </tr>
  )
}
