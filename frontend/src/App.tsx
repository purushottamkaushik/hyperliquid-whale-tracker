import { useEffect, useState } from 'react'
import { AppShell, type Tab } from './components/layout/AppShell'
import { WalletsPage } from './components/wallets/WalletsPage'
import { WhaleTrackerPage } from './components/whales/WhaleTrackerPage'
import { WhaleOrdersPage } from './components/whales/WhaleOrdersPage'
import { LiveTradesPage } from './components/livetrades/LiveTradesPage'

const TAB_STORAGE_KEY = 'autotradebtc:tab'

function getInitialTab(): Tab {
  try {
    const stored = localStorage.getItem(TAB_STORAGE_KEY)
    if (stored === 'wallets' || stored === 'whales' || stored === 'orders' || stored === 'live') return stored
  } catch {
    // localStorage unavailable - fall back to the default tab.
  }
  return 'whales'
}

export default function App() {
  const [tab, setTab] = useState<Tab>(getInitialTab)

  useEffect(() => {
    try {
      localStorage.setItem(TAB_STORAGE_KEY, tab)
    } catch {
      // localStorage unavailable - tab just won't persist across reloads.
    }
  }, [tab])

  return (
    <AppShell activeTab={tab} onTabChange={setTab} wideContent={tab === 'wallets'}>
      {tab === 'wallets' && <WalletsPage />}
      {tab === 'whales' && <WhaleTrackerPage />}
      {tab === 'orders' && <WhaleOrdersPage />}
      {tab === 'live' && <LiveTradesPage />}
    </AppShell>
  )
}
