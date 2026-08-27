import { NavLink } from 'react-router-dom'

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', icon: '🏠', end: true },
  { to: '/transactions', label: 'Receitas e despesas', icon: '💸' },
  { to: '/accounts', label: 'Contas', icon: '🏦' },
  { to: '/bills', label: 'Contas futuras', icon: '📅' },
  { to: '/credit-cards', label: 'Cartão de crédito', icon: '💳' },
  { to: '/investments', label: 'Investimentos', icon: '📈' },
  { to: '/goals', label: 'Minhas metas', icon: '🎯' },
  { to: '/debts', label: 'Dívidas', icon: '📄' },
  { to: '/calculators', label: 'Calculadoras', icon: '🧮' },
  { to: '/reports', label: 'Relatórios', icon: '📊' }
]

export default function Sidebar() {
  return (
    <aside className="hidden w-60 shrink-0 flex-col border-r border-edge bg-surface px-4 py-6 md:flex">
      <div className="mb-8 px-2">
        <span className="font-display text-xl font-bold tracking-tight text-ledger">Extrato</span>
        <p className="text-xs text-ink-muted">controle financeiro pessoal</p>
      </div>

      <nav className="flex flex-1 flex-col gap-1 overflow-y-auto">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) =>
              `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${
                isActive
                  ? 'bg-ledger text-white'
                  : 'text-ink-muted hover:bg-paper hover:text-ink'
              }`
            }
          >
            <span>{item.icon}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="mt-3 shrink-0 rounded-xl border border-dashed border-edge px-3 py-3 text-xs text-ink-muted">
        Extrato — controle financeiro pessoal completo.
      </div>
    </aside>
  )
}
