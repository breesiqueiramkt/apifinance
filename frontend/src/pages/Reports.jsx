import { useEffect, useMemo, useState } from 'react'
import {
  ResponsiveContainer, LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend
} from 'recharts'
import api from '../api/client'
import Card from '../components/ui/Card'
import MoneyValue from '../components/ui/MoneyValue'

const currencyFormatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })

const PERIODS = [
  { label: '3 meses', value: 3 },
  { label: '6 meses', value: 6 },
  { label: '12 meses', value: 12 }
]

export default function Reports() {
  const [months, setMonths] = useState(6)
  const [cashflow, setCashflow] = useState(null)
  const [netWorth, setNetWorth] = useState(null)
  const [categoryReport, setCategoryReport] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    const today = new Date()
    const start = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().slice(0, 10)
    const end = new Date(today.getFullYear(), today.getMonth() + 1, 0).toISOString().slice(0, 10)

    Promise.all([
      api.get('/reports/cashflow', { params: { months } }),
      api.get('/reports/net-worth', { params: { months } }),
      api.get('/reports/expenses-by-category', { params: { start, end } })
    ])
      .then(([cf, nw, cat]) => {
        setCashflow(cf.data)
        setNetWorth(nw.data)
        setCategoryReport(cat.data)
      })
      .finally(() => setLoading(false))
  }, [months])

  const chartTheme = useMemo(() => {
    const styles = getComputedStyle(document.documentElement)
    return {
      primary: styles.getPropertyValue('--primary').trim() || '#1F6F54',
      danger: styles.getPropertyValue('--danger').trim() || '#A23B3B',
      ink: styles.getPropertyValue('--ink').trim() || '#12261E',
      border: styles.getPropertyValue('--border').trim() || '#D8DED9'
    }
  }, [])

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-xl font-semibold">Relatórios</h1>
          <p className="text-sm text-ink-muted">Sua evolução financeira ao longo do tempo.</p>
        </div>
        <div className="flex gap-2">
          {PERIODS.map((p) => (
            <button
              key={p.value}
              onClick={() => setMonths(p.value)}
              className={`rounded-xl border px-3 py-1.5 text-xs font-semibold ${
                months === p.value ? 'border-ledger bg-ledger text-white' : 'border-edge text-ink-muted hover:bg-surface'
              }`}
            >
              {p.label}
            </button>
          ))}
        </div>
      </div>

      {loading ? (
        <p className="text-sm text-ink-muted">Carregando relatórios...</p>
      ) : (
        <>
          <Card title="Receitas x Despesas">
            <div className="h-72 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={cashflow.points}>
                  <CartesianGrid strokeDasharray="3 3" stroke={chartTheme.border} />
                  <XAxis dataKey="month" stroke={chartTheme.ink} fontSize={12} />
                  <YAxis stroke={chartTheme.ink} fontSize={12} tickFormatter={(v) => currencyFormatter.format(v)} width={80} />
                  <Tooltip formatter={(v) => currencyFormatter.format(v)} />
                  <Legend />
                  <Bar dataKey="income" name="Receitas" fill={chartTheme.primary} radius={[4, 4, 0, 0]} />
                  <Bar dataKey="expenses" name="Despesas" fill={chartTheme.danger} radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </Card>

          <Card title="Evolução do patrimônio (contas)">
            <div className="h-72 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={netWorth.points}>
                  <CartesianGrid strokeDasharray="3 3" stroke={chartTheme.border} />
                  <XAxis dataKey="month" stroke={chartTheme.ink} fontSize={12} />
                  <YAxis stroke={chartTheme.ink} fontSize={12} tickFormatter={(v) => currencyFormatter.format(v)} width={80} />
                  <Tooltip formatter={(v) => currencyFormatter.format(v)} />
                  <Line type="monotone" dataKey="netWorth" name="Patrimônio" stroke={chartTheme.primary} strokeWidth={2.5} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
            <p className="mt-2 text-xs text-ink-muted">
              Considera apenas o saldo das contas; investimentos entram no patrimônio do dashboard, mas ainda não têm
              histórico de valor ao longo do tempo.
            </p>
          </Card>

          <Card title="Gastos por categoria (mês atual)">
            {categoryReport.slices.length === 0 ? (
              <p className="text-sm text-ink-muted">Nenhuma despesa este mês ainda.</p>
            ) : (
              <ul className="space-y-3">
                {categoryReport.slices.map((slice) => (
                  <li key={slice.categoryId ?? 'none'}>
                    <div className="mb-1 flex items-center justify-between text-sm">
                      <span>
                        {slice.icon} {slice.categoryName}
                      </span>
                      <div className="flex items-center gap-2">
                        <MoneyValue value={slice.total} className="text-xs" duration={300} />
                        <span className="text-xs text-ink-muted">
                          ({((Number(slice.total) / Number(categoryReport.total)) * 100).toFixed(0)}%)
                        </span>
                      </div>
                    </div>
                    <div className="h-2 w-full overflow-hidden rounded-full bg-paper">
                      <div
                        className="h-full rounded-full bg-ledger"
                        style={{ width: `${(Number(slice.total) / Number(categoryReport.total)) * 100}%` }}
                      />
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </>
      )}
    </div>
  )
}
