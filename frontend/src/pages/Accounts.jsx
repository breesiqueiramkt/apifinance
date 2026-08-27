import { useEffect, useState } from 'react'
import api from '../api/client'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import MoneyValue from '../components/ui/MoneyValue'

const ACCOUNT_TYPES = [
  { value: 'CHECKING', label: 'Conta corrente' },
  { value: 'SAVINGS', label: 'Poupança' },
  { value: 'WALLET', label: 'Carteira' },
  { value: 'DIGITAL', label: 'Conta digital' },
  { value: 'INVESTMENT', label: 'Investimentos' },
  { value: 'OTHER', label: 'Outros' }
]

const EMPTY_FORM = { name: '', bank: '', type: 'CHECKING', balance: '', color: '#1F6F54' }

export default function Accounts() {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [error, setError] = useState('')
  const [confirmDeleteId, setConfirmDeleteId] = useState(null)

  function loadAccounts() {
    setLoading(true)
    api
      .get('/accounts')
      .then((res) => setAccounts(res.data))
      .finally(() => setLoading(false))
  }

  useEffect(loadAccounts, [])

  function openCreate() {
    setEditingId(null)
    setForm(EMPTY_FORM)
    setError('')
    setModalOpen(true)
  }

  function openEdit(account) {
    setEditingId(account.id)
    setForm({
      name: account.name,
      bank: account.bank || '',
      type: account.type,
      balance: account.balance,
      color: account.color || '#1F6F54'
    })
    setError('')
    setModalOpen(true)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    const payload = { ...form, balance: Number(form.balance) }
    try {
      if (editingId) {
        await api.put(`/accounts/${editingId}`, payload)
      } else {
        await api.post('/accounts', payload)
      }
      setModalOpen(false)
      loadAccounts()
    } catch (err) {
      setError(err.response?.data?.message || 'Não foi possível salvar a conta.')
    }
  }

  async function handleDelete(id) {
    await api.delete(`/accounts/${id}`)
    setConfirmDeleteId(null)
    loadAccounts()
  }

  const total = accounts.reduce((sum, a) => sum + Number(a.balance), 0)

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-xl font-semibold">Contas bancárias</h1>
          <p className="text-sm text-ink-muted">
            Total disponível: <MoneyValue value={total} className="font-semibold" />
          </p>
        </div>
        <Button onClick={openCreate}>+ Nova conta</Button>
      </div>

      {loading ? (
        <p className="text-sm text-ink-muted">Carregando contas...</p>
      ) : accounts.length === 0 ? (
        <Card>
          <p className="text-sm text-ink-muted">
            Você ainda não cadastrou nenhuma conta. Crie a primeira para começar a lançar receitas e despesas.
          </p>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {accounts.map((account) => (
            <Card key={account.id}>
              <div className="mb-3 flex items-start justify-between">
                <div>
                  <p className="font-display font-semibold">{account.name}</p>
                  <p className="text-xs text-ink-muted">
                    {account.bank || '—'} ·{' '}
                    {ACCOUNT_TYPES.find((t) => t.value === account.type)?.label}
                  </p>
                </div>
                <span
                  className="h-3 w-3 rounded-full"
                  style={{ backgroundColor: account.color || '#1F6F54' }}
                />
              </div>

              <MoneyValue value={account.balance} className="text-xl font-semibold" duration={400} />

              <div className="mt-4 flex gap-2">
                <Button variant="ghost" className="flex-1" onClick={() => openEdit(account)}>
                  Editar
                </Button>
                {confirmDeleteId === account.id ? (
                  <Button variant="danger" className="flex-1" onClick={() => handleDelete(account.id)}>
                    Confirmar?
                  </Button>
                ) : (
                  <Button
                    variant="danger"
                    className="flex-1"
                    onClick={() => setConfirmDeleteId(account.id)}
                  >
                    Excluir
                  </Button>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={modalOpen} title={editingId ? 'Editar conta' : 'Nova conta'} onClose={() => setModalOpen(false)}>
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
              Nome
            </label>
            <input
              required
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              placeholder="Ex: Nubank"
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
              Banco
            </label>
            <input
              value={form.bank}
              onChange={(e) => setForm({ ...form, bank: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              placeholder="Ex: Nu Pagamentos"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
                Tipo
              </label>
              <select
                value={form.type}
                onChange={(e) => setForm({ ...form, type: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              >
                {ACCOUNT_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
                Saldo {editingId ? 'atual' : 'inicial'}
              </label>
              <input
                type="number"
                step="0.01"
                required
                value={form.balance}
                onChange={(e) => setForm({ ...form, balance: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
                placeholder="0,00"
              />
            </div>
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
              Cor
            </label>
            <input
              type="color"
              value={form.color}
              onChange={(e) => setForm({ ...form, color: e.target.value })}
              className="h-10 w-full rounded-xl border border-edge bg-surface"
            />
          </div>

          {error && <p className="text-sm text-danger">{error}</p>}

          <Button type="submit" className="w-full">
            {editingId ? 'Salvar alterações' : 'Criar conta'}
          </Button>
        </form>
      </Modal>
    </div>
  )
}
