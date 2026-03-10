import { Outlet, NavLink } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useRef, useEffect, useState, useCallback } from 'react'
import PreferencesModal from './PreferencesModal'
import { getPreferences, savePreferences } from '../api/client'

const DEFAULT_NAV = [
  { id: 'dashboard', label: 'Dashboard', path: '/app/dashboard', icon: '📊', color: '#2563eb', visible: true },
  { id: 'expenses', label: 'Expenses', path: '/app/expenses', icon: '💰', color: '#059669', visible: true },
  { id: 'investments', label: 'Investments', path: '/app/investments', icon: '📈', color: '#7c3aed', visible: true },
  { id: 'salary', label: 'Salary', path: '/app/salary', icon: '💵', color: '#d97706', visible: true },
  { id: 'debts', label: 'Debts', path: '/app/debts', icon: '🏦', color: '#dc2626', visible: true },
  { id: 'analytics', label: 'Analytics', path: '/app/analytics', icon: '📉', color: '#0891b2', visible: true },
  { id: 'recurring', label: 'Recurring', path: '/app/recurring', icon: '🔄', color: '#6366f1', visible: true },
  { id: 'budgets', label: 'Budgets', path: '/app/budgets', icon: '🎯', color: '#ec4899', visible: true },
]




function NavConfigPanel({ navConfig, onUpdate, onClose }) {
  const [config, setConfig] = useState(navConfig)
  const [draggedId, setDraggedId] = useState(null)

  const handleDragStart = (e, id) => { setDraggedId(id); e.dataTransfer.effectAllowed = 'move' }
  const handleDragOver = (e) => { e.preventDefault(); e.dataTransfer.dropEffect = 'move' }
  const handleDrop = (e, targetId) => {
    e.preventDefault()
    if (!draggedId || draggedId === targetId) return
    const draggedIndex = config.findIndex(item => item.id === draggedId)
    const targetIndex = config.findIndex(item => item.id === targetId)
    const newConfig = [...config]
    const [draggedItem] = newConfig.splice(draggedIndex, 1)
    newConfig.splice(targetIndex, 0, draggedItem)
    setConfig(newConfig)
    setDraggedId(null)
  }

  return (
    <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex',
      alignItems: 'flex-start', justifyContent: 'center', zIndex: 999, paddingTop: '2rem' }}>
      <div style={{ background: 'var(--bg-secondary)', borderRadius: '0.75rem', width: '90%', maxWidth: '400px',
        maxHeight: '80vh', overflowY: 'auto', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)' }}>
        <div style={{ padding: '1.25rem 1.5rem', borderBottom: '1px solid var(--border-color)', display: 'flex',
          justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0, fontSize: '1.1rem', fontWeight: '600' }}>Configure Navigation</h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', fontSize: '1.25rem',
            cursor: 'pointer', color: 'var(--text-secondary)' }}>✕</button>
        </div>
        <div style={{ padding: '1rem 1.5rem' }}>
          {config.map(item => (
            <div key={item.id} draggable onDragStart={(e) => handleDragStart(e, item.id)}
              onDragOver={handleDragOver} onDrop={(e) => handleDrop(e, item.id)}
              style={{ padding: '0.65rem 0.75rem', marginBottom: '0.5rem', border: '1px solid var(--border-color)',
                borderRadius: '0.375rem', display: 'flex', alignItems: 'center', gap: '0.75rem',
                opacity: item.visible ? 1 : 0.5,
                cursor: draggedId === item.id ? 'grabbing' : 'grab',
                background: draggedId === item.id ? 'var(--border-color)' : 'var(--bg-secondary)' }}>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem', flexShrink: 0 }}>⋮⋮</span>
              <span style={{ flex: 1, fontSize: '0.85rem', fontWeight: 500, color: 'var(--text-primary)' }}>{item.label}</span>
              <button onClick={() => setConfig(c => c.map(i => i.id === item.id ? {...i, visible: !i.visible} : i))}
                style={{ background: item.visible ? 'var(--accent-color)' : 'var(--border-color)',
                  border: 'none', borderRadius: '10px', width: '36px', height: '20px', cursor: 'pointer',
                  position: 'relative', transition: 'background 0.2s', flexShrink: 0 }}>
                <span style={{ position: 'absolute', top: '2px',
                  left: item.visible ? '18px' : '2px',
                  width: '16px', height: '16px', borderRadius: '50%', background: '#fff',
                  transition: 'left 0.2s', boxShadow: '0 1px 3px rgba(0,0,0,0.2)' }} />
              </button>
            </div>
          ))}
        </div>
        <div style={{ padding: '1rem 1.5rem', borderTop: '1px solid var(--border-color)', display: 'flex',
          gap: '0.75rem', justifyContent: 'flex-end' }}>
          <button onClick={() => setConfig(DEFAULT_NAV)} className="btn btn-secondary">Reset</button>
          <button onClick={() => { onUpdate(config); onClose() }} className="btn btn-primary">Save</button>
        </div>
      </div>
    </div>
  )
}

