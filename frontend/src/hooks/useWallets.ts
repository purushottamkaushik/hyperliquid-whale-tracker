import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { walletsApi } from '../api/client'
import type { WalletRequest } from '../api/types'

export const walletsKey = ['wallets'] as const
export const walletsOverviewKey = ['wallets', 'overview'] as const

export function useWallets() {
  return useQuery({ queryKey: walletsKey, queryFn: walletsApi.list })
}

/**
 * Wallets combined with their latest persisted BTC snapshot (total/unrealized PnL, open BTC
 * orders, open BTC position). The snapshot is refreshed server-side every 10 minutes (see
 * app.wallet.sync-interval-ms - kept that long to stay under Hyperliquid's rate limit as more
 * wallets get tracked), so this polls on a shorter interval just to pick up that background
 * update - it does not trigger a live Hyperliquid fetch itself.
 */
export function useWalletOverview() {
  return useQuery({
    queryKey: walletsOverviewKey,
    queryFn: walletsApi.overview,
    refetchInterval: 60_000,
  })
}

export function useAddWallet() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: WalletRequest) => walletsApi.add(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: walletsKey })
      queryClient.invalidateQueries({ queryKey: walletsOverviewKey })
    },
  })
}

export function useDeleteWallet() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => walletsApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: walletsKey })
      queryClient.invalidateQueries({ queryKey: walletsOverviewKey })
    },
  })
}

export function useSetMarked() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, marked }: { id: number; marked: boolean }) => walletsApi.setMarked(id, marked),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: walletsKey })
      queryClient.invalidateQueries({ queryKey: walletsOverviewKey })
    },
  })
}

export function useSetActive() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => walletsApi.setActive(id, active),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: walletsKey })
      queryClient.invalidateQueries({ queryKey: walletsOverviewKey })
    },
  })
}

export function useTransactions(id: number | null) {
  return useQuery({
    queryKey: ['wallets', id, 'transactions'],
    queryFn: () => walletsApi.transactions(id as number),
    enabled: id !== null,
  })
}
