import { Routes, Route } from 'react-router-dom'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Accounts from './pages/Accounts'
import Transactions from './pages/Transactions'
import Bills from './pages/Bills'
import CreditCards from './pages/CreditCards'
import Investments from './pages/Investments'
import Goals from './pages/Goals'
import Debts from './pages/Debts'
import Calculators from './pages/Calculators'
import Reports from './pages/Reports'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/layout/Layout'

function Protected({ children }) {
  return (
    <ProtectedRoute>
      <Layout>{children}</Layout>
    </ProtectedRoute>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route path="/" element={<Protected><Dashboard /></Protected>} />
      <Route path="/accounts" element={<Protected><Accounts /></Protected>} />
      <Route path="/transactions" element={<Protected><Transactions /></Protected>} />
      <Route path="/bills" element={<Protected><Bills /></Protected>} />
      <Route path="/credit-cards" element={<Protected><CreditCards /></Protected>} />
      <Route path="/investments" element={<Protected><Investments /></Protected>} />
      <Route path="/goals" element={<Protected><Goals /></Protected>} />
      <Route path="/debts" element={<Protected><Debts /></Protected>} />
      <Route path="/calculators" element={<Protected><Calculators /></Protected>} />
      <Route path="/reports" element={<Protected><Reports /></Protected>} />
    </Routes>
  )
}
