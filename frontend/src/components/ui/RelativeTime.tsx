import { useEffect, useState } from 'react'
import { formatRelativeTime } from '../../lib/format'

/** Renders a relative timestamp ("12s ago") that keeps ticking on its own between data refetches. */
export function RelativeTime({ value }: { value: string | null | undefined }) {
  const [, forceTick] = useState(0)

  useEffect(() => {
    const id = setInterval(() => forceTick((n) => n + 1), 1000)
    return () => clearInterval(id)
  }, [])

  return <>{formatRelativeTime(value)}</>
}
