import { useState } from 'react'
import { ExternalLink, X } from 'lucide-react'
import { Spinner } from '../ui/Spinner'
import { shortAddress } from '../../lib/format'

interface CoinglassPanelProps {
  address: string | null
  onClose: () => void
}

function coinglassUrl(address: string): string {
  return `https://www.coinglass.com/hyperliquid/${address}`
}

/**
 * Loading state lives in a subcomponent keyed by address (see below) rather than here, so
 * switching wallets while the panel is open shows the spinner again instead of a stale iframe.
 */
function IframeArea({ address }: { address: string }) {
  const [loaded, setLoaded] = useState(false)

  return (
    <div className="relative flex-1">
      {!loaded && (
        <div className="absolute inset-0 flex items-center justify-center text-slate-500">
          <Spinner size={20} />
        </div>
      )}
      <iframe
        src={coinglassUrl(address)}
        onLoad={() => setLoaded(true)}
        className="h-full w-full border-0"
        title="Coinglass"
      />
    </div>
  )
}

/**
 * Embeds a wallet's Coinglass Hyperliquid page in a right-hand panel instead of navigating away.
 * Coinglass sends no framing-restriction headers (X-Frame-Options / CSP frame-ancestors) and
 * doesn't frame-bust, so this renders for a real browser - confirmed by loading it in an actual
 * (non-automated) headless Chrome session. Since that's still external, uncontrolled behavior
 * that could change, "Open in new tab" stays visible as a fallback.
 */
export function CoinglassPanel({ address, onClose }: CoinglassPanelProps) {
  if (!address) return null

  return (
    <div className="fixed inset-y-0 right-0 z-40 flex w-full flex-col border-l border-border-subtle bg-surface-1 shadow-2xl sm:w-[480px] lg:w-[560px]">
      <div className="flex items-center justify-between gap-2 border-b border-border-subtle px-4 py-3">
        <div className="min-w-0">
          <p className="text-xs uppercase tracking-wide text-slate-500">Coinglass</p>
          <p className="truncate font-mono text-sm text-slate-200" title={address}>
            {shortAddress(address)}
          </p>
        </div>
        <div className="flex shrink-0 items-center gap-1.5">
          <a
            href={coinglassUrl(address)}
            target="_blank"
            rel="noopener"
            className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 transition-colors hover:text-slate-200"
            title="Open in new tab"
          >
            <ExternalLink size={14} />
          </a>
          <button
            type="button"
            onClick={onClose}
            className="flex h-7 w-7 items-center justify-center rounded-lg text-slate-400 transition-colors hover:text-slate-200"
            title="Close"
          >
            <X size={16} />
          </button>
        </div>
      </div>
      <IframeArea key={address} address={address} />
    </div>
  )
}
