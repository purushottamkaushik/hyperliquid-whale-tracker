export type Chain = 'BTC' | 'HYPERLIQUID'
export type WalletSource = 'MANUAL' | 'AUTO_WHALE'

export interface WalletResponse {
  id: number
  address: string
  label: string | null
  chain: Chain
  source: WalletSource
  createdAt: string
  marked: boolean
  active: boolean
}

export interface WhaleWalletResponse {
  id: number
  address: string
  label: string | null
  createdAt: string
  accountValue: string | null
  monthPnl: string | null
  allTimePnl: string | null
}

export interface WhaleScanResult {
  scannedAt: string
  totalLeaderboardEntries: number
  whaleCandidates: number
  newlyDiscovered: number
}

/** Result of the "Scan & mark" button on the wallets screen. */
export interface WalletAutoMarkResult {
  walletsChecked: number
  newlyMarked: number
  skippedNoLeaderboardData: number
  minTotalPnl: string
}

export interface WhaleScanStatus {
  lastScanAt: string | null
  lastCandidateCount: number
  lastDiscoveredCount: number
  minAccountValue: string
  maxLeverage: string
}

export type PositionSide = 'LONG' | 'SHORT'

export interface WhalePosition {
  walletId: number
  address: string
  label: string | null
  side: PositionSide
  size: string
  notionalUsd: string | null
  entryPrice: string | null
  unrealizedPnl: string | null
  leverage: string | null
}

export interface WhalePositionSummary {
  longCount: number
  shortCount: number
  longBtcSize: string
  shortBtcSize: string
  longNotionalUsd: string
  shortNotionalUsd: string
  majoritySide: PositionSide | null
  majorityBtcSharePct: string | null
}

export interface WhalePositionsDashboard {
  summary: WhalePositionSummary
  positions: WhalePosition[]
}

export interface BtcTransaction {
  txid: string
  confirmed: boolean
  blockHeight: number | null
  blockTime: string | null
  feeSats: number
  netAmountSats: number
  direction: string
}

export interface HyperliquidFill {
  txid: string
  time: string
  coin: string
  side: string
  direction: string
  price: string
  size: string
  fee: string
  feeToken: string | null
  closedPnl: string
}

export interface HyperliquidPosition {
  coin: string
  side: PositionSide
  size: string
  entryPrice: string | null
  positionValue: string | null
  unrealizedPnl: string | null
  leverage: string | null
}

export interface HyperliquidOpenOrder {
  orderId: number
  time: string
  coin: string
  side: string
  orderType: string
  price: string
  size: string
  originalSize: string
  reduceOnly: boolean
}

export interface WalletRequest {
  address: string
  label?: string
}

/**
 * One executed trade on Hyperliquid's BTC perpetual market - market-wide (every trader, not just
 * tracked wallets). `side` is the taker's side; `makerAddress` is the resting order it matched.
 */
export interface BtcTradeEvent {
  tradeId: number
  time: string
  side: 'BUY' | 'SELL'
  price: string
  size: string
  notionalUsd: string
  takerAddress: string
  makerAddress: string
}

/** One price level in the live BTC order book - aggregated size across every resting order there. */
export interface OrderBookLevel {
  price: string
  size: string
  orderCount: number
}

/** Full BTC order book depth snapshot - each push replaces the previous one, not a delta. */
export interface OrderBookSnapshot {
  coin: string
  time: string
  bids: OrderBookLevel[]
  asks: OrderBookLevel[]
}

/**
 * A marked wallet's current BTC position, pushed live over WebSocket. Fields other than
 * `address`/`time` are null when the wallet has no open BTC position right now.
 */
export interface BtcPositionUpdate {
  address: string
  time: string
  side: 'LONG' | 'SHORT' | null
  size: string | null
  entryPrice: string | null
  positionValue: string | null
  unrealizedPnl: string | null
  leverage: string | null
}

/**
 * A tracked wallet combined with its latest persisted BTC-only snapshot (refreshed server-side
 * every few minutes). For BTC-chain wallets, pnl fields are null and the lists are empty.
 */
export interface WalletOverviewResponse {
  walletId: number
  address: string
  label: string | null
  chain: Chain
  source: WalletSource
  createdAt: string
  marked: boolean
  active: boolean
  totalPnl: string | null
  unrealizedPnl: string | null
  totalPnlBtc: string | null
  unrealizedPnlBtc: string | null
  lastSyncedAt: string | null
  openOrders: HyperliquidOpenOrder[]
  positions: HyperliquidPosition[]
}

export type WhaleOrderExchange = 'BINANCE' | 'OKX' | 'BYBIT' | 'HYPERLIQUID'
export type WhaleOrderStatus = 'ACTIVE' | 'REMOVED'

/**
 * A large resting BTC limit order tracked across exchange order books over time (Coinglass
 * "large orderbook statistics" style) - a price level whose notional value crossed the
 * configured whale threshold. `status` flips to REMOVED once the level is no longer resting.
 */
export interface WhaleLimitOrder {
  id: number
  exchange: WhaleOrderExchange
  symbol: string
  side: 'BUY' | 'SELL'
  price: string
  size: string
  notionalUsd: string
  status: WhaleOrderStatus
  firstSeenAt: string
  lastSeenAt: string
  removedAt: string | null
}

export interface WhaleOrderScanStatus {
  lastScanAt: string | null
  minNotionalUsd: string
  activeCount: number
  activeCountByExchange: Record<string, number>
  exchangeErrors: Record<string, string>
}

export interface ApiErrorBody {
  message?: string
  detail?: string
}
