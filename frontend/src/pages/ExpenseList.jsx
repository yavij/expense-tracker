import { useState, useEffect, useRef } from 'react'
import { Link } from 'react-router-dom'
import { getExpenses, deleteExpense, exportCsv, searchExpenses } from '../api/client'
import { CATEGORIES, getCategoryLabel } from '../constants/categories'

export default function ExpenseList() {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [category, setCategory] = useState('')
  const [search, setSearch] = useState('')
  const [err, setErr] = useState('')
  const debounceTimer = useRef(null)

  function load() {
    setLoading(true)
    setErr('')

    // If search query exists, use search endpoint
    if (search.trim()) {
      searchExpenses(search).then(data => {
        setList(Array.isArray(data) ? data : [])
        setLoading(false)
      }).catch(() => {
        setErr('Failed to search expenses')
        setLoading(false)
      })
    } else {
      // Otherwise use filtered endpoint
      const params = {}
      if (from) params.from = from
      if (to) params.to = to
      if (category) params.category = category
      getExpenses(params).then(data => {
        setList(Array.isArray(data) ? data : [])
        setLoading(false)
      }).catch(() => {
        setErr('Failed to load expenses')
        setLoading(false)
      })
    }
  }

  useEffect(() => { load() }, [from, to, category])

  const handleSearch = (value) => {
    setSearch(value)
    if (debounceTimer.current) clearTimeout(debounceTimer.current)
    debounceTimer.current = setTimeout(() => {
      load()
    }, 300)
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this expense? This cannot be undone.')) return
    try {
      await deleteExpense(id)
      load()
    } catch {
      setErr('Failed to delete expense')
    }
  }

  const total = list.reduce((sum, r) => sum + Number(r.amount || 0), 0)

  return (
    <>
      <div className="page-header">
        <h2>Expenses</h2>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button className="btn" style={{ background: '#059669', color: 'white', border: 'none', padding: '0.5rem 1rem', borderRadius: '8px', cursor: 'pointer', fontSize: '0.85rem' }}
            onClick={() => exportCsv('expenses', { startDate: from, endDate: to, category: '' })}>Export CSV</button>
          <Link to="/app/expenses/new" className="btn btn-primary">+ Add Expense</Link>
        </div>
      </div>

      <div className="filters">
        <input type="text" placeholder="Search by note, category, or loan name..." value={search} onChange={e => handleSearch(e.target.value)} title="Search expenses" />
        <input type="date" value={from} onChange={e => setFrom(e.target.value)} title="From date" />
        <input type="date" value={to} onChange={e => setTo(e.target.value)} title="To date" />
        <select value={category} onChange={e => setCategory(e.target.value)}>
          <option value="">All categories</option>
          {CATEGORIES.map(c => (
            <option key={c.value} value={c.value}>{c.label}</option>
          ))}
        </select>
        {(from || to || category || search) && (
          <button type="button" className="btn btn-sm btn-outline" onClick={() => { setFrom(''); setTo(''); setCategory(''); setSearch('') }}>
            Clear filters
          </button>
        )}
      </div>

      {err && <p className="error">{err}</p>}
      {loading && <p style={{ color: 'var(--text-secondary)' }}>Loading expenses...</p>}

      {!loading && (
        <div className="table-wrap">
          {list.length === 0 ? (
            <p className="empty">No expenses found. Add your first expense above.</p>
          ) : (
            <>
              <table>
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Category</th>
                    <th style={{ textAlign: 'right' }}>Amount</th>
                    <th>Note</th>
                    <th>Loan Name</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {list.map(row => (
                    <tr key={row.id}>
                      <td>{row.date}</td>
                      <td>{getCategoryLabel(row.category)}</td>
                      <td style={{ textAlign: 'right', fontWeight: 600 }}>
                        {Number(row.amount).toLocaleString('en-IN')}
                      </td>
                      <td style={{ color: row.note ? 'var(--text-primary)' : 'var(--text-muted)' }}>{row.note || '--'}</td>
                      <td style={{ color: row.loanName ? 'var(--text-primary)' : 'var(--text-muted)' }}>{row.loanName || '--'}</td>
                      <td style={{ whiteSpace: 'nowrap' }}>
                        <Link to={'/app/expenses/' + row.id + '/edit'} className="btn btn-secondary btn-sm">Edit</Link>
                        <button type="button" className="btn btn-danger btn-sm" onClick={() => handleDelete(row.id)}>Delete</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div style={{ padding: '0.75rem 1rem', borderTop: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
                <span style={{ color: 'var(--text-secondary)' }}>{list.length} record{list.length !== 1 ? 's' : ''}</span>
                <span style={{ fontWeight: 700 }}>Total: {total.toLocaleString('en-IN')}</span>
              </div>
            </>
          )}
        </div>
      )}
    </>
  )
}
