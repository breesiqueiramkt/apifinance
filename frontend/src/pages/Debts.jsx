import { useEffect, useState } from 'react'
import api from '../api/client'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import MoneyValue from '../components/ui/MoneyValue'

const EMPTY_FORM = {
  creditor: '',
  originalAmount: '',
  currentAmount: '',
  interestRate: '',
  installmentsTotal: '',
  installmentsPaid: '0',
  dueDate: ''
}

export default function Debts() {
  const [debts, setDebts] = useState([])
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [error, setError] = useState('')

  function loadAll() {
    setLoading(true)
    Promise.all([api.get('/debts'), api.get('/debts/summary')])
      .then(([d, s]) => {
        setDebts(d.data)
        setSummary(s.data)
      })
      .finally(() => setLoading(false))
  }

  useEffect(loadAll, [])

  function openCreate() {
    setEditingId(null)
    setForm(EMPTY_FORM)
    setError('')
    setModalOpen(true)
  }

  function openEdit(debt) {
    setEditingId(debt.id)
    setForm({
      creditor: debt.creditor,
      originalAmount: debt.originalAmount,
      currentAmount: debt.currentAmount,
      interestRate: debt.interestRate || '',
      installmentsTotal: debt.installmentsTotal || '',
      installmentsPaid: debt.installmentsPaid || '0',
      dueDate: debt.dueDate || ''
    })
    setError('')
    setModalOpen(true)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    const payload = {
      ...form,
      originalAmount: Number(form.originalAmount),
      currentAmount: Number(form.currentAmount),
      interestRate: form.interestRate ? Number(form.interestRate) : null,
      installmentsTotal: form.installmentsTotal ? Number(form.installmentsTotal) : null,
      installmentsPaid: form.installmentsPaid ? Number(form.installmentsPaid) : 0,
      dueDate: form.dueDate || null
    }
    try {
      if (editingId) {
        await api.put(`/debts/${editingId}`, payload)
      } else {
        await api.post('/debts', payload)
      }
      setModalOpen(false)
      loadAll()
    } catch (err) {
      setError(err.response?.data?.message || 'Não foi possível salvar a dívida.')
    }
  }

  async function handleDelete(id) {
    await api.delete(`/debts/${id}`)
    loadAll()
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-xl font-semibold">Dívidas</h1>
          <p className="text-sm text-ink-muted">
            Ordenadas por prioridade — método avalanche: quita primeiro quem mais encarece.
          </p>
        </div>
        <Button onClick={openCreate}>+ Nova dívida</Button>
      </div>

      {summary && (
        <div className="grid grid-cols-3 gap-4">
          <Card title="Total original" icon="📄">
            <MoneyValue value={summary.totalOriginal} className="text-lg font-semibold" />
          </Card>
          <Card title="Já pago" icon="✅">
            <MoneyValue value={summary.totalPaid} className="text-lg font-semibold text-ledger" />
          </Card>
          <Card title="Restante" icon="⏳">
            <MoneyValue value={summary.totalRemaining} className="text-lg font-semibold text-danger" />
          </Card>
        </div>
      )}

      {loading ? (
        <p className="text-sm text-ink-muted">Carregando dívidas...</p>
      ) : debts.length === 0 ? (
        <Card>
          <p className="text-sm text-ink-muted">Nenhuma dívida cadastrada. 🎉</p>
        </Card>
      ) : (
        <div className="space-y-3">
          {debts.map((debt, idx) => (
            <Card key={debt.id}>
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0">
                  <div className="mb-1 flex items-center gap-2">
                    {debt.status === 'OPEN' && (
                      <span className="rounded-full bg-ledger px-2 py-0.5 text-[10px] font-bold text-white">
                        #{idx + 1} prioridade
                      </span>
                    )}
                    <p className="font-display font-semibold">{debt.creditor}</p>
                  </div>
                  <p className="text-xs text-ink-muted">
                    {debt.interestRate ? `${debt.interestRate}% a.m.` : 'sem juros informados'}
                    {debt.installmentsTotal ? ` · ${debt.installmentsPaid}/${debt.installmentsTotal} parcelas` : ''}
                    {debt.dueDate ? ` · vence ${new Date(debt.dueDate + 'T00:00:00').toLocaleDateString('pt-BR')}` : ''}
                  </p>
                  {debt.estimatedInstallmentValue && (
                    <p className="mt-1 text-xs text-ink-muted">
                      Parcela estimada: <MoneyValue value={debt.estimatedInstallmentValue} duration={0} /> · Juros restantes
                      estimados: <MoneyValue value={debt.estimatedRemainingInterest} duration={0} />
                    </p>
                  )}
                </div>
                <div className="shrink-0 text-right">
                  <MoneyValue value={debt.currentAmount} className="text-lg font-semibold text-danger" duration={300} />
                  <p className="text-xs text-ink-muted">
                    de <MoneyValue value={debt.originalAmount} duration={0} />
                  </p>
                </div>
              </div>
              <div className="mt-3 flex gap-2">
                <Button variant="ghost" className="flex-1" onClick={() => openEdit(debt)}>
                  Editar
                </Button>
                <Button variant="danger" className="flex-1" onClick={() => handleDelete(debt.id)}>
                  Excluir
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={modalOpen} title={editingId ? 'Editar dívida' : 'Nova dívida'} onClose={() => setModalOpen(false)}>
        <form onSubmit={handleSubmit} className="max-h-[70vh] space-y-3 overflow-y-auto pr-1">
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Credor</label>
            <input
              required
              value={form.creditor}
              onChange={(e) => setForm({ ...form, creditor: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              placeholder="Ex: Banco XP, empréstimo pessoal..."
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Valor original</label>
              <input
                type="number"
                step="0.01"
                required
                value={form.originalAmount}
                onChange={(e) => setForm({ ...form, originalAmount: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Valor atual (restante)</label>
              <input
                type="number"
                step="0.01"
                required
                value={form.currentAmount}
                onChange={(e) => setForm({ ...form, currentAmount: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Taxa de juros (% ao mês)</label>
            <input
              type="number"
              step="0.01"
              value={form.interestRate}
              onChange={(e) => setForm({ ...form, interestRate: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              placeholder="Ex: 2.5"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Parcelas totais</label>
              <input
                type="number"
                value={form.installmentsTotal}
                onChange={(e) => setForm({ ...form, installmentsTotal: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Parcelas pagas</label>
              <input
                type="number"
                value={form.installmentsPaid}
                onChange={(e) => setForm({ ...form, installmentsPaid: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Vencimento (opcional)</label>
            <input
              type="date"
              value={form.dueDate}
              onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
            />
          </div>
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button type="submit" className="w-full">
            {editingId ? 'Salvar alterações' : 'Adicionar dívida'}
          </Button>
        </form>
      </Modal>
    </div>
  )
}
