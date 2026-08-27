import { useEffect, useState } from 'react'
import api from '../api/client'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import MoneyValue from '../components/ui/MoneyValue'

const EMPTY_FORM = {
  name: '',
  investmentTypeId: '',
  investedAmount: '',
  currentAmount: '',
  investedAt: new Date().toISOString().slice(0, 10),
  expectedRate: '',
  institution: '',
  notes: ''
}

export default function Investments() {
  const [investments, setInvestments] = useState([])
  const [types, setTypes] = useState([])
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [error, setError] = useState('')

  function loadAll() {
    setLoading(true)
    Promise.all([api.get('/investments'), api.get('/investments/types'), api.get('/investments/summary')])
      .then(([inv, t, s]) => {
        setInvestments(inv.data)
        setTypes(t.data)
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

  function openEdit(inv) {
    setEditingId(inv.id)
    setForm({
      name: inv.name,
      investmentTypeId: inv.investmentTypeId || '',
      investedAmount: inv.investedAmount,
      currentAmount: inv.currentAmount,
      investedAt: inv.investedAt,
      expectedRate: inv.expectedRate || '',
      institution: inv.institution || '',
      notes: inv.notes || ''
    })
    setError('')
    setModalOpen(true)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    const payload = {
      ...form,
      investmentTypeId: form.investmentTypeId || null,
      investedAmount: Number(form.investedAmount),
      currentAmount: Number(form.currentAmount),
      expectedRate: form.expectedRate ? Number(form.expectedRate) : null
    }
    try {
      if (editingId) {
        await api.put(`/investments/${editingId}`, payload)
      } else {
        await api.post('/investments', payload)
      }
      setModalOpen(false)
      loadAll()
    } catch (err) {
      setError(err.response?.data?.message || 'Não foi possível salvar o investimento.')
    }
  }

  async function handleDelete(id) {
    await api.delete(`/investments/${id}`)
    loadAll()
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-xl font-semibold">Investimentos</h1>
          <p className="text-sm text-ink-muted">Renda fixa, variável e outros - tudo em um só lugar.</p>
        </div>
        <Button onClick={openCreate}>+ Novo investimento</Button>
      </div>

      {summary && (
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <Card title="Total investido" icon="💰">
            <MoneyValue value={summary.totalInvested} className="text-xl font-semibold" />
          </Card>
          <Card title="Valor atual" icon="📈">
            <MoneyValue value={summary.totalCurrent} className="text-xl font-semibold" />
          </Card>
          <Card title="Rendimento" icon="✨">
            <MoneyValue
              value={summary.totalReturn}
              className={`text-xl font-semibold ${Number(summary.totalReturn) >= 0 ? 'text-ledger' : 'text-danger'}`}
            />
            <p className="mt-1 text-xs text-ink-muted">{Number(summary.totalReturnPercent).toFixed(1)}%</p>
          </Card>
          <Card title="Renda estimada/mês" icon="🗓️">
            <MoneyValue value={summary.estimatedMonthlyIncome} className="text-xl font-semibold" />
          </Card>
        </div>
      )}
      <p className="text-xs italic text-ink-muted">Rentabilidade estimada. Os valores reais podem variar.</p>

      {loading ? (
        <p className="text-sm text-ink-muted">Carregando investimentos...</p>
      ) : investments.length === 0 ? (
        <Card>
          <p className="text-sm text-ink-muted">Nenhum investimento cadastrado ainda.</p>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {investments.map((inv) => (
            <Card key={inv.id}>
              <div className="mb-2 flex items-start justify-between">
                <div>
                  <p className="font-display font-semibold">{inv.name}</p>
                  <p className="text-xs text-ink-muted">
                    {inv.investmentTypeName || 'Sem tipo'} {inv.institution ? `· ${inv.institution}` : ''}
                  </p>
                </div>
              </div>
              <MoneyValue value={inv.currentAmount} className="text-xl font-semibold" duration={400} />
              <p className={`text-xs ${Number(inv.returnAmount) >= 0 ? 'text-ledger' : 'text-danger'}`}>
                {Number(inv.returnAmount) >= 0 ? '+' : ''}
                {Number(inv.returnPercent).toFixed(1)}% desde {new Date(inv.investedAt + 'T00:00:00').toLocaleDateString('pt-BR')}
              </p>
              <div className="mt-4 flex gap-2">
                <Button variant="ghost" className="flex-1" onClick={() => openEdit(inv)}>
                  Editar
                </Button>
                <Button variant="danger" className="flex-1" onClick={() => handleDelete(inv.id)}>
                  Excluir
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal open={modalOpen} title={editingId ? 'Editar investimento' : 'Novo investimento'} onClose={() => setModalOpen(false)}>
        <form onSubmit={handleSubmit} className="max-h-[70vh] space-y-3 overflow-y-auto pr-1">
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Nome</label>
            <input
              required
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              placeholder="Ex: Tesouro Selic 2029"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Tipo</label>
            <select
              value={form.investmentTypeId}
              onChange={(e) => setForm({ ...form, investmentTypeId: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
            >
              <option value="">Selecione</option>
              {types.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Valor investido</label>
              <input
                type="number"
                step="0.01"
                required
                value={form.investedAmount}
                onChange={(e) => setForm({ ...form, investedAmount: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Valor atual</label>
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
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Data do investimento</label>
              <input
                type="date"
                required
                value={form.investedAt}
                onChange={(e) => setForm({ ...form, investedAt: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Taxa esperada (% a.a.)</label>
              <input
                type="number"
                step="0.01"
                value={form.expectedRate}
                onChange={(e) => setForm({ ...form, expectedRate: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
                placeholder="Ex: 11.5"
              />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Instituição</label>
            <input
              value={form.institution}
              onChange={(e) => setForm({ ...form, institution: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
            />
          </div>
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button type="submit" className="w-full">
            {editingId ? 'Salvar alterações' : 'Adicionar'}
          </Button>
        </form>
      </Modal>
    </div>
  )
}
