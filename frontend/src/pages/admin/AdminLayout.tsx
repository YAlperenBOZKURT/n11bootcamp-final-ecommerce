import { Navigate, Outlet, NavLink, useNavigate } from 'react-router-dom'
import { LayoutDashboard, Package, LogOut, ChevronRight, ShieldCheck, Users, Tags, ShoppingBag } from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import './AdminLayout.css'

const navItems = [
  { to: '/admin',          label: 'Dashboard',  icon: <LayoutDashboard size={17} />, end: true },
  { to: '/admin/products', label: 'Ürünler',    icon: <Package size={17} /> },
  { to: '/admin/users',    label: 'Kullanıcılar',      icon: <Users size={17} /> },
  { to: '/admin/categories', label: 'Kategoriler', icon: <Tags size={17} /> },
  { to: '/admin/orders',   label: 'Siparişler',     icon: <ShoppingBag size={17} /> },
]

export default function AdminLayout() {
  const { user, logout, status } = useAuth()
  const navigate = useNavigate()

  if (status === 'loading') {
    return (
      <div className="admin-loading">
        <div className="admin-spinner" />
      </div>
    )
  }

  if (!user || user.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }

  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <div className="admin-sidebar__brand">
          <div className="admin-sidebar__logo">
            <ShieldCheck size={16} color="#fff" />
          </div>
          <div>
            <p style={{ margin: 0, fontSize: '0.82rem', fontWeight: 700, color: '#1e293b' }}>Admin Panel</p>
            <p style={{ margin: 0, fontSize: '0.72rem', color: '#94a3b8' }}>{user.email}</p>
          </div>
        </div>
        <nav style={{ flex: 1, padding: '12px 10px' }}>
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `admin-nav-link${isActive ? ' admin-nav-link--active' : ''}`}
            >
              {({ isActive }) => (
                <span className="admin-nav-link__icon-label">
                  {item.icon}
                  {item.label}
                  {isActive && <ChevronRight size={14} style={{ opacity: 0.5, marginLeft: 'auto' }} />}
                </span>
              )}
            </NavLink>
          ))}
        </nav>
        <div style={{ padding: '12px 10px', borderTop: '1px solid #f1f5f9' }}>
          <NavLink
            to="/products"
            className="admin-nav-link"
          >
            <span className="admin-nav-link__icon-label">← Siteye Dön</span>
          </NavLink>
          <button
            onClick={async () => { await logout(); navigate('/login') }}
            className="admin-sidebar__logout-btn"
          >
            <LogOut size={17} />
            Çıkış Yap
          </button>
        </div>
      </aside>
      <main className="admin-content">
        <Outlet />
      </main>
    </div>
  )
}

