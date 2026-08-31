import type {
  ApiErrorBody,
  BtcPositionUpdate,
  BtcTradeEvent,
  BtcTransaction,
  HyperliquidOpenOrder,
  HyperliquidPosition,
  OrderBookSnapshot,
  WalletAutoMarkResult,
  WalletOverviewResponse,
  WalletRequest,
  WalletResponse,
  WhaleLimitOrder,
  WhaleOrderScanStatus,
  WhalePositionsDashboard,
  WhaleScanResult,
  WhaleScanStatus,
  WhaleWalletResponse,
} from './types'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, {
    headers: init?.body ? { 'Content-Type': 'application/json' } : undefined,
    ...init,
  })
  if (!res.ok) {
    const body = (await res.json().catch(() => ({}))) as ApiErrorBody
    throw new ApiError(res.status, body.message ?? body.detail ?? `Request failed (${res.status})`)
  }
  if (res.status === 204) {
    return undefined as T
  }
  return res.json() as Promise<T>
}

export const walletsApi = {
  list: () => request<WalletResponse[]>('/api/wallets'),
  overview: () => request<WalletOverviewResponse[]>('/api/wallets/overview'),
  add: (body: WalletRequest) =>
    request<WalletResponse>('/api/wallets', { method: 'POST', body: JSON.stringify(body) }),
  remove: (id: number) => request<void>(`/api/wallets/${id}`, { method: 'DELETE' }),
  setMarked: (id: number, marked: boolean) =>
    request<WalletResponse>(`/api/wallets/${id}/marked`, { method: 'PATCH', body: JSON.stringify({ marked }) }),
  setActive: (id: number, active: boolean) =>
    request<WalletResponse>(`/api/wallets/${id}/active`, { method: 'PATCH', body: JSON.stringify({ active }) }),
  transactions: (id: number) => request<BtcTransaction[]>(`/api/wallets/${id}/transactions`),
  openOrders: (id: number) => request<HyperliquidOpenOrder[]>(`/api/wallets/${id}/open-orders`),
  positions: (id: number) => request<HyperliquidPosition[]>(`/api/wallets/${id}/positions`),
}

export const whalesApi = {
  list: () => request<WhaleWalletResponse[]>('/api/whales'),
  status: () => request<WhaleScanStatus>('/api/whales/status'),
  scanNow: () => request<WhaleScanResult>('/api/whales/scan', { method: 'POST' }),
  positions: () => request<WhalePositionsDashboard>('/api/whales/positions'),
  markHighPnlWallets: () => request<WalletAutoMarkResult>('/api/whales/mark-high-pnl', { method: 'POST' }),
}

export const btcTradesApi = {
  // The live stream itself is opened directly via EventSource in useBtcTradeFeed, not through
  // this fetch-based `request` helper.
  recent: () => request<BtcTradeEvent[]>('/api/btc-trades/recent'),
}

export const orderBookApi = {
  snapshot: () => request<OrderBookSnapshot | null>('/api/btc-orderbook/snapshot'),
}

export const walletPositionsApi = {
  recent: () => request<BtcPositionUpdate[]>('/api/wallet-positions/recent'),
}

export interface WhaleOrdersFilter {
  exchange?: string
  side?: string
  includeRemoved?: boolean
}

function toQueryString(params: WhaleOrdersFilter): string {
  const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== '')
  if (entries.length === 0) return ''
  return `?${entries.map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`).join('&')}`
}

export const whaleOrdersApi = {
  list: (filter: WhaleOrdersFilter = {}) =>
    request<WhaleLimitOrder[]>(`/api/whale-orders${toQueryString(filter)}`),
  status: () => request<WhaleOrderScanStatus>('/api/whale-orders/status'),
  scanNow: () => request<WhaleOrderScanStatus>('/api/whale-orders/scan', { method: 'POST' }),
}
