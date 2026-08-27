const STYLES = {
  PAID: { label: 'Pago', color: 'var(--primary)' },
  PENDING: { label: 'Pendente', color: 'var(--gold)' },
  LATE: { label: 'Atrasado', color: 'var(--danger)' },
  SCHEDULED: { label: 'Agendado', color: 'var(--ink-muted)' }
}

export default function StatusStamp({ status }) {
  const s = STYLES[status] || STYLES.PENDING
  return (
    <span className="stamp" style={{ color: s.color }}>
      {s.label}
    </span>
  )
}
