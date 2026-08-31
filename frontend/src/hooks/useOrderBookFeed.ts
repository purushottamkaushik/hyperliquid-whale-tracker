import { useEffect, useState } from 'react'
import { orderBookApi } from '../api/client'
import type { OrderBookSnapshot } from '../api/types'
import type { StreamStatus } from './useBtcTradeFeed'

/** Live BTC order book depth - an initial REST snapshot, then full-replacement pushes over SSE. */
export function useOrderBookFeed() {
  const [book, setBook] = useState<OrderBookSnapshot | null>(null)
  const [status, setStatus] = useState<StreamStatus>('connecting')

  useEffect(() => {
    let cancelled = false

    orderBookApi
      .snapshot()
      .then((initial) => {
        if (!cancelled && initial) setBook(initial)
      })
      .catch(() => {
        // Initial snapshot failed to load - the live stream below can still populate it.
      })

    const source = new EventSource('/api/btc-orderbook/stream')
    source.addEventListener('open', () => {
      if (!cancelled) setStatus('open')
    })
    source.addEventListener('error', () => {
      if (!cancelled) setStatus('reconnecting')
    })
    source.addEventListener('book', (event) => {
      if (cancelled) return
      setBook(JSON.parse((event as MessageEvent).data) as OrderBookSnapshot)
    })

    return () => {
      cancelled = true
      source.close()
    }
  }, [])

  return { book, status }
}
