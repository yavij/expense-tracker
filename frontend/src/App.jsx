import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import ExpenseList from './pages/ExpenseList'
import ExpenseForm from './pages/ExpenseForm'
import InvestmentList from './pages/InvestmentList'
import InvestmentForm from './pages/InvestmentForm'
import SalaryTracker from './pages/SalaryTracker'
import DebtList from './pages/DebtList'
import DebtForm from './pages/DebtForm'
import DebtPayoff from './pages/DebtPayoff'
import Analytics from './pages/Analytics'
import RecurringTransactions from './pages/RecurringTransactions'
import BudgetManager from './pages/BudgetManager'
import Admin from './pages/Admin'
import PaymentGate from './components/PaymentGate'

function ProtectedRoute({ children }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="app">Loading...</div>
  if (!user) return <Navigate to="/" replace />
  return children
}

function AdminRoute({ children }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="app">Loading...</div>
  if (!user) return <Navigate to="/" replace />
  if (user?.role !== 'ADMIN') return <Navigate to="/app/dashboard" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/app" element={
        <ProtectedRoute>
          <Layout />
        </ProtectedRoute>
      }>
        <Route index element={<Navigate to="/app/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="expenses" element={<ExpenseList />} />
        <Route path="expenses/new" element={<ExpenseForm />} />
        <Route path="expenses/:id/edit" element={<ExpenseForm />} />
        <Route path="investments" element={<InvestmentList />} />
        <Route path="investments/new" element={<InvestmentForm />} />
        <Route path="investments/:id/edit" element={<InvestmentForm />} />
        <Route path="salary" element={<SalaryTracker />} />
        <Route path="debts" element={<DebtList />} />
        <Route path="debts/new" element={<DebtForm />} />
        <Route path="debts/:id/edit" element={<DebtForm />} />
        <Route path="debts/payoff" element={<DebtPayoff />} />
        <Route path="analytics" element={<PaymentGate feature="Analytics"><Analytics /></PaymentGate>} />
        <Route path="recurring" element={<PaymentGate feature="Recurring Transactions"><RecurringTransactions /></PaymentGate>} />
        <Route path="budgets" element={<PaymentGate feature="Budget Manager"><BudgetManager /></PaymentGate>} />
        <Route path="admin" element={
          <AdminRoute>
            <Admin />
          </AdminRoute>
        } />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
