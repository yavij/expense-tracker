import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getExpenseSummary } from '../api/client'

const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

const DEFAULT_WIDGETS = [
  { id: 'savings', label: 'Savings' },
  { id: 'loans', label: 'Loans' },
  { id: 'expenses', label: 'Expenses' },
  { id: 'actions', label: 'Quick Actions' },
]

export default function Dashboard() {
  const [summary, setSummary] = useState(null)
  const [month, setMonth] = useState(() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
  })
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState('')

  // Layout management
  const [layout, setLayout] = useState(() => {
    return localStorage.getItem('dashboard-layout') || 'default'
  })
  const [widgets, setWidgets] = useState(() => {
    const saved = localStorage.getItem('dashboard-order')
    return saved ? JSON.parse(saved) : DEFAULT_WIDGETS
  })
  const [draggedItem, setDraggedItem] = useState(null)

  useEffect(() => {
    localStorage.setItem('dashboard-layout', layout)
  }, [layout])

  useEffect(() => {
    localStorage.setItem('dashboard-order', JSON.stringify(widgets))
  }, [widgets])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setErr('')
    getExpenseSummary(month).then(data => {
      if (cancelled) return
      if (data && typeof data.savings !== 'undefined') setSummary(data)
      else setErr('Failed to load summary')
      setLoading(false)
    }).catch(() => {
      if (!cancelled) { setErr('Failed to load summary'); setLoading(false) }
    })
    return () => { cancelled = true }
  }, [month])

  const [y, m] = month.split('-').map(Number)
  const prevMonth = () => {
    if (m === 1) setMonth(`${y - 1}-12`)
    else setMonth(`${y}-${String(m - 1).padStart(2, '0')}`)
  }
  const nextMonth = () => {
    if (m === 12) setMonth(`${y + 1}-01`)
    else setMonth(`${y}-${String(m + 1).padStart(2, '0')}`)
  }

  const formatAmount = (val) => {
    const num = Number(val)
    return isNaN(num) ? '0' : num.toLocaleString('en-IN')
  }

  const handleDragStart = (e, index) => {
    setDraggedItem(index)
    e.dataTransfer.effectAllowed = 'move'
  }

  const handleDragOver = (e) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
  }

  const handleDrop = (e, targetIndex) => {
    e.preventDefault()
    if (draggedItem === null || draggedItem === targetIndex) {
      setDraggedItem(null)
      return
    }
    const newWidgets = [...widgets]
    const [draggedWidget] = newWidgets.splice(draggedItem, 1)
    newWidgets.splice(targetIndex, 0, draggedWidget)
    setWidgets(newWidgets)
    setDraggedItem(null)
  }

  const handleDragEnd = () => {
    setDraggedItem(null)
  }

  const renderWidget = (widget, index) => {
    const isDragging = draggedItem === index
    const className = `drag-widget ${isDragging ? 'dragging' : ''}`

    if (widget.id === 'savings') {
      return (
        <div
          key={widget.id}
          className={className}
          draggable
          onDragStart={(e) => handleDragStart(e, index)}
          onDragOver={handleDragOver}
          onDrop={(e) => handleDrop(e, index)}
          onDragEnd={handleDragEnd}
        >
          <span className="drag-handle">⋮⋮</span>
          <div className="card savings">
            <h3>Savings</h3>
            <div className="amount">{summary?.savings > 0 ? '+' : ''}{formatAmount(summary?.savings || 0)}</div>
          </div>
        </div>
      )
    } else if (widget.id === 'loans') {
      return (
        <div
          key={widget.id}
          className={className}
          draggable
          onDragStart={(e) => handleDragStart(e, index)}
          onDragOver={handleDragOver}
          onDrop={(e) => handleDrop(e, index)}
          onDragEnd={handleDragEnd}
        >
          <span className="drag-handle">⋮⋮</span>
          <div className="card loans">
            <h3>Loans</h3>
            <div className="amount">{formatAmount(summary?.loans || 0)}</div>
          </div>
        </div>
      )
    } else if (widget.id === 'expenses') {
      return (
        <div
          key={widget.id}
          className={className}
          draggable
          onDragStart={(e) => handleDragStart(e, index)}
          onDragOver={handleDragOver}
          onDrop={(e) => handleDrop(e, index)}
          onDragEnd={handleDragEnd}
        >
          <span className="drag-handle">⋮⋮</span>
          <div className="card expenses">
            <h3>Expenses</h3>
            <div className="amount">{formatAmount(summary?.expenses || 0)}</div>
          </div>
        </div>
      )
    } else if (widget.id === 'actions') {
      return (
        <div
          key={widget.id}
          className={className}
          draggable
          onDragStart={(e) => handleDragStart(e, index)}
          onDragOver={handleDragOver}
          onDrop={(e) => handleDrop(e, index)}
          onDragEnd={handleDragEnd}
        >
          <span className="drag-handle">⋮⋮</span>
          <div className="quick-actions">
            <Link to="/app/expenses/new" className="btn btn-primary">+ Add Expense</Link>
            <Link to="/app/expenses" className="btn btn-secondary">View All Expenses</Link>
          </div>
        </div>
      )
    }
  }

  const renderDefault = () => (
    <>
      <div className="month-nav">
        <button type="button" className="btn btn-secondary" onClick={prevMonth}>&larr;</button>
        <span>{MONTHS[m - 1]} {y}</span>
        <button type="button" className="btn btn-secondary" onClick={nextMonth}>&rarr;</button>
      </div>
      {loading && <p style={{ color: 'var(--text-secondary)' }}>Loading summary...</p>}
      {err && <p className="error">{err}</p>}
      {summary && !loading && (
        <div className="cards">
          {widgets.map((w, i) => renderWidget(w, i))}
        </div>
      )}
    </>
  )

  const renderCompact = () => (
    <>
      <div className="month-nav">
        <button type="button" className="btn btn-secondary" onClick={prevMonth}>&larr;</button>
        <span>{MONTHS[m - 1]} {y}</span>
        <button type="button" className="btn btn-secondary" onClick={nextMonth}>&rarr;</button>
      </div>
      {loading && <p style={{ color: 'var(--text-secondary)' }}>Loading summary...</p>}
      {err && <p className="error">{err}</p>}
      {summary && !loading && (
        <div className="cards">
          {widgets.map((w, i) => renderWidget(w, i))}
        </div>
      )}
    </>
  )

  const renderSidebar = () => (
    <>
      <div className="dashboard-sidebar">
        <div className="sidebar-panel">
          <div className="month-nav" style={{ flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button type="button" className="btn btn-secondary" onClick={prevMonth} style={{ flex: 1 }}>&larr;</button>
              <span style={{ flex: 1, textAlign: 'center', alignSelf: 'center' }}>{MONTHS[m - 1]} {y}</span>
              <button type="button" className="btn btn-secondary" onClick={nextMonth} style={{ flex: 1 }}>&rarr;</button>
            </div>
          </div>
        </div>
        <div>
          {loading && <p style={{ color: 'var(--text-secondary)' }}>Loading summary...</p>}
          {err && <p className="error">{err}</p>}
          {summary && !loading && (
            <div className="cards">
              {widgets.map((w, i) => renderWidget(w, i))}
            </div>
          )}
        </div>
      </div>
    </>
  )

  return (
    <>
      <div className={`dashboard-${layout}`}>
        {layout === 'default' && renderDefault()}
        {layout === 'compact' && renderCompact()}
        {layout === 'sidebar' && renderSidebar()}
      </div>
    </>
  )
}
