import { useEffect, useRef, useState } from 'react'
import { btcTradesApi } from '../api/client'
import type { BtcTradeEvent } from '../api/types'

const MAX_TRADES = 300

export type StreamStatus = 'connecting' | 'open' | 'reconnecting'

/**
 * Live feed of every executed BTC perpetual trade on Hyperliquid (market-wide, not scoped to
 * tracked wallets) - an initial REST snapshot followed by a Server-Sent Events stream of new
 * trades as they happen. Trades are deduplicated by tradeId since a reconnect can re-send
 * recently buffered ones.
 */
export function useBtcTradeFeed() {
  const [trades, setTrades] = useState<BtcTradeEvent[]>([])
  const [status, setStatus] = useState<StreamStatus>('connecting')
  const seenIds = useRef(new Set<number>())

  useEffect(() => {
    let cancelled = false

    btcTradesApi
      .recent()
      .then((initial) => {
        if (cancelled) return
        initial.forEach((t) => seenIds.current.add(t.tradeId))
        setTrades(initial)
      })
      .catch(() => {
        // Initial snapshot failed to load - the live stream below can still populate the feed.
      })

    const source = new EventSource('/api/btc-trades/stream')

    source.addEventListener('open', () => {
      if (!cancelled) setStatus('open')
    })
    source.addEventListener('error', () => {
      if (!cancelled) setStatus('reconnecting')
    })
    source.addEventListener('trades', (event) => {
      if (cancelled) return
      const incoming = JSON.parse((event as MessageEvent).data) as BtcTradeEvent[]
      const fresh = incoming.filter((t) => !seenIds.current.has(t.tradeId))
      if (fresh.length === 0) return
      fresh.forEach((t) => seenIds.current.add(t.tradeId))
      // incoming arrives oldest-first; reverse so the running list stays newest-first.
      setTrades((prev) => [...fresh.slice().reverse(), ...prev].slice(0, MAX_TRADES))
    })

    return () => {
      cancelled = true
      source.close()
    }
  }, [])

  return { trades, status }
}
