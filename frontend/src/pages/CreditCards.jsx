import { useEffect, useState } from 'react'
import api from '../api/client'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import MoneyValue from '../components/ui/MoneyValue'

const EMPTY_CARD_FORM = { name: '', bank: '', creditLimit: '', closingDay: '10', dueDay: '17' }
const EMPTY_PURCHASE_FORM = { description: '', amount: '', purchaseDate: new Date().toISOString().slice(0, 10), categoryId: '', installments: 1 }

export default function CreditCards() {
  const [cards, setCards] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [cardModalOpen, setCardModalOpen] = useState(false)
  const [cardForm, setCardForm] = useState(EMPTY_CARD_FORM)
  const [purchaseModal, setPurchaseModal] = useState(null)
  const [purchaseForm, setPurchaseForm] = useState(EMPTY_PURCHASE_FORM)
  const [error, setError] = useState('')

  function loadCards() {
    setLoading(true)
    api.get('/credit-cards').then((res) => setCards(res.data)).finally(() => setLoading(false))
  }

  useEffect(loadCards, [])
  useEffect(() => {
    api.get('/categories').then((res) => setCategories(res.data.filter((c) => c.type === 'EXPENSE')))
  }, [])

  async function handleCreateCard(e) {
    e.preventDefault()
    setError('')
    try {
      await api.post('/credit-cards', {
        ...cardForm,
        creditLimit: Number(cardForm.creditLimit),
        closingDay: Number(cardForm.closingDay),
        dueDay: Number(cardForm.dueDay)
      })
      setCardModalOpen(false)
      setCardForm(EMPTY_CARD_FORM)
      loadCards()
    } catch (err) {
      setError(err.response?.data?.message || 'Não foi possível criar o cartão.')
    }
  }

  async function handleDeleteCard(id) {
    await api.delete(`/credit-cards/${id}`)
    loadCards()
  }

  function openPurchase(card) {
    setPurchaseModal(card)
    setPurchaseForm(EMPTY_PURCHASE_FORM)
    setError('')
  }

  async function handleAddPurchase(e) {
    e.preventDefault()
    setError('')
    try {
      await api.post(`/credit-cards/${purchaseModal.id}/purchases`, {
        ...purchaseForm,
        amount: Number(purchaseForm.amount),
        installments: Number(purchaseForm.installments),
        categoryId: purchaseForm.categoryId || null
      })
      setPurchaseModal(null)
      loadCards()
    } catch (err) {
      setError(err.response?.data?.message || 'Não foi possível lançar a compra.')
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-xl font-semibold">Cartões de crédito</h1>
          <p className="text-sm text-ink-muted">Fatura atual, próxima fatura e limite calculados automaticamente.</p>
        </div>
        <Button onClick={() => setCardModalOpen(true)}>+ Novo cartão</Button>
      </div>

      {loading ? (
        <p className="text-sm text-ink-muted">Carregando cartões...</p>
      ) : cards.length === 0 ? (
        <Card>
          <p className="text-sm text-ink-muted">Nenhum cartão cadastrado ainda.</p>
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          {cards.map((card) => {
            const usagePct = Math.min(100, (Number(card.limitUsed) / Number(card.creditLimit)) * 100 || 0)
            return (
              <Card key={card.id}>
                <div className="mb-3 flex items-start justify-between">
                  <div>
                    <p className="font-display font-semibold">{card.name}</p>
                    <p className="text-xs text-ink-muted">
                      {card.bank || '—'} · fecha dia {card.closingDay}, vence dia {card.dueDay}
                    </p>
                  </div>
                  <button onClick={() => handleDeleteCard(card.id)} className="text-xs font-semibold text-danger">
                    Excluir
                  </button>
                </div>

                <div className="mb-3 grid grid-cols-2 gap-3">
                  <div>
                    <p className="text-xs text-ink-muted">Fatura atual</p>
                    <MoneyValue value={card.currentInvoice} className="text-lg font-semibold" duration={400} />
                  </div>
                  <div>
                    <p className="text-xs text-ink-muted">Próxima fatura</p>
                    <MoneyValue value={card.nextInvoice} className="text-lg font-semibold" duration={400} />
                  </div>
                </div>

                <div className="mb-1 flex items-center justify-between text-xs text-ink-muted">
                  <span>
                    Limite usado: <MoneyValue value={card.limitUsed} duration={300} />
                  </span>
                  <span>
                    Disponível: <MoneyValue value={card.limitAvailable} duration={300} />
                  </span>
                </div>
                <div className="mb-4 h-2 w-full overflow-hidden rounded-full bg-paper">
                  <div
                    className={`h-full rounded-full ${usagePct > 80 ? 'bg-danger' : 'bg-ledger'}`}
                    style={{ width: `${usagePct}%` }}
                  />
                </div>

                <Button variant="ghost" className="w-full" onClick={() => openPurchase(card)}>
                  + Lançar compra
                </Button>
              </Card>
            )
          })}
        </div>
      )}

      <Modal open={cardModalOpen} title="Novo cartão" onClose={() => setCardModalOpen(false)}>
        <form onSubmit={handleCreateCard} className="space-y-3">
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Nome</label>
            <input
              required
              value={cardForm.name}
              onChange={(e) => setCardForm({ ...cardForm, name: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              placeholder="Ex: Nubank Ultravioleta"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Banco</label>
            <input
              value={cardForm.bank}
              onChange={(e) => setCardForm({ ...cardForm, bank: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Limite</label>
            <input
              type="number"
              step="0.01"
              required
              value={cardForm.creditLimit}
              onChange={(e) => setCardForm({ ...cardForm, creditLimit: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Dia de fechamento</label>
              <input
                type="number"
                min="1"
                max="31"
                required
                value={cardForm.closingDay}
                onChange={(e) => setCardForm({ ...cardForm, closingDay: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Dia de vencimento</label>
              <input
                type="number"
                min="1"
                max="31"
                required
                value={cardForm.dueDay}
                onChange={(e) => setCardForm({ ...cardForm, dueDay: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
          </div>
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button type="submit" className="w-full">
            Criar cartão
          </Button>
        </form>
      </Modal>

      <Modal open={!!purchaseModal} title={`Nova compra — ${purchaseModal?.name || ''}`} onClose={() => setPurchaseModal(null)}>
        <form onSubmit={handleAddPurchase} className="space-y-3">
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Descrição</label>
            <input
              required
              value={purchaseForm.description}
              onChange={(e) => setPurchaseForm({ ...purchaseForm, description: e.target.value })}
              className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Valor total</label>
              <input
                type="number"
                step="0.01"
                required
                value={purchaseForm.amount}
                onChange={(e) => setPurchaseForm({ ...purchaseForm, amount: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Parcelas</label>
              <input
                type="number"
                min="1"
                max="48"
                required
                value={purchaseForm.installments}
                onChange={(e) => setPurchaseForm({ ...purchaseForm, installments: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Data da compra</label>
              <input
                type="date"
                required
                value={purchaseForm.purchaseDate}
                onChange={(e) => setPurchaseForm({ ...purchaseForm, purchaseDate: e.target.value })}
                className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Categoria</label>
              <select
                value={purchaseForm.categoryId}
                onChange={(e) => setPurchaseForm({ ...purchaseForm, categoryId: e.target.value })}
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
          </div>
          {error && <p className="text-sm text-danger">{error}</p>}
          <Button type="submit" className="w-full">
            Lançar compra
          </Button>
        </form>
      </Modal>
    </div>
  )
}
