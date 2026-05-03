import './admin-shared.css'
import './AdminCategoriesPage.css'
import Breadcrumb from '../../shared/ui/Breadcrumb'
import { useEffect, useMemo, useState } from 'react'
import { Edit3, Plus, Save, Trash2, X } from 'lucide-react'
import { adminService } from '../../services/adminService'
import type { CategoryResponse } from '../../services/productService'
import type { CategoryRequest } from '../../services/adminService'

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [name, setName] = useState('')
  const [saving, setSaving] = useState(false)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [editName, setEditName] = useState('')
  const [editParentId, setEditParentId] = useState<number | null>(null)
  const [updating, setUpdating] = useState(false)
  const [deletingId, setDeletingId] = useState<number | null>(null)

  const categoryMap = useMemo(() => {
    const map = new Map<number, CategoryResponse>()
    categories.forEach((c) => map.set(c.id, c))
    return map
  }, [categories])

  useEffect(() => {
    async function init() {
      setLoading(true)
      setError(null)
      try {
        const data = await adminService.getCategories()
        setCategories(data)
      } catch {
        setError('Kategoriler yüklenemedi.')
      } finally {
        setLoading(false)
      }
    }
    void init()
  }, [])

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim()) return
    setSaving(true)
    setError(null)
    try {
      const payload: CategoryRequest = { name: name.trim(), parentId: null }
      const created = await adminService.createCategory(payload)
      setCategories((prev) => [...prev, created])
      setName('')
    } catch {
      setError('Kategori oluşturulamadı.')
    } finally {
      setSaving(false)
    }
  }

  function startEdit(c: CategoryResponse) {
    setEditingId(c.id)
    setEditName(c.name)
    setEditParentId(c.parentId ?? null)
  }

  function cancelEdit() {
    setEditingId(null)
    setEditName('')
    setEditParentId(null)
  }

  async function handleUpdate() {
    if (!editingId || !editName.trim()) return
    setUpdating(true)
    setError(null)
    try {
      const payload: CategoryRequest = { name: editName.trim(), parentId: editParentId }
      const updated = await adminService.updateCategory(editingId, payload)
      setCategories((prev) => prev.map((c) => (c.id === editingId ? updated : c)))
      cancelEdit()
    } catch {
      setError('Kategori güncellenemedi.')
    } finally {
      setUpdating(false)
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Bu kategoriyi silmek istediğinize emin misiniz?')) return
    setDeletingId(id)
    setError(null)
    try {
      await adminService.deleteCategory(id)
      setCategories((prev) => prev.filter((c) => c.id !== id))
      if (editingId === id) cancelEdit()
    } catch {
      setError('Kategori silinemedi.')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className="admin-page">
      <Breadcrumb
        items={[
          { label: 'Admin', to: '/admin' },
          { label: 'Kategoriler' },
        ]}
        className="breadcrumb--mb-12"
      />
      <h1 className="admin-page__title">Kategoriler</h1>
      <p className="admin-page__subtitle">Kategori oluşturun, düzenleyin ve silin.</p>

      <form onSubmit={handleCreate} className="admin-categories__form">
        <input
          className="admin-input"
          placeholder="Kategori adı"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <button type="submit" disabled={saving} className="admin-btn admin-btn--primary">
          <Plus size={14} />{saving ? 'Kaydediliyor...' : 'Ekle'}
        </button>
      </form>

      {error && <div className="admin-alert-error">{error}</div>}

      <div className="admin-categories__table-wrapper">
        <div className="admin-categories__table-header">
          <div className="admin-categories__id">ID</div>
          <div className="admin-categories__name">Ad</div>
          <div className="admin-categories__parent">Üst Kategori</div>
          <div className="admin-categories__actions">İşlemler</div>
        </div>

        {loading ? (
          <div style={{ padding: '16px', color: '#64748b', fontSize: '0.9rem' }}>Kategoriler yükleniyor...</div>
        ) : categories.length === 0 ? (
          <div style={{ padding: '16px', color: '#94a3b8', fontSize: '0.9rem' }}>Kategori bulunamadı.</div>
        ) : (
          categories.map((c) => {
            const isEditing = editingId === c.id
            return (
              <div key={c.id} className="admin-categories__table-row">
                <div className="admin-categories__id">{c.id}</div>
                <div className="admin-categories__name">
                  {isEditing ? (
                    <input className="admin-input" value={editName} onChange={(e) => setEditName(e.target.value)} />
                  ) : (
                    <span>{c.name}</span>
                  )}
                </div>
                <div className="admin-categories__parent">
                  {isEditing ? (
                    <select
                      className="admin-input"
                      value={editParentId ?? ''}
                      onChange={(e) => setEditParentId(e.target.value ? Number(e.target.value) : null)}
                    >
                      <option value="">Yok</option>
                      {categories.filter((x) => x.id !== c.id).map((x) => <option key={x.id} value={x.id}>{x.name}</option>)}
                    </select>
                  ) : (
                    <span>
                      {c.parentId ? `${categoryMap.get(c.parentId)?.name ?? 'Bilinmiyor'} (#${c.parentId})` : '-'}
                    </span>
                  )}
                </div>
                <div className="admin-categories__actions">
                  {isEditing ? (
                    <>
                      <button type="button" onClick={handleUpdate} disabled={updating} className="admin-btn admin-btn--primary">
                        <Save size={14} />{updating ? 'Kaydediliyor...' : 'Kaydet'}
                      </button>
                      <button type="button" onClick={cancelEdit} className="admin-btn">
                        <X size={14} />İptal
                      </button>
                    </>
                  ) : (
                    <>
                      <button type="button" onClick={() => startEdit(c)} className="admin-btn">
                        <Edit3 size={14} />Düzenle
                      </button>
                      <button type="button" onClick={() => handleDelete(c.id)} disabled={deletingId === c.id} className="admin-btn admin-btn--danger">
                        <Trash2 size={14} />{deletingId === c.id ? 'Siliniyor...' : 'Sil'}
                      </button>
                    </>
                  )}
                </div>
              </div>
            )
          })
        )}
      </div>
    </div>
  )
}
