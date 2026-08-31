import { useState, type FormEvent } from 'react'
import { Plus } from 'lucide-react'
import { useAddWallet } from '../../hooks/useWallets'
import { ApiError } from '../../api/client'
import { Spinner } from '../ui/Spinner'

export function AddWalletForm() {
  const [address, setAddress] = useState('')
  const [label, setLabel] = useState('')
  const [error, setError] = useState<string | null>(null)
  const addWallet = useAddWallet()

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      await addWallet.mutateAsync({ address: address.trim(), label: label.trim() || undefined })
      setAddress('')
      setLabel('')
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Failed to add wallet')
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-2 sm:flex-row">
      <input
        type="text"
        value={address}
        onChange={(e) => setAddress(e.target.value)}
        placeholder="Bitcoin or Hyperliquid address"
        required
        className="flex-1 rounded-lg border border-border-subtle bg-surface-2 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:border-accent focus:outline-none"
      />
      <input
        type="text"
        value={label}
        onChange={(e) => setLabel(e.target.value)}
        placeholder="Label (optional)"
        className="rounded-lg border border-border-subtle bg-surface-2 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:border-accent focus:outline-none sm:w-48"
      />
      <button
        type="submit"
        disabled={addWallet.isPending}
        className="inline-flex items-center justify-center gap-1.5 rounded-lg bg-accent px-4 py-2 text-sm font-medium text-surface-0 transition-opacity hover:opacity-90 disabled:opacity-60"
      >
        {addWallet.isPending ? <Spinner size={14} /> : <Plus size={14} />}
        Save wallet
      </button>
      {error && <p className="text-xs text-loss sm:self-center">{error}</p>}
    </form>
  )
}
