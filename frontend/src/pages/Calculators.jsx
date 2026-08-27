import { useState } from 'react'
import api from '../api/client'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import MoneyValue from '../components/ui/MoneyValue'

const CALCULATORS = [
  { id: 'compound', label: 'Juros compostos', icon: '📈' },
  { id: 'independence', label: 'Independência financeira', icon: '🏝️' },
  { id: 'emergency', label: 'Reserva de emergência', icon: '🛟' },
  { id: 'inflation', label: 'Inflação', icon: '📉' },
  { id: 'financing', label: 'Financiamento', icon: '🚗' },
  { id: 'retirement', label: 'Aposentadoria', icon: '🌅' }
]

function Field({ label, ...props }) {
  return (
    <div>
      <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">{label}</label>
      <input
        {...props}
        className="w-full rounded-xl border border-edge bg-surface px-3 py-2.5 text-sm outline-none focus:border-ledger"
      />
    </div>
  )
}

function ResultRow({ label, value, highlight }) {
  return (
    <div className="flex items-center justify-between border-b border-edge py-2 last:border-0">
      <span className="text-sm text-ink-muted">{label}</span>
      <MoneyValue value={value} className={`text-sm font-semibold ${highlight ? 'text-ledger' : ''}`} duration={400} />
    </div>
  )
}

export default function Calculators() {
  const [active, setActive] = useState('compound')

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-display text-xl font-semibold">Calculadoras financeiras</h1>
        <p className="text-sm text-ink-muted">Simule cenários antes de decidir. Taxas são sempre estimativas.</p>
      </div>

      <div className="flex flex-wrap gap-2">
        {CALCULATORS.map((c) => (
          <button
            key={c.id}
            onClick={() => setActive(c.id)}
            className={`rounded-xl border px-3 py-2 text-xs font-semibold ${
              active === c.id ? 'border-ledger bg-ledger text-white' : 'border-edge text-ink-muted hover:bg-surface'
            }`}
          >
            {c.icon} {c.label}
          </button>
        ))}
      </div>

      {active === 'compound' && <CompoundInterestCalc />}
      {active === 'independence' && <FinancialIndependenceCalc />}
      {active === 'emergency' && <EmergencyFundCalc />}
      {active === 'inflation' && <InflationCalc />}
      {active === 'financing' && <FinancingCalc />}
      {active === 'retirement' && <RetirementCalc />}
    </div>
  )
}

