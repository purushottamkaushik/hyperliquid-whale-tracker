import { useState } from 'react'
import { Check, Copy } from 'lucide-react'

export function CopyButton({ value }: { value: string }) {
  const [copied, setCopied] = useState(false)

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(value)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      // Clipboard API unavailable (e.g. insecure context) - silently ignore.
    }
  }

  return (
    <button
      type="button"
      onClick={handleCopy}
      className="text-slate-500 transition-colors hover:text-accent"
      title="Copy address"
    >
      {copied ? <Check size={13} className="text-profit" /> : <Copy size={13} />}
    </button>
  )
}
