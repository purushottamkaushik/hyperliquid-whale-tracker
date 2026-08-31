import { BookOpen } from 'lucide-react'
import { useOrderBookFeed } from '../../hooks/useOrderBookFeed'
import { EmptyState } from '../ui/EmptyState'
import { Spinner } from '../ui/Spinner'
import { StreamStatusBadge } from './StreamStatusBadge'

const LEVELS_SHOWN = 12

/** One side (bids or asks) of the book, deepest-first toward the middle spread. */
function Side({ levels, side }: { levels: { price: string; size: string; orderCount: number }[]; side: 'bid' | 'ask' }) {
  const maxSize = Math.max(...levels.map((l) => Number(l.size)), 1)
  return (
    <div className="flex flex-1 flex-col gap-0.5">
      {levels.slice(0, LEVELS_SHOWN).map((level, i) => {
        const pct = Math.min(100, (Number(level.size) / maxSize) * 100)
        return (
          <div key={`${level.price}-${i}`} className="relative flex items-center justify-between px-2 py-0.5 text-xs">
            <div
              className={`absolute inset-y-0 ${side === 'bid' ? 'right-0 bg-profit/10' : 'left-0 bg-loss/10'}`}
              style={{ width: `${pct}%` }}
            />
            <span className={`relative z-10 font-mono ${side === 'bid' ? 'text-profit' : 'text-loss'}`}>
              {Number(level.price).toLocaleString('en-US', { maximumFractionDigits: 1 })}
            </span>
            <span className="relative z-10 font-mono text-slate-300">{level.size}</span>
          </div>
        )
      })}
    </div>
  )
}

export function OrderBookDepth() {
  const { book, status } = useOrderBookFeed()

  return (
    <div className="flex flex-col gap-3 rounded-xl border border-border-subtle bg-surface-2 p-4">
      <div className="flex items-center justify-between">
        <h3 className="flex items-center gap-2 text-sm font-semibold text-slate-100">
          BTC Order Book
          <StreamStatusBadge status={status} />
        </h3>
      </div>

      {!book ? (
        <div className="flex items-center justify-center py-10 text-slate-500">
          {status === 'connecting' ? <Spinner size={18} /> : <EmptyState icon={BookOpen} title="No order book data yet" />}
        </div>
      ) : (
        <div>
          <div className="mb-1 flex justify-between px-2 text-[11px] uppercase tracking-wide text-slate-500">
            <span>Price (bids)</span>
            <span>Size</span>
          </div>
          <Side levels={book.bids} side="bid" />
          <div className="my-2 border-t border-border-subtle" />
          <div className="mb-1 flex justify-between px-2 text-[11px] uppercase tracking-wide text-slate-500">
            <span>Price (asks)</span>
            <span>Size</span>
          </div>
          <Side levels={book.asks} side="ask" />
        </div>
      )}
    </div>
  )
}
