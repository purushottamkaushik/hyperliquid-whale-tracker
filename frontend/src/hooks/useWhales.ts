import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { whalesApi } from '../api/client'

const POLL_INTERVAL_MS = 30_000

export function useWhales() {
  return useQuery({
    queryKey: ['whales'],
    queryFn: whalesApi.list,
    refetchInterval: POLL_INTERVAL_MS,
  })
}

export function useWhaleStatus() {
  return useQuery({
    queryKey: ['whales', 'status'],
    queryFn: whalesApi.status,
    refetchInterval: POLL_INTERVAL_MS,
  })
}

export function useWhalePositions() {
  return useQuery({
    queryKey: ['whales', 'positions'],
    queryFn: whalesApi.positions,
    refetchInterval: POLL_INTERVAL_MS,
  })
}

export function useScanNow() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: whalesApi.scanNow,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['whales'] })
      queryClient.invalidateQueries({ queryKey: ['wallets'] })
    },
  })
}

/** "Scan & mark" button on the wallets screen - marks high-PnL Hyperliquid wallets automatically. */
export function useMarkHighPnlWallets() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: whalesApi.markHighPnlWallets,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['wallets'] })
    },
  })
}
