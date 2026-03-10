import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getExpense, createExpense, updateExpense } from '../api/client'
import { CATEGORIES, isLoanCategory } from '../constants/categories'

export default function ExpenseForm() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()
  const [category, setCategory] = useState('DAILY')
  const [amount, setAmount] = useState('')
  const [currency, setCurrency] = useState('INR')
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [note, setNote] = useState('')
  const [loanName, setLoanName] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [fetching, setFetching] = useState(isEdit)

  useEffect(() => {
    if (!isEdit) return
    setFetching(true)
    getExpense(id).then(row => {
      if (row && row.id) {
        setCategory(row.category)
        setAmount(String(row.amount))
        setCurrency(row.currency || 'INR')
        setDate(row.date || new Date().toISOString().slice(0, 10))
        setNote(row.note || '')
        setLoanName(row.loanName || '')
      } else {
        setError('Expense not found')
      }
    }).catch(() => {
      setError('Failed to load expense')
    }).finally(() => setFetching(false))
  }, [id, isEdit])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    const amt = parseFloat(amount)
    if (isNaN(amt) || amt <= 0) {
      setError('Amount is required and must be a positive number')
      return
    }
    if (!date) {
      setError('Date is required')
      return
    }
    if (isLoanCategory(category) && !loanName.trim()) {
      setError('Loan name is recommended for loan entries')
      // Not blocking — just a warning; proceed anyway
    }
    setLoading(true)
    try {
      const payload = { category, amount: amt, currency, date, note: note.trim() || undefined }
      if (isLoanCategory(category)) payload.loanName = loanName.trim() || undefined
      const result = isEdit ? await updateExpense(id, payload) : await createExpense(payload)
      if (result?.error) {
        setError(result.error)
        setLoading(false)
        return
      }
      navigate('/app/expenses')
    } catch (e) {
      setError(e?.message || 'Failed to save. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  if (fetching) return <p style={{ color: 'var(--text-secondary)' }}>Loading expense...</p>

  return (
    <>
      <div className="page-header">
        <h2>{isEdit ? 'Edit Expense' : 'Add Expense'}</h2>
      </div>
      <form onSubmit={handleSubmit} style={{ maxWidth: 440 }}>
        <div className="form-group">
          <label>Category *</label>
          <select value={category} onChange={e => setCategory(e.target.value)} required>
            {CATEGORIES.map(c => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </select>
        </div>
        <div className="form-group">
          <label>Amount *</label>
          <input
            type="number" step="0.01" min="0.01"
            value={amount} onChange={e => setAmount(e.target.value)}
            placeholder="0.00" required
          />
        </div>
        <div className="form-group">
          <label>Currency</label>
          <select value={currency} onChange={e => setCurrency(e.target.value)}>
            <option value="INR">INR (Indian Rupee)</option>
            <option value="USD">USD (US Dollar)</option>
          </select>
        </div>
        <div className="form-group">
          <label>Date *</label>
          <input type="date" value={date} onChange={e => setDate(e.target.value)} required />
        </div>
        <div className="form-group">
          <label>Note</label>
          <textarea
            value={note} onChange={e => setNote(e.target.value)}
            placeholder="What was this expense for?"
            maxLength={500}
          />
        </div>
        {isLoanCategory(category) && (
          <div className="form-group">
            <label>Loan Name</label>
            <input
              type="text" value={loanName} onChange={e => setLoanName(e.target.value)}
              placeholder="e.g. Bank X, Friend Y"
            />
          </div>
        )}
        {error && <p className="error">{error}</p>}
        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Saving...' : isEdit ? 'Update' : 'Save'}
          </button>
          <button type="button" className="btn btn-secondary" onClick={() => navigate('/app/expenses')}>Cancel</button>
        </div>
      </form>
    </>
  )
}
