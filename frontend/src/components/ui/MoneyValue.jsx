import { useEffect, useRef, useState } from 'react'

const formatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL'
})

export default function MoneyValue({ value, className = '', duration = 700 }) {
  const [display, setDisplay] = useState(0)
  const startRef = useRef(null)
  const fromRef = useRef(0)

  useEffect(() => {
    const target = Number(value) || 0
    fromRef.current = display
    startRef.current = null

    let frameId
    const step = (timestamp) => {
      if (startRef.current === null) startRef.current = timestamp
      const progress = Math.min((timestamp - startRef.current) / duration, 1)
      const eased = 1 - Math.pow(1 - progress, 3)
      setDisplay(fromRef.current + (target - fromRef.current) * eased)
      if (progress < 1) frameId = requestAnimationFrame(step)
    }
    frameId = requestAnimationFrame(step)
    return () => cancelAnimationFrame(frameId)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value])

  const isNegative = display < 0

  return (
    <span className={`money ${isNegative ? 'text-danger' : ''} ${className}`}>
      {formatter.format(display)}
    </span>
  )
}
