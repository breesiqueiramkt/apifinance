import { useAuth } from '../../context/AuthContext'
import { useDarkMode } from '../../hooks/useDarkMode'

function greeting() {
  const h = new Date().getHours()
  if (h < 12) return 'Bom dia'
  if (h < 18) return 'Boa tarde'
  return 'Boa noite'
}

export default function Topbar() {
  const { user, logout } = useAuth()
  const [isDark, setIsDark] = useDarkMode()

  return (
    <header className="flex items-center justify-between border-b border-edge bg-surface px-6 py-4">
      <div>
        <p className="font-display text-lg font-semibold">
          {greeting()}, {user?.name?.split(' ')[0]} 👋
        </p>
        <p className="text-xs text-ink-muted">
          {new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })}
        </p>
      </div>

      <div className="flex items-center gap-2">
        <button
          onClick={() => setIsDark(!isDark)}
          aria-label="Alternar tema"
          className="rounded-xl border border-edge p-2.5 text-sm hover:bg-paper"
        >
          {isDark ? '☀️' : '🌙'}
        </button>
        <button
          onClick={logout}
          className="rounded-xl border border-edge px-3 py-2.5 text-sm font-medium text-ink-muted hover:bg-paper"
        >
          Sair
        </button>
      </div>
    </header>
  )
}
