import { useEffect, useState } from 'react'
import { Clock } from 'lucide-react'

const dateFormatter = new Intl.DateTimeFormat('en-US', {
  weekday: 'short',
  month: 'short',
  day: 'numeric',
})

const timeFormatter = new Intl.DateTimeFormat('en-US', {
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: true,
})

/** Fixed bottom-right readout of the viewer's system clock, ticking every second. */
export function ClockWidget() {
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000)
    return () => clearInterval(id)
  }, [])

  return (
    <div className="pointer-events-none fixed bottom-4 right-4 z-50">
      <div className="pointer-events-auto flex items-center gap-2 rounded-lg border border-border-subtle bg-surface-2/90 px-3 py-2 text-xs text-slate-300 shadow-lg backdrop-blur">
        <Clock size={13} className="text-slate-500" />
        <span className="text-slate-500">{dateFormatter.format(now)}</span>
        <span className="font-mono font-medium tabular-nums text-slate-100">{timeFormatter.format(now)}</span>
      </div>
    </div>
  )
}