// Format phone for display: +919876543210 -> +91 98765 43210
function formatPhone(phone) {
  if (!phone) return ''
  const digits = phone.replace(/\D/g, '')
  if (digits.length === 12 && digits.startsWith('91')) return `+91 ${digits.slice(2, 7)} ${digits.slice(7)}`
  if (digits.length === 10) return `+91 ${digits.slice(0, 5)} ${digits.slice(5)}`
  return phone
}

// Check if email is internal phone placeholder
function isPhoneAccountEmail(email) {
  return email && /^phone_\d+@phone\.local$/.test(email)
}

// ====== User Profile Dropdown (shared between topbar and sidebar mode) ======
function UserProfileDropdown({ user, isAdmin, memberSince, logout, onOpenPreferences, compact }) {
  const [showProfile, setShowProfile] = useState(false)
  const dropdownRef = useRef(null)

  const phone = user?.phoneNumber || user?.phone
  const email = user?.email
  const isPhoneUser = isPhoneAccountEmail(email)
  const displayEmail = isPhoneUser ? null : email
  const displayPhone = formatPhone(phone)
  const hasEmail = !!displayEmail
  const hasPhone = !!displayPhone

  useEffect(() => {
    function handleClickOutside(e) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) setShowProfile(false)
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  return (
    <div className="user user-profile-trigger" ref={dropdownRef}>
      <div onClick={() => setShowProfile(!showProfile)} className="user-trigger-inner">
        {user?.pictureUrl ? (
          <img src={user.pictureUrl} alt="" className="user-trigger-avatar" />
        ) : (
          <span className="user-trigger-avatar-fallback">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
          </span>
        )}
        {!compact && (
          <div className="user-trigger-info">
            {hasEmail && <span className="user-trigger-email">{displayEmail}</span>}
            {hasPhone && <span className="user-trigger-phone">{displayPhone}</span>}
            {!hasEmail && !hasPhone && <span className="user-trigger-label">{user?.name || 'Account'}</span>}
          </div>
        )}
      </div>
      {showProfile && (
        <div className="user-dropdown">
          <div className="user-dropdown-header">
            {user?.pictureUrl ? (
              <img src={user.pictureUrl} alt="" />
            ) : (
              <span className="user-dropdown-avatar-fallback">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
              </span>
            )}
            <div className="name">{user?.name || displayEmail || displayPhone || 'Account'}</div>
            <div className={`role-badge ${isAdmin ? 'admin' : 'user'}`} style={{ marginTop: '0.5rem' }}>
              {isAdmin ? 'Admin' : 'User'}
            </div>
          </div>
          <div className="user-dropdown-body">
            <div className="user-dropdown-row">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}>
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
              </svg>
              <span className="user-dropdown-value">{displayEmail || (isPhoneUser ? 'Phone account' : '—')}</span>
            </div>
            <div className="user-dropdown-row">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}>
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/>
              </svg>
              <span className="user-dropdown-value">{displayPhone || <a href="#" style={{ color: 'var(--accent-color)' }}>Add phone number</a>}</span>
            </div>
            <div className="user-dropdown-row">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ flexShrink: 0 }}>
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                <line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/>
                <line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              <span className="user-dropdown-value">Member since {memberSince}</span>
            </div>
          </div>
          <div className="user-dropdown-footer">
            <button type="button" className="btn btn-outline" style={{ width: '100%', marginBottom: '0.5rem' }}
              onClick={() => { setShowProfile(false); onOpenPreferences() }}>
              🎨 Preferences
            </button>
            <button type="button" className="btn btn-secondary" style={{ width: '100%', marginBottom: '0.5rem' }}>
              Edit Profile
            </button>
            <button type="button" className="btn btn-danger" style={{ width: '100%' }} onClick={logout}>
              Logout
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

// ====== Main Layout ======
export default function Layout() {
  const { user, logout } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  const [showNavConfig, setShowNavConfig] = useState(false)
  const [showPreferences, setShowPreferences] = useState(false)
  const [navConfig, setNavConfig] = useState([])

  // Layout & Theme preferences
  const [layoutMode, setLayoutMode] = useState(() => localStorage.getItem('layout-mode') || 'top')
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => localStorage.getItem('sidebar-collapsed') === 'true')
  const [appTheme, setAppTheme] = useState(() => localStorage.getItem('app-theme') || 'light')

  // Apply theme on mount and change
  useEffect(() => {
    if (appTheme === 'light') {
      document.documentElement.removeAttribute('data-theme')
    } else {
      document.documentElement.setAttribute('data-theme', appTheme)
    }
  }, [appTheme])

  // Load preferences from backend on mount, fallback to localStorage
  useEffect(() => {
    const stored = localStorage.getItem('nav-config')
    if (stored) {
      try { setNavConfig(JSON.parse(stored)) } catch { setNavConfig(DEFAULT_NAV) }
    } else {
      setNavConfig(DEFAULT_NAV)
    }

    // Sync preferences from backend
    getPreferences().then(prefs => {
      if (prefs && typeof prefs === 'object') {
        if (prefs.theme) { setAppTheme(prefs.theme); localStorage.setItem('app-theme', prefs.theme) }
        if (prefs.layout) { setLayoutMode(prefs.layout); localStorage.setItem('layout-mode', prefs.layout) }
        if (prefs.sidebarCollapsed !== undefined) {
          const collapsed = prefs.sidebarCollapsed === 'true'
          setSidebarCollapsed(collapsed)
          localStorage.setItem('sidebar-collapsed', String(collapsed))
        }
        if (prefs.navConfig) {
          try {
            const nc = JSON.parse(prefs.navConfig)
            setNavConfig(nc)
            localStorage.setItem('nav-config', JSON.stringify(nc))
          } catch {}
        }
      }
    }).catch(() => { /* fallback to localStorage values already set */ })
  }, [])

  // Helper to persist preferences both locally and to backend
  const persistPrefs = useCallback((key, value) => {
    savePreferences({ [key]: value }).catch(() => {})
  }, [])

  const handleNavConfigUpdate = (newConfig) => {
    setNavConfig(newConfig)
    localStorage.setItem('nav-config', JSON.stringify(newConfig))
    persistPrefs('navConfig', JSON.stringify(newConfig))
  }

  const handleLayoutChange = (mode) => {
    setLayoutMode(mode)
    localStorage.setItem('layout-mode', mode)
    persistPrefs('layout', mode)
  }

  const handleSidebarCollapsedChange = (collapsed) => {
    setSidebarCollapsed(collapsed)
    localStorage.setItem('sidebar-collapsed', String(collapsed))
    persistPrefs('sidebarCollapsed', String(collapsed))
  }

  const handleThemeChange = (theme) => {
    setAppTheme(theme)
    localStorage.setItem('app-theme', theme)
    persistPrefs('theme', theme)
  }

  const memberSince = user?.createdAt
    ? new Date(user.createdAt).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' })
    : 'N/A'

  const visibleNavItems = navConfig.filter(item => item.visible)

  const [sidebarWidth, setSidebarWidth] = useState(() => {
    const saved = localStorage.getItem('sidebar-width')
    const w = saved ? parseInt(saved, 10) : 220
    return Math.max(w, 180)
  })
  const isResizing = useRef(false)

  const handleMouseDown = useCallback((e) => {
    e.preventDefault()
    isResizing.current = true
    document.body.style.cursor = 'col-resize'
    document.body.style.userSelect = 'none'

    const handleMouseMove = (e) => {
      if (!isResizing.current) return
      const newWidth = Math.min(Math.max(e.clientX, 180), 400)
      setSidebarWidth(newWidth)
    }

    const handleMouseUp = () => {
      isResizing.current = false
      document.body.style.cursor = ''
      document.body.style.userSelect = ''
      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
      setSidebarWidth(w => { localStorage.setItem('sidebar-width', String(w)); return w })
    }

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
  }, [])

  // ===== SIDEBAR MODE =====
  if (layoutMode === 'sidebar') {
    return (
      <div className="app-wrapper sidebar-mode">
        <aside className="sidebar" style={{ width: sidebarWidth }}>
          <div className="sidebar-header">
            <div className="sidebar-brand">
              <span className="sidebar-brand-icon">₹</span>
              <span className="sidebar-brand-text">Kanaku Book</span>
            </div>
          </div>

          <nav className="sidebar-nav">
            {visibleNavItems.map(item => (
              <NavLink key={item.id} to={item.path}
                className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}>
                <span className="sidebar-nav-label">{item.label}</span>
              </NavLink>
            ))}
            {isAdmin && (
              <NavLink to="/app/admin"
                className={({ isActive }) => `sidebar-nav-item ${isActive ? 'active' : ''}`}>
                <span className="sidebar-nav-label">Admin</span>
              </NavLink>
            )}
          </nav>

          <div className="sidebar-footer">
            <button className="sidebar-collapse-btn" style={{ width: '100%', textAlign: 'left', display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}
              onClick={() => setShowNavConfig(true)} title="Configure Navigation">
              <span className="sidebar-nav-label">Customize</span>
            </button>
            <UserProfileDropdown user={user} isAdmin={isAdmin} memberSince={memberSince}
              logout={logout} onOpenPreferences={() => setShowPreferences(true)} compact={false} />
          </div>

          <div className="sidebar-resize-handle" onMouseDown={handleMouseDown} />
        </aside>

        <div className="sidebar-main-content" style={{ marginLeft: sidebarWidth }}>
          <main><Outlet /></main>
        </div>

        {showNavConfig && <NavConfigPanel navConfig={navConfig} onUpdate={handleNavConfigUpdate}
          onClose={() => setShowNavConfig(false)} />}
        <PreferencesModal isOpen={showPreferences} onClose={() => setShowPreferences(false)}
          layoutMode={layoutMode} onLayoutChange={handleLayoutChange}
          sidebarCollapsed={sidebarCollapsed} onSidebarCollapsedChange={handleSidebarCollapsedChange}
          appTheme={appTheme} onThemeChange={handleThemeChange} />
      </div>
    )
  }

  // ===== TOP BAR MODE (default) =====
  return (
    <div className="app">
      <nav className="topbar">
        <div className="topbar-left">
          <NavLink to="/app/dashboard" className="topbar-brand" title="Kanaku Book">
            <span className="topbar-brand-icon">₹</span>
          </NavLink>
          {navConfig.map(item => (
            <NavLink key={item.id} to={item.path} className="topbar-nav-item">
              {item.label}
            </NavLink>
          ))}
          {isAdmin && (
            <NavLink to="/app/admin" className="topbar-nav-item">Admin</NavLink>
          )}
        </div>
        <div className="topbar-right">
          <UserProfileDropdown user={user} isAdmin={isAdmin} memberSince={memberSince}
            logout={logout} onOpenPreferences={() => setShowPreferences(true)} />
        </div>
      </nav>

      {showNavConfig && <NavConfigPanel navConfig={navConfig} onUpdate={handleNavConfigUpdate}
        onClose={() => setShowNavConfig(false)} />}
      <PreferencesModal isOpen={showPreferences} onClose={() => setShowPreferences(false)}
        layoutMode={layoutMode} onLayoutChange={handleLayoutChange}
        sidebarCollapsed={sidebarCollapsed} onSidebarCollapsedChange={handleSidebarCollapsedChange}
        appTheme={appTheme} onThemeChange={handleThemeChange} />

      <main><Outlet /></main>
    </div>
  )
}
