export default function Card({ title, icon, children, className = '' }) {
  return (
    <div
      className={`rounded-2xl border border-edge bg-surface p-5 shadow-sm ${className}`}
    >
      {title && (
        <div className="mb-3 flex items-center gap-2">
          {icon && <span className="text-lg leading-none">{icon}</span>}
          <h3 className="font-display text-sm font-semibold uppercase tracking-wide text-ink-muted">
            {title}
          </h3>
        </div>
      )}
      {children}
    </div>
  )
}
