import { useState, useEffect } from 'react'
import { getAdminUsers, getAdminLogins } from '../api/client'

export default function Admin() {
  const [tab, setTab] = useState('users')
  const [users, setUsers] = useState([])
  const [logins, setLogins] = useState({ items: [] })
  const [loading, setLoading] = useState(false)
  const [err, setErr] = useState('')

  useEffect(() => {
    if (tab === 'users') {
      setLoading(true)
      getAdminUsers().then(data => {
        setUsers(Array.isArray(data) ? data : [])
        setLoading(false)
      }).catch(() => {
        setErr('Failed to load users')
        setLoading(false)
      })
    } else {
      setLoading(true)
      getAdminLogins(50, 0).then(data => {
        setLogins(data && data.items ? data : { items: [] })
        setLoading(false)
      }).catch(() => {
        setErr('Failed to load logins')
        setLoading(false)
      })
    }
  }, [tab])

  return (
    <>
      <h2>Admin</h2>
      <div className="tabs">
        <button type="button" className={tab === 'users' ? 'active' : ''} onClick={() => setTab('users')}>Users</button>
        <button type="button" className={tab === 'logins' ? 'active' : ''} onClick={() => setTab('logins')}>Login history</button>
      </div>
      {err && <p className="error">{err}</p>}
      {loading && <p>Loading...</p>}
      {!loading && tab === 'users' && (
        <div className="table-wrap">
          {users.length === 0 ? (
            <p className="empty">No users</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Email</th>
                  <th>Name</th>
                  <th>Role</th>
                  <th>Created at</th>
                </tr>
              </thead>
              <tbody>
                {users.map(u => (
                  <tr key={u.id}>
                    <td>{u.email}</td>
                    <td>{u.name || '—'}</td>
                    <td>{u.role}</td>
                    <td>{u.createdAt ? new Date(u.createdAt).toLocaleString() : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
      {!loading && tab === 'logins' && (
        <div className="table-wrap">
          {logins.items.length === 0 ? (
            <p className="empty">No logins</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>User (email)</th>
                  <th>Login time</th>
                  <th>IP</th>
                </tr>
              </thead>
              <tbody>
                {logins.items.map(e => (
                  <tr key={e.id}>
                    <td>{e.userEmail}</td>
                    <td>{e.loggedInAt ? new Date(e.loggedInAt).toLocaleString() : '—'}</td>
                    <td>{e.ipAddress || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </>
  )
}
