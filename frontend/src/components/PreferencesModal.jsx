import { useState } from 'react'

const THEMES = [
  { id: 'light', name: 'Light', topbar: 'linear-gradient(135deg, #1a1a2e, #16213e)', bg: '#f0f2f5', accent: '#2563eb', text: '#1a1a2e' },
  { id: 'dark', name: 'Dark', topbar: 'linear-gradient(135deg, #0f0f1e, #1a1a2e)', bg: '#111827', accent: '#60a5fa', text: '#f0f2f5' },
  { id: 'blue', name: 'Ocean Blue', topbar: 'linear-gradient(135deg, #1e40af, #1e3a8a)', bg: '#eff6ff', accent: '#3b82f6', text: '#1e3a8a' },
  { id: 'green', name: 'Forest', topbar: 'linear-gradient(135deg, #047857, #065f46)', bg: '#ecfdf5', accent: '#10b981', text: '#065f46' },
  { id: 'purple', name: 'Royal', topbar: 'linear-gradient(135deg, #7c3aed, #581c87)', bg: '#faf5ff', accent: '#a855f7', text: '#581c87' },
  { id: 'sunset', name: 'Sunset', topbar: 'linear-gradient(135deg, #ea580c, #c2410c)', bg: '#fff7ed', accent: '#f97316', text: '#7c2d12' },
]

export default function PreferencesModal({ isOpen, onClose, layoutMode, onLayoutChange, sidebarCollapsed, onSidebarCollapsedChange, appTheme, onThemeChange }) {
  if (!isOpen) return null

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      zIndex: 1000,
    }} onClick={onClose}>
      <div style={{
        background: 'var(--bg-secondary)', borderRadius: '16px',
        width: '90%', maxWidth: '520px', maxHeight: '85vh', overflowY: 'auto',
        boxShadow: '0 20px 60px rgba(0,0,0,0.2)',
        color: 'var(--text-primary)',
      }} onClick={e => e.stopPropagation()}>

        {/* Header */}
        <div style={{
          padding: '1.25rem 1.5rem', borderBottom: '1px solid var(--border-color)',
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        }}>
          <h3 style={{ margin: 0, fontSize: '1.15rem', fontWeight: 600 }}>Preferences</h3>
          <button onClick={onClose} style={{
            background: 'none', border: 'none', fontSize: '1.5rem',
            cursor: 'pointer', color: 'var(--text-secondary)',
          }}>✕</button>
        </div>

        {/* Layout Section */}
        <div style={{ padding: '1.25rem 1.5rem', borderBottom: '1px solid var(--border-color)' }}>
          <div style={{ fontSize: '0.8rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
            Navigation Layout
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            {/* Top Bar option */}
            <div
              onClick={() => onLayoutChange('top')}
              style={{
                border: `2px solid ${layoutMode === 'top' ? 'var(--accent-color)' : 'var(--border-color)'}`,
                borderRadius: '12px', padding: '1rem', cursor: 'pointer',
                background: layoutMode === 'top' ? 'rgba(37,99,235,0.05)' : 'transparent',
                transition: 'all 0.2s ease',
              }}
            >
              {/* Mini mockup - top bar */}
              <div style={{ marginBottom: '0.75rem' }}>
                <div style={{ height: '8px', background: 'var(--accent-color)', borderRadius: '2px', marginBottom: '4px' }} />
                <div style={{ display: 'flex', gap: '4px' }}>
                  <div style={{ flex: 1, height: '40px', background: 'var(--border-color)', borderRadius: '3px' }} />
                </div>
              </div>
              <div style={{ fontWeight: 600, fontSize: '0.88rem' }}>Top Bar</div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Horizontal navigation</div>
              {layoutMode === 'top' && <div style={{ marginTop: '0.4rem', fontSize: '0.7rem', color: 'var(--accent-color)', fontWeight: 600 }}>✓ Active</div>}
            </div>

            {/* Sidebar option */}
            <div
              onClick={() => onLayoutChange('sidebar')}
              style={{
                border: `2px solid ${layoutMode === 'sidebar' ? 'var(--accent-color)' : 'var(--border-color)'}`,
                borderRadius: '12px', padding: '1rem', cursor: 'pointer',
                background: layoutMode === 'sidebar' ? 'rgba(37,99,235,0.05)' : 'transparent',
                transition: 'all 0.2s ease',
              }}
            >
              {/* Mini mockup - sidebar */}
              <div style={{ marginBottom: '0.75rem', display: 'flex', gap: '4px' }}>
                <div style={{ width: '16px', background: 'var(--accent-color)', borderRadius: '3px', height: '48px' }} />
                <div style={{ flex: 1, height: '48px', background: 'var(--border-color)', borderRadius: '3px' }} />
              </div>
              <div style={{ fontWeight: 600, fontSize: '0.88rem' }}>Sidebar</div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Vertical navigation</div>
              {layoutMode === 'sidebar' && <div style={{ marginTop: '0.4rem', fontSize: '0.7rem', color: 'var(--accent-color)', fontWeight: 600 }}>✓ Active</div>}
            </div>
          </div>

          {/* Collapse toggle - only show if sidebar is selected */}
          {layoutMode === 'sidebar' && (
            <label style={{
              display: 'flex', alignItems: 'center', gap: '0.5rem',
              marginTop: '0.75rem', fontSize: '0.85rem', cursor: 'pointer',
              color: 'var(--text-secondary)',
            }}>
              <input
                type="checkbox"
                checked={sidebarCollapsed}
                onChange={e => onSidebarCollapsedChange(e.target.checked)}
                style={{ accentColor: 'var(--accent-color)' }}
              />
              Start collapsed (icon-only mode)
            </label>
          )}
        </div>

        {/* Theme Section */}
        <div style={{ padding: '1.25rem 1.5rem' }}>
          <div style={{ fontSize: '0.8rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
            Color Theme
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.6rem' }}>
            {THEMES.map(theme => (
              <div
                key={theme.id}
                onClick={() => onThemeChange(theme.id)}
                style={{
                  border: `2px solid ${appTheme === theme.id ? theme.accent : 'var(--border-color)'}`,
                  borderRadius: '10px',
                  padding: '0.6rem',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  background: appTheme === theme.id ? `${theme.accent}10` : 'transparent',
                }}
              >
                {/* Color preview bar */}
                <div style={{
                  height: '24px',
                  background: theme.topbar,
                  borderRadius: '4px',
                  marginBottom: '0.4rem',
                }} />
                {/* Mini body preview */}
                <div style={{
                  height: '12px',
                  background: theme.bg,
                  borderRadius: '2px',
                  border: '1px solid rgba(0,0,0,0.06)',
                  marginBottom: '0.4rem',
                }} />
                <div style={{
                  fontSize: '0.78rem', fontWeight: 600,
                  color: theme.text,
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                }}>
                  <span>{theme.name}</span>
                  {appTheme === theme.id && <span style={{ color: theme.accent }}>✓</span>}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
