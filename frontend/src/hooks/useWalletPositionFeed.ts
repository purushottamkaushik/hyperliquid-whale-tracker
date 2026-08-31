import { useEffect, useState } from 'react'
import { walletPositionsApi } from '../api/client'
import type { BtcPositionUpdate } from '../api/types'
import type { StreamStatus } from './useBtcTradeFeed'

/**
 * Live BTC positions for every marked Hyperliquid wallet - an initial REST snapshot, then
 * per-wallet updates pushed over SSE as they change. Keyed by address so each new update replaces
 * that wallet's prior entry rather than appending.
 */
export function useWalletPositionFeed() {
  const [positionsByAddress, setPositionsByAddress] = useState<Map<string, BtcPositionUpdate>>(new Map())
  const [status, setStatus] = useState<StreamStatus>('connecting')

  useEffect(() => {
    let cancelled = false

    walletPositionsApi
      .recent()
      .then((initial) => {
        if (cancelled) return
        setPositionsByAddress(new Map(initial.map((p) => [p.address, p])))
      })
      .catch(() => {
        // Initial snapshot failed to load - the live stream below can still populate it.
      })

    const source = new EventSource('/api/wallet-positions/stream')
    source.addEventListener('open', () => {
      if (!cancelled) setStatus('open')
    })
    source.addEventListener('error', () => {
      if (!cancelled) setStatus('reconnecting')
    })
    source.addEventListener('position', (event) => {
      if (cancelled) return
      const update = JSON.parse((event as MessageEvent).data) as BtcPositionUpdate
      setPositionsByAddress((prev) => new Map(prev).set(update.address, update))
    })

    return () => {
      cancelled = true
      source.close()
    }
  }, [])

  const positions = [...positionsByAddress.values()].filter((p) => p.side !== null)

  return { positions, status }
}
