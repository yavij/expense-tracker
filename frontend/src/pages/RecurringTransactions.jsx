import { useState, useEffect } from 'react'
import {
  getRecurringTransactions,
  createRecurringTransaction,
  updateRecurringTransaction,
  deleteRecurringTransaction,
  processRecurringTransactions,
} from '../api/client'
import { CATEGORIES, getCategoryLabel } from '../constants/categories'

export default function RecurringTransactions() {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [processing, setProcessing] = useState(false)

  // Form fields
  const [formData, setFormData] = useState({
    name: '',
    category: '',
    amount: '',
    currency: 'INR',
    frequency: 'MONTHLY',
    nextDueDate: '',
    isActive: true,
  })

  function load() {
    setLoading(true)
    setErr('')
    getRecurringTransactions()
      .then(data => {
        setList(Array.isArray(data) ? data : [])
        setLoading(false)
      })
      .catch(() => {
        setErr('Failed to load recurring transactions')
        setLoading(false)
      })
  }

  useEffect(() => {
    load()
  }, [])

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setErr('')

    if (!formData.name || !formData.category || !formData.amount || !formData.nextDueDate) {
      setErr('Please fill in all required fields')
      return
    }

    try {
      const payload = {
        ...formData,
        amount: Number(formData.amount),
      }

      if (editingId) {
        await updateRecurringTransaction(editingId, payload)
      } else {
        await createRecurringTransaction(payload)
      }

      setFormData({
        name: '',
        category: '',
        amount: '',
        currency: 'INR',
        frequency: 'MONTHLY',
        nextDueDate: '',
        isActive: true,
      })
      setEditingId(null)
      setShowForm(false)
      load()
    } catch (error) {
      setErr(error.message || 'Failed to save recurring transaction')
    }
  }

  const handleEdit = (transaction) => {
    setFormData({
      name: transaction.name,
      category: transaction.category,
      amount: transaction.amount.toString(),
      currency: transaction.currency || 'INR',
      frequency: transaction.frequency,
      nextDueDate: transaction.nextDueDate,
      isActive: transaction.isActive !== false,
    })
    setEditingId(transaction.id)
    setShowForm(true)
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this recurring transaction? This cannot be undone.')) return
    try {
      await deleteRecurringTransaction(id)
      load()
    } catch {
      setErr('Failed to delete recurring transaction')
    }
  }

  const handleToggleActive = async (transaction) => {
    try {
      await updateRecurringTransaction(transaction.id, {
        ...transaction,
        isActive: !transaction.isActive,
      })
      load()
    } catch {
      setErr('Failed to update transaction status')
    }
  }

  const handleProcessDue = async () => {
    setProcessing(true)
    try {
      await processRecurringTransactions()
      setErr('')
      load()
    } catch (error) {
      setErr(error.message || 'Failed to process recurring transactions')
    } finally {
      setProcessing(false)
    }
  }

  const handleCancel = () => {
    setFormData({
      name: '',
      category: '',
      amount: '',
      currency: 'INR',
      frequency: 'MONTHLY',
      nextDueDate: '',
      isActive: true,
    })
    setEditingId(null)
    setShowForm(false)
    setErr('')
  }

  return (
    <>
      <div className="page-header">
        <h2>Recurring Transactions</h2>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            className="btn btn-primary"
            onClick={handleProcessDue}
            disabled={processing}
            style={{ opacity: processing ? 0.6 : 1 }}
          >
            {processing ? 'Processing...' : 'Process Due'}
          </button>
          <button
            className="btn btn-primary"
            onClick={() => {
              setFormData({
                name: '',
                category: '',
                amount: '',
                currency: 'INR',
                frequency: 'MONTHLY',
                nextDueDate: '',
                isActive: true,
              })
              setEditingId(null)
              setShowForm(true)
            }}
          >
            + Add Recurring Transaction
          </button>
        </div>
      </div>

      {err && <p className="error">{err}</p>}

      {showForm && (
        <div className="table-wrap" style={{ marginBottom: '1.5rem', padding: '1.25rem' }}>
          <h3 style={{ marginTop: 0, marginBottom: '1rem', fontSize: '1.05rem', fontWeight: 600 }}>
            {editingId ? 'Edit Recurring Transaction' : 'Add New Recurring Transaction'}
          </h3>
          <form onSubmit={handleSubmit}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
              <div className="form-group">
                <label htmlFor="name">Name *</label>
                <input
                  id="name"
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  placeholder="e.g., Monthly Rent"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="category">Category *</label>
                <select
                  id="category"
                  name="category"
                  value={formData.category}
                  onChange={handleInputChange}
                  required
                >
                  <option value="">Select category</option>
                  {CATEGORIES.map(c => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="amount">Amount *</label>
                <input
                  id="amount"
                  type="number"
                  name="amount"
                  step="0.01"
                  value={formData.amount}
                  onChange={handleInputChange}
                  placeholder="0.00"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="currency">Currency</label>
                <select
                  id="currency"
                  name="currency"
                  value={formData.currency}
                  onChange={handleInputChange}
                >
                  <option value="INR">INR</option>
                  <option value="USD">USD</option>
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="frequency">Frequency *</label>
                <select
                  id="frequency"
                  name="frequency"
                  value={formData.frequency}
                  onChange={handleInputChange}
                  required
                >
                  <option value="DAILY">Daily</option>
                  <option value="WEEKLY">Weekly</option>
                  <option value="MONTHLY">Monthly</option>
                  <option value="YEARLY">Yearly</option>
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="nextDueDate">Next Due Date *</label>
                <input
                  id="nextDueDate"
                  type="date"
                  name="nextDueDate"
                  value={formData.nextDueDate}
                  onChange={handleInputChange}
                  required
                />
              </div>

              <div className="form-group" style={{ display: 'flex', alignItems: 'flex-end' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 500, cursor: 'pointer' }}>
                  <input
                    type="checkbox"
                    name="isActive"
                    checked={formData.isActive}
                    onChange={handleInputChange}
                  />
                  <span>Active</span>
                </label>
              </div>
            </div>

            <div className="form-actions" style={{ marginTop: '1rem' }}>
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update' : 'Create'}
              </button>
              <button type="button" className="btn btn-outline" onClick={handleCancel}>
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}

      {loading && <p style={{ color: 'var(--text-secondary)' }}>Loading recurring transactions...</p>}

      {!loading && (
        <div className="table-wrap">
          {list.length === 0 ? (
            <p className="empty">
              No recurring transactions found. {!showForm && 'Click the button above to add one.'}
            </p>
          ) : (
            <>
              <table>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Category</th>
                    <th style={{ textAlign: 'right' }}>Amount</th>
                    <th>Frequency</th>
                    <th>Next Due Date</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map(transaction => (
                    <tr key={transaction.id}>
                      <td style={{ fontWeight: 500 }}>{transaction.name}</td>
                      <td>{getCategoryLabel(transaction.category)}</td>
                      <td style={{ textAlign: 'right', fontWeight: 600 }}>
                        {transaction.currency === 'USD' ? '$' : '₹'}
                        {Number(transaction.amount).toLocaleString('en-IN')}
                      </td>
                      <td>{transaction.frequency}</td>
                      <td>{transaction.nextDueDate}</td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-sm"
                          onClick={() => handleToggleActive(transaction)}
                          style={{
                            background: transaction.isActive ? '#f0fdf4' : '#fffbeb',
                            color: transaction.isActive ? '#16a34a' : '#d97706',
                            border: 'none',
                            padding: '0.3rem 0.6rem',
                            fontSize: '0.8rem',
                            fontWeight: 600,
                            cursor: 'pointer',
                            borderRadius: '6px',
                          }}
                        >
                          {transaction.isActive ? 'Active' : 'Paused'}
                        </button>
                      </td>
                      <td style={{ whiteSpace: 'nowrap' }}>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => handleEdit(transaction)}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger btn-sm"
                          onClick={() => handleDelete(transaction.id)}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div
                style={{
                  padding: '0.75rem 1rem',
                  borderTop: '1px solid var(--border-color)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  fontSize: '0.9rem',
                }}
              >
                <span style={{ color: 'var(--text-secondary)' }}>
                  {list.length} transaction{list.length !== 1 ? 's' : ''}
                </span>
                <span style={{ fontWeight: 700 }}>
                  Total Monthly: ₹
                  {list
                    .filter(t => t.isActive && t.frequency === 'MONTHLY')
                    .reduce((sum, t) => sum + Number(t.amount || 0), 0)
                    .toLocaleString('en-IN')}
                </span>
              </div>
            </>
          )}
        </div>
      )}
    </>
  )
}
