import { useState, useEffect } from 'react'
import { getBudgets, getBudgetStatus, upsertBudget, deleteBudget } from '../api/client'
import { CATEGORIES, getCategoryLabel } from '../constants/categories'

const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

export default function BudgetManager() {
  const [budgets, setBudgets] = useState([])
  const [budgetStatus, setBudgetStatus] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Current month for status display
  const [statusMonth, setStatusMonth] = useState(() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  })

  // Form state
  const [formData, setFormData] = useState({
    category: '',
    monthlyLimit: '',
    alertThreshold: '80',
    month: ''
  })
  const [editingId, setEditingId] = useState(null)
  const [formError, setFormError] = useState('')

  // Load budgets and status on mount and when dependencies change
  useEffect(() => {
    loadData()
  }, [statusMonth])

  const loadData = async () => {
    setLoading(true)
    setError('')
    try {
      const [budgetsData, statusData] = await Promise.all([
        getBudgets(),
        getBudgetStatus(statusMonth)
      ])
      setBudgets(budgetsData || [])
      setBudgetStatus(statusData || {})
    } catch (err) {
      setError('Failed to load budget data')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleStatusMonthChange = (direction) => {
    const [y, m] = statusMonth.split('-').map(Number)
    let newMonth = m
    let newYear = y

    if (direction === 'prev') {
      newMonth--
      if (newMonth < 1) {
        newMonth = 12
        newYear--
      }
    } else {
      newMonth++
      if (newMonth > 12) {
        newMonth = 1
        newYear++
      }
    }

    setStatusMonth(`${newYear}-${String(newMonth).padStart(2, '0')}`)
  }

  const getProgressColor = (percentage) => {
    if (percentage < 50) return '#10b981' // green
    if (percentage < 80) return '#f59e0b' // amber/yellow
    return '#ef4444' // red
  }

  const formatAmount = (val) => {
    const num = Number(val)
    return isNaN(num) ? '0' : num.toLocaleString('en-IN')
  }

  const handleFormChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({
      ...prev,
      [name]: value
    }))
  }

  const validateForm = () => {
    setFormError('')

    if (!formData.category) {
      setFormError('Category is required')
      return false
    }

    if (!formData.monthlyLimit || isNaN(Number(formData.monthlyLimit)) || Number(formData.monthlyLimit) <= 0) {
      setFormError('Monthly Limit must be a positive number')
      return false
    }

    const threshold = Number(formData.alertThreshold)
    if (isNaN(threshold) || threshold <= 0 || threshold > 100) {
      setFormError('Alert Threshold must be between 1 and 100')
      return false
    }

    return true
  }

  const handleFormSubmit = async (e) => {
    e.preventDefault()

    if (!validateForm()) return

    try {
      const data = {
        category: formData.category,
        monthlyLimit: Number(formData.monthlyLimit),
        alertThreshold: Number(formData.alertThreshold),
        month: formData.month || null
      }

      if (editingId) {
        data.id = editingId
      }

      await upsertBudget(data)

      // Reset form
      setFormData({
        category: '',
        monthlyLimit: '',
        alertThreshold: '80',
        month: ''
      })
      setEditingId(null)

      // Reload data
      await loadData()
    } catch (err) {
      setFormError(err.message || 'Failed to save budget')
      console.error(err)
    }
  }

  const handleEdit = (budget) => {
    setFormData({
      category: budget.category,
      monthlyLimit: budget.monthlyLimit.toString(),
      alertThreshold: budget.alertThreshold.toString(),
      month: budget.month || ''
    })
    setEditingId(budget.id)
    setFormError('')
    window.scrollTo({ top: document.getElementById('manage-section')?.offsetTop - 100, behavior: 'smooth' })
  }

  const handleDelete = async (id) => {
    if (!confirm('Are you sure you want to delete this budget?')) return

    try {
      await deleteBudget(id)
      await loadData()
    } catch (err) {
      setError('Failed to delete budget')
      console.error(err)
    }
  }

  const handleCancel = () => {
    setFormData({
      category: '',
      monthlyLimit: '',
      alertThreshold: '80',
      month: ''
    })
    setEditingId(null)
    setFormError('')
  }

  const [statusY, statusM] = statusMonth.split('-').map(Number)

  return (
    <div>
      <div className="page-header">
        <h2>Budget Manager</h2>
      </div>

      {error && <p className="error">{error}</p>}

      {/* SECTION 1: Budget Status */}
      <section style={{ marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1rem' }}>
          <h2 style={{ margin: 0, fontSize: '1rem' }}>Budget Status</h2>
          <div className="month-nav" style={{ marginBottom: 0 }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => handleStatusMonthChange('prev')}
            >
              &larr;
            </button>
            <span style={{ minWidth: '100px', textAlign: 'center' }}>
              {MONTHS[statusM - 1]} {statusY}
            </span>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => handleStatusMonthChange('next')}
            >
              &rarr;
            </button>
          </div>
        </div>

        {loading ? (
          <p style={{ color: 'var(--text-secondary)' }}>Loading budget status...</p>
        ) : budgetStatus && Object.keys(budgetStatus).length > 0 ? (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '0.75rem' }}>
            {Object.entries(budgetStatus).map(([category, status]) => {
              const percentage = status.limit > 0 ? (status.spent / status.limit) * 100 : 0
              const isExceeded = percentage > 100
              const isAlert = percentage > status.alertThreshold

              return (
                <div
                  key={category}
                  style={{
                    padding: '1rem',
                    backgroundColor: 'var(--bg-secondary)',
                    border: `1px solid var(--border-color)`,
                    borderRadius: '0.5rem',
                    boxShadow: 'var(--card-shadow)',
                  }}
                >
                  <div style={{ marginBottom: '0.5rem' }}>
                    <h3 style={{ margin: '0 0 0.5rem 0', fontSize: '1.1rem' }}>
                      {status.category === null ? 'Overall' : getCategoryLabel(status.category)}
                    </h3>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                      <span>₹{formatAmount(status.spent)} / ₹{formatAmount(status.limit)}</span>
                      <span>{percentage.toFixed(1)}%</span>
                    </div>
                  </div>

                  {/* Progress Bar */}
                  <div
                    style={{
                      width: '100%',
                      height: '8px',
                      backgroundColor: 'var(--border-color)',
                      borderRadius: '4px',
                      overflow: 'hidden',
                      marginBottom: '0.75rem'
                    }}
                  >
                    <div
                      style={{
                        width: `${Math.min(percentage, 100)}%`,
                        height: '100%',
                        backgroundColor: getProgressColor(percentage),
                        transition: 'width 0.3s ease'
                      }}
                    />
                  </div>

                  {/* Alert Badge */}
                  {isAlert && (
                    <div
                      style={{
                        display: 'inline-block',
                        padding: '0.25rem 0.75rem',
                        borderRadius: '0.25rem',
                        fontSize: '0.8rem',
                        fontWeight: '600',
                        color: 'white',
                        backgroundColor: isExceeded ? '#ef4444' : '#f59e0b'
                      }}
                    >
                      {isExceeded ? 'Over Budget' : 'Alert Threshold'}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        ) : (
          <p style={{ color: 'var(--text-secondary)' }}>No budget data available for this month</p>
        )}
      </section>

      {/* SECTION 2: Manage Budgets */}
      <section id="manage-section" style={{ marginTop: '1.5rem' }}>
        <h2 style={{ fontSize: '1rem', marginBottom: '0.6rem' }}>Manage Budgets</h2>

        {/* Add/Edit Form */}
        <div className="form-section">
          <h3 style={{ marginTop: 0, fontSize: '0.9rem', marginBottom: '0.5rem' }}>
            {editingId ? 'Edit Budget' : 'Add New Budget'}
          </h3>

          {formError && <p className="error">{formError}</p>}

          <form onSubmit={handleFormSubmit}>
            <div className="salary-form">
              <div className="form-group">
                <label htmlFor="category">Category <span style={{ color: '#ef4444' }}>*</span></label>
                <select id="category" name="category" value={formData.category} onChange={handleFormChange}>
                  <option value="">Select Category</option>
                  <option value="OVERALL">Overall</option>
                  {CATEGORIES.map(cat => (
                    <option key={cat.value} value={cat.value}>{cat.label}</option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="monthlyLimit">Monthly Limit <span style={{ color: '#ef4444' }}>*</span></label>
                <input id="monthlyLimit" type="number" name="monthlyLimit" placeholder="Enter amount"
                  value={formData.monthlyLimit} onChange={handleFormChange} min="0" step="0.01" />
              </div>

              <div className="form-group">
                <label htmlFor="alertThreshold">Alert Threshold (%) <span style={{ color: '#ef4444' }}>*</span></label>
                <input id="alertThreshold" type="number" name="alertThreshold" placeholder="80"
                  value={formData.alertThreshold} onChange={handleFormChange} min="1" max="100" />
              </div>

              <div className="form-group">
                <label htmlFor="month">Month (Optional)</label>
                <input id="month" type="month" name="month" value={formData.month} onChange={handleFormChange} />
              </div>
            </div>

            <div className="form-actions">
              <button type="submit" className="btn btn-primary">
                {editingId ? 'Update Budget' : 'Add Budget'}
              </button>
              {editingId && (
                <button type="button" className="btn btn-secondary" onClick={handleCancel}>Cancel</button>
              )}
            </div>
          </form>
        </div>

        {/* Budgets Table */}
        {loading ? (
          <p style={{ color: 'var(--text-secondary)' }}>Loading budgets...</p>
        ) : budgets && budgets.length > 0 ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Category</th>
                  <th style={{ textAlign: 'right' }}>Monthly Limit</th>
                  <th style={{ textAlign: 'right' }}>Alert %</th>
                  <th>Month</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {budgets.map((budget) => (
                  <tr key={budget.id}>
                    <td style={{ fontWeight: 500 }}>
                      {budget.category === null ? 'Overall' : getCategoryLabel(budget.category)}
                    </td>
                    <td style={{ textAlign: 'right' }}>₹{formatAmount(budget.monthlyLimit)}</td>
                    <td style={{ textAlign: 'right' }}>{budget.alertThreshold}%</td>
                    <td>
                      {budget.month ? (
                        (() => {
                          const [y, m] = budget.month.split('-').map(Number)
                          return `${MONTHS[m - 1]} ${y}`
                        })()
                      ) : (
                        <span style={{ color: 'var(--text-muted)' }}>Recurring</span>
                      )}
                    </td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <button type="button" className="btn btn-secondary btn-sm"
                        onClick={() => handleEdit(budget)}>Edit</button>
                      <button type="button" className="btn btn-danger btn-sm"
                        onClick={() => handleDelete(budget.id)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">
            <p>No budgets created yet. Add one using the form above.</p>
          </div>
        )}
      </section>
    </div>
  )
}
