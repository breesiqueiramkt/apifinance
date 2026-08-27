import { useEffect, useState } from 'react'
import api from '../api/client'
import Card from '../components/ui/Card'
import MoneyValue from '../components/ui/MoneyValue'

export default function Dashboard() {
  const [data, setData] = useState(null)
  const [insights, setInsights] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([api.get('/dashboard'), api.get('/dashboard/insights')])
      .then(([dashRes, insightsRes]) => {
        setData(dashRes.data)
        setInsights(insightsRes.data)
      })
      .catch(() => setError('Não foi possível carregar o dashboard agora.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return <p className="text-sm text-ink-muted">Carregando seu resumo financeiro...</p>
  }

  if (error) {
    return <p className="text-sm text-danger">{error}</p>
  }

  const variation = data.netWorth - data.previousMonthNetWorth
  const variationPct =
    data.previousMonthNetWorth !== 0 ? (variation / Math.abs(data.previousMonthNetWorth)) * 100 : 0

  const maxCategory = Math.max(1, ...data.expensesByCategory.map((c) => Number(c.total)))

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        <Card title="Patrimônio total" icon="💰">
          <MoneyValue value={data.netWorth} className="text-2xl font-semibold" />
          <p className={`mt-1 text-xs ${variation >= 0 ? 'text-ledger' : 'text-danger'}`}>
            {variation >= 0 ? '▲' : '▼'} {Math.abs(variationPct).toFixed(1)}% vs. mês anterior
          </p>
        </Card>

        <Card title="Disponível" icon="🏦">
          <MoneyValue value={data.available} className="text-2xl font-semibold" />
          <p className="mt-1 text-xs text-ink-muted">soma de todas as contas</p>
        </Card>

        <Card title="Investimentos" icon="📈">
          <MoneyValue value={data.investedTotal} className="text-2xl font-semibold" />
          <p className="mt-1 text-xs text-ink-muted">valor atual investido</p>
        </Card>

        <Card title="Receitas do mês" icon="💵">
          <MoneyValue value={data.monthlyIncome} className="text-2xl font-semibold text-ledger" />
          <p className="mt-1 text-xs text-ink-muted">lançamentos pagos neste mês</p>
        </Card>

        <Card title="Despesas do mês" icon="💸">
          <MoneyValue value={data.monthlyExpenses} className="text-2xl font-semibold text-danger" />
          <p className="mt-1 text-xs text-ink-muted">lançamentos pagos neste mês</p>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card title="Saldo previsto" icon="📊" className="lg:col-span-1">
          <MoneyValue value={data.projectedBalance} className="text-2xl font-semibold" />
          <p className="mt-1 text-xs text-ink-muted">disponível + pendentes do mês</p>
        </Card>

        <Card title="Taxa de poupança" icon="📈" className="lg:col-span-1">
          <p className="money text-2xl font-semibold text-ledger">
            {Number(data.savingsRate).toFixed(1)}%
          </p>
          <p className="mt-1 text-xs text-ink-muted">
            você guardou {Number(data.savingsRate).toFixed(1)}% da sua renda este mês
          </p>
        </Card>

        <Card title="Comprometimento da renda" icon="⚖️" className="lg:col-span-1">
          <p className="money text-2xl font-semibold">{Number(data.expenseCommitment).toFixed(1)}%</p>
          <p className="mt-1 text-xs text-ink-muted">da sua renda já está comprometida com despesas</p>
        </Card>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card title="Gastos por categoria (mês atual)" icon="🧾">
          {data.expensesByCategory.length === 0 ? (
            <p className="text-sm text-ink-muted">Nenhuma despesa lançada este mês ainda.</p>
          ) : (
            <ul className="space-y-3">
              {data.expensesByCategory.map((c) => (
                <li key={c.categoryId ?? 'none'}>
                  <div className="mb-1 flex items-center justify-between text-sm">
                    <span>
                      {c.icon} {c.categoryName}
                    </span>
                    <MoneyValue value={c.total} className="text-xs" duration={400} />
                  </div>
                  <div className="h-2 w-full overflow-hidden rounded-full bg-paper">
                    <div
                      className="h-full rounded-full bg-ledger"
                      style={{ width: `${(Number(c.total) / maxCategory) * 100}%` }}
                    />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card title="Próximas contas (30 dias)" icon="📅">
          {data.upcomingPending.length === 0 ? (
            <p className="text-sm text-ink-muted">Nenhuma conta pendente nos próximos 30 dias. 🎉</p>
          ) : (
            <ul className="divide-y divide-edge">
              {data.upcomingPending.map((bill) => (
                <li key={bill.transactionId} className="flex items-center justify-between py-2.5 text-sm">
                  <div>
                    <p className="font-medium">{bill.description}</p>
                    <p className="text-xs text-ink-muted">
                      {new Date(bill.dueDate + 'T00:00:00').toLocaleDateString('pt-BR')}
                    </p>
                  </div>
                  <MoneyValue value={bill.amount} className="text-sm" duration={400} />
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      {insights && insights.insights.length > 0 && (
        <Card title="Análise financeira automática" icon="🤖">
          <ul className="space-y-2.5">
            {insights.insights.map((text, idx) => (
              <li key={idx} className="flex gap-2 text-sm">
                <span className="text-ledger">•</span>
                <span>{text}</span>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  )
}