function CompoundInterestCalc() {
  const [form, setForm] = useState({ initialValue: '1000', monthlyContribution: '300', annualRate: '11', months: '60' })
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  async function calculate(e) {
    e.preventDefault()
    setLoading(true)
    try {
      const { data } = await api.post('/calculators/compound-interest', {
        initialValue: Number(form.initialValue),
        monthlyContribution: Number(form.monthlyContribution),
        annualRate: Number(form.annualRate),
        months: Number(form.months)
      })
      setResult(data)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card title="Dados da simulação">
        <form onSubmit={calculate} className="space-y-3">
          <Field label="Valor inicial" type="number" step="0.01" value={form.initialValue}
            onChange={(e) => setForm({ ...form, initialValue: e.target.value })} />
          <Field label="Aporte mensal" type="number" step="0.01" value={form.monthlyContribution}
            onChange={(e) => setForm({ ...form, monthlyContribution: e.target.value })} />
          <Field label="Taxa de rendimento (% a.a.)" type="number" step="0.01" value={form.annualRate}
            onChange={(e) => setForm({ ...form, annualRate: e.target.value })} />
          <Field label="Período (meses)" type="number" value={form.months}
            onChange={(e) => setForm({ ...form, months: e.target.value })} />
          <Button type="submit" disabled={loading} className="w-full">Calcular</Button>
        </form>
      </Card>

      <Card title="Resultado">
        {result ? (
          <div>
            <ResultRow label="Total investido" value={result.totalInvested} />
            <ResultRow label="Rendimentos" value={result.totalReturns} highlight />
            <ResultRow label="Patrimônio final" value={result.finalAmount} highlight />
            <p className="mt-3 text-xs text-ink-muted">
              Nunca confundir rendimento com valor aportado: dos {result.evolution.length} meses simulados, o
              rendimento sozinho já representa{' '}
              {((Number(result.totalReturns) / Number(result.finalAmount)) * 100).toFixed(0)}% do patrimônio final.
            </p>
          </div>
        ) : (
          <p className="text-sm text-ink-muted">Preencha os dados e calcule para ver o resultado.</p>
        )}
      </Card>
    </div>
  )
}

function FinancialIndependenceCalc() {
  const [form, setForm] = useState({ monthlyExpenses: '4000', currentNetWorth: '20000', expectedRate: '6', monthlyContribution: '1500' })
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)

  async function calculate(e) {
    e.preventDefault()
    setLoading(true)
    try {
      const { data } = await api.post('/calculators/financial-independence', {
        monthlyExpenses: Number(form.monthlyExpenses),
        currentNetWorth: Number(form.currentNetWorth),
        expectedRate: Number(form.expectedRate),
        monthlyContribution: Number(form.monthlyContribution)
      })
      setResult(data)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card title="Seus dados">
        <form onSubmit={calculate} className="space-y-3">
          <Field label="Gastos mensais" type="number" step="0.01" value={form.monthlyExpenses}
            onChange={(e) => setForm({ ...form, monthlyExpenses: e.target.value })} />
          <Field label="Patrimônio atual" type="number" step="0.01" value={form.currentNetWorth}
            onChange={(e) => setForm({ ...form, currentNetWorth: e.target.value })} />
          <Field label="Rentabilidade estimada (% a.a.)" type="number" step="0.01" value={form.expectedRate}
            onChange={(e) => setForm({ ...form, expectedRate: e.target.value })} />
          <Field label="Aporte mensal" type="number" step="0.01" value={form.monthlyContribution}
            onChange={(e) => setForm({ ...form, monthlyContribution: e.target.value })} />
          <Button type="submit" disabled={loading} className="w-full">Calcular</Button>
        </form>
      </Card>

      <Card title="Resultado">
        {result ? (
          <div>
            <ResultRow label="Patrimônio necessário" value={result.requiredNetWorth} highlight />
            <div className="flex items-center justify-between border-b border-edge py-2 last:border-0">
              <span className="text-sm text-ink-muted">Tempo estimado</span>
              <span className="money text-sm font-semibold">
                {result.yearsToReach != null ? `${result.yearsToReach} anos` : 'mais de 80 anos'}
              </span>
            </div>
            <p className="mt-3 text-xs text-ink-muted">
              Patrimônio necessário para que os rendimentos anuais cubram seus gastos anuais, sem precisar tocar no principal.
            </p>
          </div>
        ) : (
          <p className="text-sm text-ink-muted">Preencha os dados e calcule para ver o resultado.</p>
        )}
      </Card>
    </div>
  )
}

function EmergencyFundCalc() {
  const [form, setForm] = useState({ monthlyExpenses: '2500', months: 6 })
  const [result, setResult] = useState(null)

  async function calculate(e) {
    e.preventDefault()
    const { data } = await api.post('/calculators/emergency-fund', {
      monthlyExpenses: Number(form.monthlyExpenses),
      months: Number(form.months)
    })
    setResult(data)
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card title="Seus dados">
        <form onSubmit={calculate} className="space-y-3">
          <Field label="Gastos mensais" type="number" step="0.01" value={form.monthlyExpenses}
            onChange={(e) => setForm({ ...form, monthlyExpenses: e.target.value })} />
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-wide text-ink-muted">Meses de reserva</label>
            <div className="flex gap-2">
              {[3, 6, 9, 12].map((m) => (
                <button
                  key={m}
                  type="button"
                  onClick={() => setForm({ ...form, months: m })}
                  className={`flex-1 rounded-xl border py-2 text-sm font-semibold ${
                    form.months === m ? 'border-ledger bg-ledger text-white' : 'border-edge text-ink-muted'
                  }`}
                >
                  {m}
                </button>
              ))}
            </div>
          </div>
          <Button type="submit" className="w-full">Calcular</Button>
        </form>
      </Card>

      <Card title="Resultado">
        {result ? (
          <ResultRow label={`Reserva recomendada (${form.months} meses)`} value={result.recommendedAmount} highlight />
        ) : (
          <p className="text-sm text-ink-muted">Preencha os dados e calcule para ver o resultado.</p>
        )}
      </Card>
    </div>
  )
}

function InflationCalc() {
  const [form, setForm] = useState({ currentValue: '1000', annualInflation: '4.5', years: '10' })
  const [result, setResult] = useState(null)

  async function calculate(e) {
    e.preventDefault()
    const { data } = await api.post('/calculators/inflation', {
      currentValue: Number(form.currentValue),
      annualInflation: Number(form.annualInflation),
      years: Number(form.years)
    })
    setResult(data)
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card title="Seus dados">
        <form onSubmit={calculate} className="space-y-3">
          <Field label="Valor atual" type="number" step="0.01" value={form.currentValue}
            onChange={(e) => setForm({ ...form, currentValue: e.target.value })} />
          <Field label="Inflação anual estimada (%)" type="number" step="0.01" value={form.annualInflation}
            onChange={(e) => setForm({ ...form, annualInflation: e.target.value })} />
          <Field label="Período (anos)" type="number" value={form.years}
            onChange={(e) => setForm({ ...form, years: e.target.value })} />
          <Button type="submit" className="w-full">Calcular</Button>
        </form>
      </Card>

      <Card title="Resultado">
        {result ? (
          <div>
            <ResultRow label="Poder de compra futuro do valor de hoje" value={result.futurePurchasingPower} />
            <ResultRow label="Valor nominal necessário no futuro" value={result.futureNominalValueNeeded} highlight />
          </div>
        ) : (
          <p className="text-sm text-ink-muted">Preencha os dados e calcule para ver o resultado.</p>
        )}
      </Card>
    </div>
  )
}

function FinancingCalc() {
  const [form, setForm] = useState({ assetValue: '80000', downPayment: '16000', annualRate: '18', installmentsCount: '48' })
  const [result, setResult] = useState(null)

  async function calculate(e) {
    e.preventDefault()
    const { data } = await api.post('/calculators/financing', {
      assetValue: Number(form.assetValue),
      downPayment: Number(form.downPayment),
      annualRate: Number(form.annualRate),
      installmentsCount: Number(form.installmentsCount)
    })
    setResult(data)
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card title="Dados do financiamento">
        <form onSubmit={calculate} className="space-y-3">
          <Field label="Valor do bem" type="number" step="0.01" value={form.assetValue}
            onChange={(e) => setForm({ ...form, assetValue: e.target.value })} />
          <Field label="Entrada" type="number" step="0.01" value={form.downPayment}
            onChange={(e) => setForm({ ...form, downPayment: e.target.value })} />
          <Field label="Taxa de juros (% a.a.)" type="number" step="0.01" value={form.annualRate}
            onChange={(e) => setForm({ ...form, annualRate: e.target.value })} />
          <Field label="Número de parcelas" type="number" value={form.installmentsCount}
            onChange={(e) => setForm({ ...form, installmentsCount: e.target.value })} />
          <Button type="submit" className="w-full">Calcular</Button>
        </form>
      </Card>

      <Card title="Resultado">
        {result ? (
          <div>
            <ResultRow label="Valor financiado" value={result.financedAmount} />
            <ResultRow label="Valor das parcelas" value={result.installmentValue} highlight />
            <ResultRow label="Total pago" value={result.totalPaid} />
            <ResultRow label="Juros totais" value={result.totalInterest} />
            <p className="mt-3 text-xs text-ink-muted">Calculado pela Tabela Price (parcelas fixas).</p>
          </div>
        ) : (
          <p className="text-sm text-ink-muted">Preencha os dados e calcule para ver o resultado.</p>
        )}
      </Card>
    </div>
  )
}

function RetirementCalc() {
  const [form, setForm] = useState({
    currentAge: '30', retirementAge: '65', currentNetWorth: '15000', monthlyContribution: '800', expectedRate: '10'
  })
  const [result, setResult] = useState(null)

  async function calculate(e) {
    e.preventDefault()
    const { data } = await api.post('/calculators/retirement', {
      currentAge: Number(form.currentAge),
      retirementAge: Number(form.retirementAge),
      currentNetWorth: Number(form.currentNetWorth),
      monthlyContribution: Number(form.monthlyContribution),
      expectedRate: Number(form.expectedRate)
    })
    setResult(data)
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card title="Seus dados">
        <form onSubmit={calculate} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <Field label="Idade atual" type="number" value={form.currentAge}
              onChange={(e) => setForm({ ...form, currentAge: e.target.value })} />
            <Field label="Idade de aposentadoria" type="number" value={form.retirementAge}
              onChange={(e) => setForm({ ...form, retirementAge: e.target.value })} />
          </div>
          <Field label="Patrimônio atual" type="number" step="0.01" value={form.currentNetWorth}
            onChange={(e) => setForm({ ...form, currentNetWorth: e.target.value })} />
          <Field label="Aporte mensal" type="number" step="0.01" value={form.monthlyContribution}
            onChange={(e) => setForm({ ...form, monthlyContribution: e.target.value })} />
          <Field label="Rentabilidade estimada (% a.a.)" type="number" step="0.01" value={form.expectedRate}
            onChange={(e) => setForm({ ...form, expectedRate: e.target.value })} />
          <Button type="submit" className="w-full">Calcular</Button>
        </form>
      </Card>

      <Card title="Projeção">
        {result ? (
          <div>
            <ResultRow label="Patrimônio projetado na aposentadoria" value={result.projectedNetWorth} highlight />
            <ResultRow label="Renda mensal estimada (regra dos 4% a.a.)" value={result.estimatedMonthlyIncome} />
          </div>
        ) : (
          <p className="text-sm text-ink-muted">Preencha os dados e calcule para ver a projeção.</p>
        )}
      </Card>
    </div>
  )
}
