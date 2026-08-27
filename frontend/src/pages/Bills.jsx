import { useEffect, useMemo, useState } from 'react'
import api from '../api/client'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import MoneyValue from '../components/ui/MoneyValue'
import StatusStamp from '../components/ui/StatusStamp'

const EMPTY_FORM = { description: '', amount: '', dueDate: '', accountId: '', categoryId: '', recurrence: 'NONE' }

const RANGES = [
  { label: '7 dias', value: 7 },
  { label: '30 dias', value: 30 },
  { label: '90 dias', value: 90 },
  { label: 'Todas', value: null }
]

export default function Bills() {
  const [bills, setBills] = useState([])
  const [accounts, setAccounts] = useState([])
  const [categories, setCategories] = useState([])
  const [range, setRange] = useState(30)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [payModal, setPayModal] = useState(null)
  const [payAccountId, setPayAccountId] = useState('')
  const [payLoading, setPayLoading] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [error, setError] = useState('')

  function loadBills() {
    setLoading(true)
    const params = range ? { nextDays: range } : {}
    api.get('/bills', { params }).then((res) => setBills(res.data)).finally(() => setLoading(false))
  }

  useEffect(loadBills, [range])

  useEffect(() => {
    api.get('/accounts').then((res) => setAccounts(res.data))
    api.get('/categories').then((res) => setCategories(res.data.filter((c) => c.type === 'EXPENSE')))
  }, [])

  const total = useMemo(
    () => bills.filter((b) => b.status !== 'PAID').reduce((sum, b) => sum + Number(b.amount), 0),
    [bills]
  )

  function openCreate() {
    setForm(EMPTY_FORM)
    setError('')
    setModalOpen(true)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    try {
      await api.post('/bills', {
        ...form,
        amount: Number(form.amount),
        accountId: form.accountId || null,
        categoryId: form.categoryId || null
      })
      setModalOpen(false)
      loadBills()
    } catch (err) {
      setError(err.response?.data?.message || 'Não foi possível salvar a conta.')
    }
  }

  async function handleDelete(id) {
    await api.delete(`/bills/${id}`)
    loadBills()
  }

  function openPay(bill) {
    setPayModal(bill)
    setPayAccountId(bill.accountId || accounts[0]?.id || '')
    setError('')
  }

  async function confirmPay() {
    if (payLoading) return
    setPayLoading(true)
    try {
      await api.post(`/bills/${payModal.id}/pay`, null, { params: { accountId: payAccountId } })
      setPayModal(null)
      loadBills()
    } catch (err) {
      setError(err.response?.data?.message || 'Não foi possível confirmar o pagamento.')
    } finally {
      setPayLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-xl font-semibold">Contas futuras</h1>
          <p className="text-sm text-ink-muted">
            Em aberto no período: <MoneyValue value={total} className="font-semibold" />
          </p>
        </div>
        <Button onClick={openCreate}>+ Nova conta</Button>
      </div>

      <div className="flex gap-2">
        {RANGES.map((r) => (
          <button
            key={r.label}
            onClick={() => setRange(r.value)}
            className={`rounded-xl border px-3 py-1.5 text-xs font-semibold ${
              range === r.value ? 'border-ledger bg-ledger text-white' : 'border-edge text-ink-muted hover:bg-surface'
            }`}
          >
            {r.label}
          </button>
        ))}
      </div>

      <Card className="!p-0 overflow-hidden">
        {loading ? (
          <p className="p-5 text-sm text-ink-muted">Carregando...</p>
        ) : bills.length === 0 ? (
          <p className="p-5 text-sm text-ink-muted">Nenhuma conta neste período. 🎉</p>
        ) : (
          <ul className="divide-y divide-edge">
            {bills.map((bill) => (
              <li key={bill.id} className="flex items-center justify-between gap-3 px-5 py-3.5">
                <div className="min-w-0">
                  <p className="truncate font-medium">{bill.description}</p>
                  <p className="text-xs text-ink-muted">
                    Vence em {new Date(bill.dueDate + 'T00:00:00').toLocaleDateString('pt-BR')}
                    {bill.daysUntilDue >= 0 ? ` (${bill.daysUntilDue}d)` : ' — atrasada'}
                    {bill.categoryName ? ` · ${bill.categoryName}` : ''}
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-3">
                  <StatusStamp status={bill.daysUntilDue < 0 && bill.status !== 'PAID' ? 'LATE' : bill.status} />
                  <MoneyValue value={bill.amount} className="text-sm font-medium" duration={300} />
                  {bill.status !== 'PAID' && (
                    <Button variant="ghost" onClick={() => openPay(bill)}>
                      Pagar
                    </Button>
                  )}
                  <button onClick={() => handleDelete(bill.id)} className="text-xs font-semibold text-danger">
                    Excluir
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Modal open={modalOpen} title="Nova conta futura" onClose={() => setModalOpen(false)}>
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Descrição</label>
            <input
              required
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              placeholder="Ex: Internet"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Valor</label>
              <input
                type="number"
                step="0.01"
                required
                value={form.amount}
                onChange={(e) => setForm({ ...form, amount: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Vencimento</label>
              <input
                type="date"
                required
                value={form.dueDate}
                onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Categoria</label>
              <select
                value={form.categoryId}
                onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              >
                <option value="">Sem categoria</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.icon} {c.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Recorrência</label>
              <select
                value={form.recurrence}
                onChange={(e) => setForm({ ...form, recurrence: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              >
                <option value="NONE">Não se repete</option>
                <option value="MONTHLY">Mensal</option>
                <option value="WEEKLY">Semanal</option>
                <option value="YEARLY">Anual</option>
              </select>
            </div>
          </div>
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button type="submit" className="w-full">
            Salvar
          </Button>
        </form>
      </Modal>

      <Modal open={!!payModal} title="Pagar conta" onClose={() => setPayModal(null)}>
        {payModal && (
          <div className="space-y-3">
            <p className="text-sm">
              Pagar <strong>{payModal.description}</strong> ({<MoneyValue value={payModal.amount} duration={0} />}) e
              lançar como despesa paga em qual conta?
            </p>
            <select
              value={payAccountId}
              onChange={(e) => setPayAccountId(e.target.value)}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
            >
              {accounts.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.name}
                </option>
              ))}
            </select>
            {error && <p className="text-sm text-danger">{error}</p>}
            <Button className="w-full" onClick={confirmPay} disabled={!payAccountId || payLoading}>
              {payLoading ? 'Confirmando...' : 'Confirmar pagamento'}
            </Button>
          </div>
        )}
      </Modal>
    </div>
  )
}
