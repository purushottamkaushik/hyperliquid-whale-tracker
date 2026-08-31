import type { WalletResponse } from '../../api/types'
import { useTransactions } from '../../hooks/useWallets'
import { formatDateTime, satsToBtc } from '../../lib/format'
import { EmptyState } from '../ui/EmptyState'
import { Spinner } from '../ui/Spinner'
import { Activity } from 'lucide-react'

interface ActivityPanelProps {
  wallet: WalletResponse
}

/** BTC on-chain transaction history for a tracked Bitcoin wallet. */
export function ActivityPanel({ wallet }: ActivityPanelProps) {
  const { data, isLoading } = useTransactions(wallet.id)
  const txs = data ?? []

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-14 text-slate-500">
        <Spinner size={20} />
      </div>
    )
  }
  if (txs.length === 0) {
    return <EmptyState icon={Activity} title="No transactions found" />
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[640px] text-sm">
        <thead>
          <tr className="border-b border-border-subtle text-left text-xs uppercase tracking-wide text-slate-500">
            <th className="py-2 pr-4 font-medium">Date</th>
            <th className="py-2 pr-4 font-medium">Direction</th>
            <th className="py-2 pr-4 font-medium">Amount (BTC)</th>
            <th className="py-2 pr-4 font-medium">Fee (BTC)</th>
            <th className="py-2 pr-4 font-medium">Status</th>
            <th className="py-2 font-medium">Txid</th>
          </tr>
        </thead>
        <tbody>
          {txs.map((tx) => (
            <tr key={tx.txid} className="border-b border-border-subtle/60 text-slate-300">
              <td className="py-2 pr-4">{tx.blockTime ? formatDateTime(tx.blockTime) : 'Unconfirmed'}</td>
              <td className="py-2 pr-4">{tx.direction}</td>
              <td className="py-2 pr-4 font-mono">{satsToBtc(tx.netAmountSats)}</td>
              <td className="py-2 pr-4 font-mono">{satsToBtc(tx.feeSats)}</td>
              <td className="py-2 pr-4">{tx.confirmed ? 'Confirmed' : 'Pending'}</td>
              <td className="py-2 font-mono text-xs text-slate-500">{tx.txid}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
