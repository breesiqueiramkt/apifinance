import { useEffect, useMemo, useState } from 'react'
import api from '../api/client'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import MoneyValue from '../components/ui/MoneyValue'
import StatusStamp from '../components/ui/StatusStamp'

const STATUS_OPTIONS = [
  { value: 'PAID', label: 'Pago' },
  { value: 'PENDING', label: 'Pendente' },
  { value: 'LATE', label: 'Atrasado' },
  { value: 'SCHEDULED', label: 'Agendado' }
]

const RECURRENCE_OPTIONS = [
  { value: 'NONE', label: 'Não se repete' },
  { value: 'WEEKLY', label: 'Semanal' },
  { value: 'MONTHLY', label: 'Mensal' },
  { value: 'YEARLY', label: 'Anual' }
]

const EMPTY_FORM = {
  accountId: '',
  categoryId: '',
  type: 'EXPENSE',
  description: '',
  amount: '',
  date: new Date().toISOString().slice(0, 10),
  paymentMethod: '',
  status: 'PAID',
  recurrence: 'NONE',
  notes: ''
}

export default function Transactions() {
  const [transactions, setTransactions] = useState([])
  const [accounts, setAccounts] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [error, setError] = useState('')
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [confirmDeleteId, setConfirmDeleteId] = useState(null)

  function loadAll() {
    setLoading(true)
    Promise.all([api.get('/transactions'), api.get('/accounts'), api.get('/categories')])
      .then(([tx, acc, cat]) => {
        setTransactions(tx.data)
        setAccounts(acc.data)
        setCategories(cat.data)
      })
      .finally(() => setLoading(false))
  }

  useEffect(loadAll, [])

  const filteredCategories = useMemo(
    () => categories.filter((c) => c.type === form.type),
    [categories, form.type]
  )

  const visibleTransactions = useMemo(
    () => (typeFilter === 'ALL' ? transactions : transactions.filter((t) => t.type === typeFilter)),
    [transactions, typeFilter]
  )

  function openCreate(type = 'EXPENSE') {
    setEditingId(null)
    setForm({ ...EMPTY_FORM, type, accountId: accounts[0]?.id || '' })
    setError('')
    setModalOpen(true)
  }

  function openEdit(t) {
    setEditingId(t.id)
    setForm({
      accountId: t.accountId,
      categoryId: t.categoryId || '',
      type: t.type,
      description: t.description,
      amount: t.amount,
      date: t.date,
      paymentMethod: t.paymentMethod || '',
      status: t.status,
      recurrence: t.recurrence,
      notes: t.notes || ''
    })
    setError('')
    setModalOpen(true)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    const payload = {
      ...form,
      accountId: Number(form.accountId),
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      amount: Number(form.amount)
    }
    try {
      if (editingId) {
        await api.put(`/transactions/${editingId}`, payload)
      } else {
        await api.post('/transactions', payload)
      }
      setModalOpen(false)
      loadAll()
    } catch (err) {
      setError(err.response?.data?.message || 'Não foi possível salvar o lançamento.')
    }
  }

  async function handleDelete(id) {
    await api.delete(`/transactions/${id}`)
    setConfirmDeleteId(null)
    loadAll()
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-xl font-semibold">Receitas e despesas</h1>
          <p className="text-sm text-ink-muted">Todo lançamento pago atualiza o saldo da conta automaticamente.</p>
        </div>
        <div className="flex gap-2">
          <Button variant="ghost" onClick={() => openCreate('INCOME')}>
            + Receita
          </Button>
          <Button onClick={() => openCreate('EXPENSE')}>+ Despesa</Button>
        </div>
      </div>

      <div className="flex gap-2">
        {['ALL', 'INCOME', 'EXPENSE'].map((f) => (
          <button
            key={f}
            onClick={() => setTypeFilter(f)}
            className={`rounded-xl border px-3 py-1.5 text-xs font-semibold ${
              typeFilter === f
                ? 'border-ledger bg-ledger text-white'
                : 'border-edge text-ink-muted hover:bg-surface'
            }`}
          >
            {f === 'ALL' ? 'Todos' : f === 'INCOME' ? 'Receitas' : 'Despesas'}
          </button>
        ))}
      </div>

      <Card className="!p-0 overflow-hidden">
        {loading ? (
          <p className="p-5 text-sm text-ink-muted">Carregando lançamentos...</p>
        ) : accounts.length === 0 ? (
          <p className="p-5 text-sm text-ink-muted">
            Cadastre uma conta bancária primeiro para conseguir lançar receitas e despesas.
          </p>
        ) : visibleTransactions.length === 0 ? (
          <p className="p-5 text-sm text-ink-muted">Nenhum lançamento encontrado.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-edge text-left text-xs uppercase tracking-wide text-ink-muted">
                <th className="px-5 py-3 font-medium">Descrição</th>
                <th className="px-5 py-3 font-medium">Categoria</th>
                <th className="px-5 py-3 font-medium">Conta</th>
                <th className="px-5 py-3 font-medium">Data</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3 text-right font-medium">Valor</th>
                <th className="px-5 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-edge">
              {visibleTransactions.map((t) => (
                <tr key={t.id} className="hover:bg-paper">
                  <td className="px-5 py-3 font-medium">{t.description}</td>
                  <td className="px-5 py-3 text-ink-muted">{t.categoryName || '—'}</td>
                  <td className="px-5 py-3 text-ink-muted">{t.accountName}</td>
                  <td className="px-5 py-3 text-ink-muted">
                    {new Date(t.date + 'T00:00:00').toLocaleDateString('pt-BR')}
                  </td>
                  <td className="px-5 py-3">
                    <StatusStamp status={t.status} />
                  </td>
                  <td className="px-5 py-3 text-right">
                    <MoneyValue
                      value={t.type === 'EXPENSE' ? -t.amount : t.amount}
                      className="text-sm font-medium"
                      duration={300}
                    />
                  </td>
                  <td className="px-5 py-3 text-right">
                    <button
                      onClick={() => openEdit(t)}
                      className="mr-2 text-xs font-semibold text-ledger hover:underline"
                    >
                      Editar
                    </button>
                    {confirmDeleteId === t.id ? (
                      <button
                        onClick={() => handleDelete(t.id)}
                        className="text-xs font-semibold text-danger hover:underline"
                      >
                        Confirmar?
                      </button>
                    ) : (
                      <button
                        onClick={() => setConfirmDeleteId(t.id)}
                        className="text-xs font-semibold text-danger hover:underline"
                      >
                        Excluir
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      <Modal
        open={modalOpen}
        title={editingId ? 'Editar lançamento' : form.type === 'INCOME' ? 'Nova receita' : 'Nova despesa'}
        onClose={() => setModalOpen(false)}
      >
        <form onSubmit={handleSubmit} className="max-h-[70vh] space-y-3 overflow-y-auto pr-1">
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => setForm({ ...form, type: 'INCOME', categoryId: '' })}
              className={`rounded-xl border px-3 py-2 text-sm font-semibold ${
                form.type === 'INCOME' ? 'border-ledger bg-ledger text-white' : 'border-edge text-ink-muted'
              }`}
            >
              Receita
            </button>
            <button
              type="button"
              onClick={() => setForm({ ...form, type: 'EXPENSE', categoryId: '' })}
              className={`rounded-xl border px-3 py-2 text-sm font-semibold ${
                form.type === 'EXPENSE' ? 'border-danger bg-danger text-white' : 'border-edge text-ink-muted'
              }`}
            >
              Despesa
            </button>
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
              Descrição
            </label>
            <input
              required
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              placeholder="Ex: Supermercado"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
                Valor
              </label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                value={form.amount}
                onChange={(e) => setForm({ ...form, amount: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
                placeholder="0,00"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
                Data
              </label>
              <input
                type="date"
                required
                value={form.date}
                onChange={(e) => setForm({ ...form, date: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
                Conta
              </label>
              <select
                required
                value={form.accountId}
                onChange={(e) => setForm({ ...form, accountId: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              >
                <option value="" disabled>
                  Selecione
                </option>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
                Categoria
              </label>
              <select
                value={form.categoryId}
                onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              >
                <option value="">Sem categoria</option>
                {filteredCategories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.icon} {c.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
                Status
              </label>
              <select
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              >
                {STATUS_OPTIONS.map((s) => (
                  <option key={s.value} value={s.value}>
                    {s.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
                Recorrência
              </label>
              <select
                value={form.recurrence}
                onChange={(e) => setForm({ ...form, recurrence: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              >
                {RECURRENCE_OPTIONS.map((r) => (
                  <option key={r.value} value={r.value}>
                    {r.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
              Forma de pagamento
            </label>
            <input
              value={form.paymentMethod}
              onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              placeholder="Ex: Pix, débito, dinheiro"
            />
          </div>

          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">
              Observação
            </label>
            <textarea
              value={form.notes}
              onChange={(e) => setForm({ ...form, notes: e.target.value })}
              rows={2}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
            />
          </div>

          {error && <p className="text-sm text-danger">{error}</p>}

          <Button type="submit" className="w-full">
            {editingId ? 'Salvar alterações' : 'Salvar lançamento'}
          </Button>
        </form>
      </Modal>
    </div>
  )
}
