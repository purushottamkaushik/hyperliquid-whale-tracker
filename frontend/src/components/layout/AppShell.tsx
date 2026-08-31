import type { ReactNode } from 'react'
import { Activity, Bitcoin, Layers, Moon, Sun, Waves } from 'lucide-react'
import { useTheme } from '../../hooks/useTheme'
import { ClockWidget } from './ClockWidget'

export type Tab = 'wallets' | 'whales' | 'orders' | 'live'

interface AppShellProps {
  activeTab: Tab
  onTabChange: (tab: Tab) => void
  children: ReactNode
  /**
   * Left-aligns and widens the main content instead of centering it in a narrow column - used by
   * the Wallets tab so its table has real room next to the Coinglass side panel (~480-560px)
   * instead of being squeezed under it.
   */
  wideContent?: boolean
}

const tabs: { id: Tab; label: string; icon: typeof Bitcoin }[] = [
  { id: 'wallets', label: 'Wallets', icon: Bitcoin },
  { id: 'whales', label: 'Whale Tracker', icon: Waves },
  { id: 'orders', label: 'Whale Orders', icon: Layers },
  { id: 'live', label: 'Live Trades', icon: Activity },
]

export function AppShell({ activeTab, onTabChange, children, wideContent = false }: AppShellProps) {
  const [theme, setTheme] = useTheme()

  return (
    <div className="min-h-svh bg-surface-0">
      <header className="border-b border-border-subtle bg-surface-1/80 backdrop-blur">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent/10 text-accent">
              <Bitcoin size={18} strokeWidth={2.2} />
            </div>
            <div>
              <p className="text-sm font-semibold leading-tight text-slate-100">autotradebtc</p>
              <p className="text-xs leading-tight text-slate-500">Wallet &amp; whale tracker</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <nav className="flex gap-1 rounded-lg border border-border-subtle bg-surface-2 p-1">
              {tabs.map(({ id, label, icon: Icon }) => (
                <button
                  key={id}
                  type="button"
                  onClick={() => onTabChange(id)}
                  className={`inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                    activeTab === id
                      ? 'bg-surface-3 text-slate-100'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <Icon size={15} />
                  {label}
                </button>
              ))}
            </nav>

            <button
              type="button"
              onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
              className="flex h-8 w-8 items-center justify-center rounded-lg border border-border-subtle bg-surface-2 text-slate-400 transition-colors hover:text-slate-200"
              title={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}
            >
              {theme === 'dark' ? <Sun size={15} /> : <Moon size={15} />}
            </button>
          </div>
        </div>
      </header>

      <main className={wideContent ? 'max-w-[1600px] px-6 py-8' : 'mx-auto max-w-5xl px-6 py-8'}>{children}</main>

      <ClockWidget />
    </div>
  )
}
