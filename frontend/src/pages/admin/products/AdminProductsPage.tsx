import { useEffect, useState, useCallback } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Plus, Search, Edit2, Trash2, X, RefreshCw } from 'lucide-react'
import { adminService } from '../../../services/adminService'
import type { AdminSearchParams } from '../../../services/adminService'
import type { ProductResponse, CategoryResponse } from '../../../services/productService'
import './AdminProductsPage.css'

const PAGE_SIZE = 20

const STATUS_OPTIONS = [
  { value: '', label: 'Tüm Durumlar' },
  { value: 'ACTIVE', label: 'Aktif' },
  { value: 'PASSIVE', label: 'Pasif' },
]

export default function AdminProductsPage() {
  const navigate = useNavigate()

  const [products, setProducts] = useState<ProductResponse[]>([])
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [currentPage, setCurrentPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [deleting, setDeleting] = useState<number | null>(null)

  const [keyword, setKeyword] = useState('')
  const [inputValue, setInputValue] = useState('')
  const [selectedCategory, setSelectedCategory] = useState<number | undefined>()
  const [selectedStatus, setSelectedStatus] = useState('')

  useEffect(() => {
    adminService.getCategories().then(setCategories).catch(() => {})
  }, [])

  const fetchProducts = useCallback(() => {
    setLoading(true)
    const params: AdminSearchParams = {
      page: currentPage,
      size: PAGE_SIZE,
      sort: 'createdAt,desc',
    }
    if (keyword) params.keyword = keyword
    if (selectedCategory) params.categoryId = selectedCategory
    if (selectedStatus) params.status = selectedStatus

    adminService.searchProducts(params)
      .then((page) => {
        setProducts(page.content)
        setTotalElements(page.totalElements)
        setTotalPages(page.totalPages)
      })
      .catch(() => setProducts([]))
      .finally(() => setLoading(false))
  }, [keyword, selectedCategory, selectedStatus, currentPage])

  useEffect(() => {
    fetchProducts()
  }, [fetchProducts])

  const handleSearch = () => {
    setKeyword(inputValue.trim())
    setCurrentPage(0)
  }

  const handleClearFilters = () => {
    setInputValue('')
    setKeyword('')
    setSelectedCategory(undefined)
    setSelectedStatus('')
    setCurrentPage(0)
  }

  const handleDelete = async (id: number, name: string) => {
    if (!confirm(`"${name}" ürününü silmek istediğinize emin misiniz?`)) return
    setDeleting(id)
    try {
      await adminService.deleteProduct(id)
      fetchProducts()
    } catch {
      alert('Ürün silinemedi.')
    } finally {
      setDeleting(null)
    }
  }

  const hasFilters = keyword || selectedCategory || selectedStatus

  return (
    <div className="admin-products-page">
      <div className="admin-products-header">
        <div>
          <h1 className="admin-products-title">Ürün Yönetimi</h1>
          {!loading && <p className="admin-products-count">{totalElements} ürün bulundu</p>}
        </div>
        <Link to="/admin/products/new" className="admin-products-new-link">
          <Plus size={16} /> Yeni Ürün
        </Link>
      </div>

      <div className="admin-products-filter-card">
        <div className="admin-products-filter-col--search">
          <label className="admin-products-label">Ürün Ara</label>
          <div className="admin-products-search-wrap">
            <Search size={14} className="admin-products-search-icon" />
            <input
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              placeholder="İsim veya marka..."
              className="admin-products-input"
            />
          </div>
        </div>

        <div className="admin-products-filter-col--category">
          <label className="admin-products-label">Kategori</label>
          <select
            value={selectedCategory ?? ''}
            onChange={(e) => {
              setSelectedCategory(e.target.value ? Number(e.target.value) : undefined)
              setCurrentPage(0)
            }}
            className="admin-products-select"
          >
            <option value="">Tüm Kategoriler</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </div>

        <div className="admin-products-filter-col--status">
          <label className="admin-products-label">Durum</label>
          <select
            value={selectedStatus}
            onChange={(e) => {
              setSelectedStatus(e.target.value)
              setCurrentPage(0)
            }}
            className="admin-products-select"
          >
            {STATUS_OPTIONS.map((o) => (
              <option key={o.value} value={o.value}>{o.label}</option>
            ))}
          </select>
        </div>

        <div className="admin-products-filter-actions">
          <button onClick={handleSearch} className="admin-products-btn admin-products-btn--search">Ara</button>
          {hasFilters && (
            <button onClick={handleClearFilters} className="admin-products-btn admin-products-btn--clear">
              <X size={13} /> Temizle
            </button>
          )}
          <button onClick={fetchProducts} className="admin-products-btn admin-products-btn--refresh" title="Yenile">
            <RefreshCw size={14} />
          </button>
        </div>
      </div>

      <div className="admin-products-table-card">
        {loading ? (
          <div className="admin-products-loading">Yükleniyor...</div>
        ) : products.length === 0 ? (
          <div className="admin-products-empty">
            <p className="admin-products-empty-text">Ürün bulunamadı.</p>
          </div>
        ) : (
          <table className="admin-products-table">
            <thead>
              <tr className="admin-products-thead-row">
                {['ID', 'Ürün', 'Kategori', 'Fiyat', 'Durum', 'İşlemler'].map((h) => (
                  <th key={h} className="admin-products-th">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id} className="admin-products-row">
                  <td className="admin-products-td admin-products-td--id">#{p.id}</td>
                  <td className="admin-products-td admin-products-td--product">
                    <div className="admin-products-product-wrap">
                      {p.imageUrl ? (
                        <img src={p.imageUrl} alt={p.name} className="admin-products-thumb" />
                      ) : (
                        <div className="admin-products-thumb-placeholder" />
                      )}
                      <div className="admin-products-product-meta">
                        <p className="admin-products-product-name">{p.name}</p>
                        <p className="admin-products-product-brand">{p.brand}</p>
                      </div>
                    </div>
                  </td>
                  <td className="admin-products-td admin-products-td--category">{p.category?.name ?? '—'}</td>
                  <td className="admin-products-td admin-products-td--price">{p.priceFrom != null ? `₺${p.priceFrom.toLocaleString('tr-TR')}` : '—'}</td>
                  <td className="admin-products-td"><StatusBadge status={p.status} /></td>
                  <td className="admin-products-td">
                    <div className="admin-products-actions">
                      <button
                        onClick={() => navigate(`/admin/products/${p.id}`)}
                        className="admin-products-action-btn admin-products-action-btn--edit"
                      >
                        <Edit2 size={12} /> Düzenle
                      </button>
                      <button
                        onClick={() => handleDelete(p.id, p.name)}
                        disabled={deleting === p.id}
                        className={`admin-products-action-btn admin-products-action-btn--delete${deleting === p.id ? ' admin-products-action-btn--disabled' : ''}`}
                      >
                        <Trash2 size={12} /> Sil
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {totalPages > 1 && (
        <div className="admin-products-pagination">
          <PageBtn label="‹" disabled={currentPage === 0} onClick={() => setCurrentPage((p) => p - 1)} />
          {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
            const page = totalPages <= 7 ? i : Math.max(0, Math.min(currentPage - 3 + i, totalPages - 1))
            return <PageBtn key={page} label={String(page + 1)} active={page === currentPage} onClick={() => setCurrentPage(page)} />
          })}
          <PageBtn label="›" disabled={currentPage === totalPages - 1} onClick={() => setCurrentPage((p) => p + 1)} />
        </div>
      )}
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  const map: Record<string, { label: string; tone: 'active' | 'passive' | 'deleted' | 'default' }> = {
    ACTIVE: { label: 'Aktif', tone: 'active' },
    PASSIVE: { label: 'Pasif', tone: 'passive' },
    DELETED: { label: 'Silindi', tone: 'deleted' },
  }
  const s = map[status] ?? { label: status, tone: 'default' as const }
  return <span className={`admin-products-status-badge admin-products-status-badge--${s.tone}`}>{s.label}</span>
}

function PageBtn({ label, disabled, active, onClick }: { label: string; disabled?: boolean; active?: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`admin-products-page-btn${active ? ' admin-products-page-btn--active' : disabled ? ' admin-products-page-btn--disabled' : ''}`}
    >
      {label}
    </button>
  )
}
