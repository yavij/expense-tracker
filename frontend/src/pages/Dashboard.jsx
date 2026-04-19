import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { getExpenseSummary, getMonthlyAnalytics } from '../api/client'

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
  const [analytics, setAnalytics] = useState(null)

  useEffect(() => {
    getMonthlyAnalytics(month).then(setAnalytics).catch(() => {})
  }, [month])

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

  const CHART_COLORS = ['#2563eb', '#059669', '#d97706', '#dc2626', '#7c3aed', '#0891b2', '#ec4899', '#f59e0b', '#6366f1', '#14b8a6']

  const renderCharts = () => {
    const breakdown = analytics?.expenseBreakdown ?? analytics?.categoryBreakdown ?? []
    if (breakdown.length === 0) return null
    const total = breakdown.reduce((s, c) => s + (c.amount ?? 0), 0) || 1

    return (
      <div className="dashboard-charts">
        <div className="dashboard-chart-card">
          <h3>Category Breakdown</h3>
          <div className="mini-donut-wrap">
            <svg viewBox="0 0 200 200" className="donut-svg">
              <circle cx="100" cy="100" r="70" fill="none" stroke="var(--border-color)" strokeWidth="30" />
              {(() => {
                let cumPct = 0
                return breakdown.map((cat, idx) => {
                  const pct = (cat.amount / total) * 100
                  const startAngle = (cumPct / 100) * 360
                  cumPct += pct
                  const endAngle = (cumPct / 100) * 360
                  const r = 80
                  const startRad = (startAngle * Math.PI) / 180 - Math.PI / 2
                  const endRad = (endAngle * Math.PI) / 180 - Math.PI / 2
                  const x1 = 100 + r * Math.cos(startRad)
                  const y1 = 100 + r * Math.sin(startRad)
                  const x2 = 100 + r * Math.cos(endRad)
                  const y2 = 100 + r * Math.sin(endRad)
                  const largeArc = pct > 50 ? 1 : 0
                  return (
                    <path key={idx}
                      d={`M ${x1} ${y1} A ${r} ${r} 0 ${largeArc} 1 ${x2} ${y2}`}
                      fill="none" stroke={CHART_COLORS[idx % CHART_COLORS.length]}
                      strokeWidth="30" />
                  )
                })
              })()}
              <circle cx="100" cy="100" r="45" fill="var(--bg-secondary, white)" />
              <text x="100" y="96" textAnchor="middle" fontSize="14" fill="var(--text-secondary)">Total</text>
              <text x="100" y="114" textAnchor="middle" fontSize="16" fontWeight="700" fill="var(--text-primary)">
                {formatAmount(total)}
              </text>
            </svg>
            <div className="mini-legend">
              {breakdown.slice(0, 6).map((cat, idx) => (
                <div key={idx} className="mini-legend-item">
                  <span className="mini-legend-dot" style={{ background: CHART_COLORS[idx % CHART_COLORS.length] }} />
                  <span>{cat.category}</span>
                  <span style={{ fontWeight: 600, marginLeft: 'auto', color: 'var(--text-secondary)' }}>
                    {((cat.amount / total) * 100).toFixed(0)}%
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="dashboard-chart-card">
          <h3>Top Spending Categories</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {breakdown.slice(0, 5).map((cat, idx) => {
              const pct = (cat.amount / total) * 100
              return (
                <div key={idx}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '0.2rem' }}>
                    <span style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{cat.category}</span>
                    <span style={{ color: 'var(--text-secondary)' }}>{formatAmount(cat.amount)}</span>
                  </div>
                  <div style={{ height: '8px', background: 'var(--border-color)', borderRadius: '4px', overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: `${pct}%`, background: CHART_COLORS[idx % CHART_COLORS.length], borderRadius: '4px', transition: 'width 0.5s' }} />
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </div>
    )
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
        <>
          <div className="cards">
            {widgets.map((w, i) => renderWidget(w, i))}
          </div>
          {renderCharts()}
        </>
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
        <>
          <div className="cards">
            {widgets.map((w, i) => renderWidget(w, i))}
          </div>
          {renderCharts()}
        </>
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
            <>
              <div className="cards">
                {widgets.map((w, i) => renderWidget(w, i))}
              </div>
              {renderCharts()}
            </>
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
