import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import Button from '../components/ui/Button'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(email, password)
      navigate('/')
    } catch (err) {
      setError(err.response?.data?.message || 'Não foi possível entrar. Verifique seus dados.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-paper px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <span className="font-display text-2xl font-bold text-ledger">Extrato</span>
          <p className="mt-1 text-sm text-ink-muted">Entre para ver sua situação financeira</p>
        </div>

        <form onSubmit={handleSubmit} className="ledger-surface rounded-2xl border border-edge p-6 shadow-sm">
          <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
            E-mail
          </label>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="mb-4 w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
            placeholder="voce@email.com"
          />

          <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
            Senha
          </label>
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="mb-2 w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
            placeholder="••••••••"
          />

          {error && <p className="mb-3 text-sm text-danger">{error}</p>}

          <Button type="submit" disabled={loading} className="mt-3 w-full">
            {loading ? 'Entrando...' : 'Entrar'}
          </Button>
        </form>
      </div>
    </div>
  )
}
