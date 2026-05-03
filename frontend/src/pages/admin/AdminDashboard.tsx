import { Link } from 'react-router-dom'
import { Package, Plus, Users, Tags, ShoppingBag } from 'lucide-react'
import './AdminDashboard.css'

export default function AdminDashboard() {
  return (
    <div className="admin-dashboard">
      <h1 className="admin-dashboard__title">Admin Paneli</h1>
      <p className="admin-dashboard__subtitle">
        Hoş geldiniz. Sol menüden yönetmek istediğiniz bölümü seçin.
      </p>

      <div className="admin-dashboard__grid">
        <QuickCard
          icon={<Package size={22} color="#6366f1" />}
          title="Ürün Yönetimi"
          desc="Ürünleri listele, düzenle, sil"
          to="/admin/products"
          color="#eef2ff"
        />
        <QuickCard
          icon={<Plus size={22} color="#22c55e" />}
          title="Yeni Ürün Ekle"
          desc="Hızlıca yeni ürün oluştur"
          to="/admin/products/new"
          color="#f0fdf4"
        />
        <QuickCard
          icon={<Users size={22} color="#0ea5e9" />}
          title="Kullanıcıları Görüntüle"
          desc="Kullanıcıları görüntüle"
          to="/admin/users"
          color="#ecfeff"
        />
        <QuickCard
          icon={<Tags size={22} color="#f59e0b" />}
          title="Kategoriler"
          desc="Kategori yönetimi"
          to="/admin/categories"
          color="#fffbeb"
        />
        <QuickCard
          icon={<ShoppingBag size={22} color="#8b5cf6" />}
          title="Siparişler"
          desc="Siparişleri görüntüle"
          to="/admin/orders"
          color="#f5f3ff"
        />
      </div>
    </div>
  )
}

function QuickCard({ icon, title, desc, to, color }: {
  icon: React.ReactNode
  title: string
  desc: string
  to: string
  color: string
}) {
  return (
    <Link to={to} className="quick-card">
      <div className="quick-card__icon" style={{ background: color }}>
        {icon}
      </div>
      <div>
        <p className="quick-card__title">{title}</p>
        <p className="quick-card__desc">{desc}</p>
      </div>
    </Link>
  )
}
