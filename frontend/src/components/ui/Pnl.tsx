import { ArrowDownRight, ArrowUpRight } from 'lucide-react'
import { formatSignedBtc, formatSignedUsd, formatSignedUsdCompact, isPositive } from '../../lib/format'

interface PnlProps {
  value: string | number | null | undefined
  size?: 'sm' | 'md' | 'lg'
  showIcon?: boolean
  /** Abbreviate large USD figures as e.g. "$1.2M" instead of the full "$1,234,567.89". Ignored for the "btc" unit. */
  compact?: boolean
  /** Denomination to render the figure in. Defaults to USD. */
  unit?: 'usd' | 'btc'
}

const sizeClasses: Record<NonNullable<PnlProps['size']>, string> = {
  sm: 'text-xs',
  md: 'text-sm',
  lg: 'text-lg',
}

function format(value: PnlProps['value'], unit: NonNullable<PnlProps['unit']>, compact: boolean): string {
  if (unit === 'btc') return formatSignedBtc(value)
  return compact ? formatSignedUsdCompact(value) : formatSignedUsd(value)
}

/** Renders a signed PnL/profit figure, colored green (profit) or red (loss). */
export function Pnl({ value, size = 'md', showIcon = false, compact = false, unit = 'usd' }: PnlProps) {
  const positive = isPositive(value)
  const colorClass =
    positive === null ? 'text-slate-500' : positive ? 'text-profit' : 'text-loss'
  const Icon = positive ? ArrowUpRight : ArrowDownRight

  return (
    <span
      className={`inline-flex items-center gap-1 font-mono font-semibold tabular-nums ${colorClass} ${sizeClasses[size]}`}
      title={unit === 'usd' && compact ? formatSignedUsd(value) : undefined}
    >
      {showIcon && positive !== null && <Icon size={14} strokeWidth={2.5} />}
      {format(value, unit, compact)}
    </span>
  )
}
