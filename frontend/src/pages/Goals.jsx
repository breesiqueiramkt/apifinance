import { useEffect, useState } from 'react'
import api from '../api/client'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import MoneyValue from '../components/ui/MoneyValue'

const EMPTY_FORM = { name: '', targetAmount: '', currentAmount: '0', deadline: '' }

export default function Goals() {
  const [goals, setGoals] = useState([])
  const [loading, setLoading] = useState(true)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [contributeModal, setContributeModal] = useState(null)
  const [contributeAmount, setContributeAmount] = useState('')
  const [contributeLoading, setContributeLoading] = useState(false)
  const [error, setError] = useState('')

  function loadGoals() {
    setLoading(true)
    api.get('/goals').then((res) => setGoals(res.data)).finally(() => setLoading(false))
  }

  useEffect(loadGoals, [])

  function openCreate() {
    setEditingId(null)
    setForm(EMPTY_FORM)
    setError('')
    setModalOpen(true)
  }

  function openEdit(goal) {
    setEditingId(goal.id)
    setForm({
      name: goal.name,
      targetAmount: goal.targetAmount,
      currentAmount: goal.currentAmount,
      deadline: goal.deadline || ''
    })
    setError('')
    setModalOpen(true)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    const payload = {
      ...form,
      targetAmount: Number(form.targetAmount),
      currentAmount: Number(form.currentAmount),
      deadline: form.deadline || null
    }
    try {
      if (editingId) {
        await api.put(`/goals/${editingId}`, payload)
      } else {
        await api.post('/goals', payload)
      }
      setModalOpen(false)
      loadGoals()
    } catch (err) {
      setError(err.response?.data?.message || 'Não foi possível salvar a meta.')
    }
  }

  async function handleDelete(id) {
    await api.delete(`/goals/${id}`)
    loadGoals()
  }

  function openContribute(goal) {
    setContributeModal(goal)
    setContributeAmount('')
    setError('')
  }

  async function confirmContribute() {
    if (contributeLoading) return
    setContributeLoading(true)
    try {
      await api.post(`/goals/${contributeModal.id}/contribute`, { amount: Number(contributeAmount) })
      setContributeModal(null)
      setContributeAmount('')
      loadGoals()
    } catch (err) {
      setError(err.response?.data?.message || 'Não foi possível registrar o aporte.')
    } finally {
      setContributeLoading(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-xl font-semibold">Minhas metas</h1>
          <p className="text-sm text-ink-muted">Quanto você precisa guardar por mês para chegar lá.</p>
        </div>
        <Button onClick={openCreate}>+ Nova meta</Button>
      </div>

      {loading ? (
        <p className="text-sm text-ink-muted">Carregando metas...</p>
      ) : goals.length === 0 ? (
        <Card>
          <p className="text-sm text-ink-muted">Nenhuma meta cadastrada ainda.</p>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {goals.map((goal) => (
            <Card key={goal.id}>
              <p className="font-display font-semibold">{goal.name}</p>
              <div className="my-3 flex items-baseline justify-between">
                <MoneyValue value={goal.currentAmount} className="text-lg font-semibold" duration={400} />
                <span className="text-xs text-ink-muted">
                  de <MoneyValue value={goal.targetAmount} duration={0} />
                </span>
              </div>
              <div className="mb-1 h-2.5 w-full overflow-hidden rounded-full bg-paper">
                <div className="h-full rounded-full bg-ledger" style={{ width: `${goal.progressPercent}%` }} />
              </div>
              <p className="mb-3 text-xs text-ink-muted">{Number(goal.progressPercent).toFixed(0)}% concluído</p>

              {goal.monthlyContributionNeeded != null && (
                <p className="mb-3 text-xs text-ink-muted">
                  Guarde <MoneyValue value={goal.monthlyContributionNeeded} className="font-semibold text-ink" duration={0} />
                  /mês para chegar até {new Date(goal.deadline + 'T00:00:00').toLocaleDateString('pt-BR')}
                </p>
              )}

              <div className="flex gap-2">
                <Button variant="ghost" className="flex-1" onClick={() => openEdit(goal)}>
                  Editar
                </Button>
                <Button className="flex-1" onClick={() => openContribute(goal)}>
                  Aportar
                </Button>
              </div>
              <button
                onClick={() => handleDelete(goal.id)}
                className="mt-2 w-full text-center text-xs font-semibold text-danger"
              >
                Excluir meta
              </button>
            </Card>
          ))}
        </div>
      )}

      <Modal open={modalOpen} title={editingId ? 'Editar meta' : 'Nova meta'} onClose={() => setModalOpen(false)}>
        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Nome da meta</label>
            <input
              required
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              placeholder="Ex: Reserva de emergência"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Valor objetivo</label>
              <input
                type="number"
                step="0.01"
                required
                value={form.targetAmount}
                onChange={(e) => setForm({ ...form, targetAmount: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Valor atual</label>
              <input
                type="number"
                step="0.01"
                value={form.currentAmount}
                onChange={(e) => setForm({ ...form, currentAmount: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Prazo (opcional)</label>
            <input
              type="date"
              value={form.deadline}
              onChange={(e) => setForm({ ...form, deadline: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
            />
          </div>
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button type="submit" className="w-full">
            {editingId ? 'Salvar alterações' : 'Criar meta'}
          </Button>
        </form>
      </Modal>

      <Modal open={!!contributeModal} title={`Aportar em "${contributeModal?.name || ''}"`} onClose={() => setContributeModal(null)}>
        <div className="space-y-3">
          <input
            type="number"
            step="0.01"
            autoFocus
            value={contributeAmount}
            onChange={(e) => setContributeAmount(e.target.value)}
            placeholder="Valor do aporte"
            className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
          />
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button className="w-full" onClick={confirmContribute} disabled={!contributeAmount || contributeLoading}>
            {contributeLoading ? 'Confirmando...' : 'Confirmar aporte'}
          </Button>
        </div>
      </Modal>
    </div>
  )
}
