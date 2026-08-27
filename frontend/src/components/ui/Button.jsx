export default function Button({ children, variant = 'primary', className = '', ...props }) {
  const base = 'inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition disabled:opacity-50 disabled:cursor-not-allowed'
  const variants = {
    primary: 'bg-ledger text-white hover:bg-ledger-dark',
    ghost: 'bg-transparent text-ink border border-edge hover:bg-paper',
    danger: 'bg-transparent text-danger border border-danger/40 hover:bg-danger/10'
  }
  return (
    <button className={`${base} ${variants[variant]} ${className}`} {...props}>
      {children}
    </button>
  )
}
