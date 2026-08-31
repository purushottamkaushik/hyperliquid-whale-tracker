import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { whaleOrdersApi, type WhaleOrdersFilter } from '../api/client'

const POLL_INTERVAL_MS = 15_000

export function useWhaleOrders(filter: WhaleOrdersFilter) {
  return useQuery({
    queryKey: ['whale-orders', filter],
    queryFn: () => whaleOrdersApi.list(filter),
    refetchInterval: POLL_INTERVAL_MS,
  })
}

export function useWhaleOrderStatus() {
  return useQuery({
    queryKey: ['whale-orders', 'status'],
    queryFn: whaleOrdersApi.status,
    refetchInterval: POLL_INTERVAL_MS,
  })
}

/** Runs a scan on demand (e.g. on first page load) instead of waiting for the next scheduled tick. */
export function useScanWhaleOrdersNow() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: whaleOrdersApi.scanNow,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['whale-orders'] })
    },
  })
}
