import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import {
  Plus, Trash2, ArrowLeft, Save, Package, TrendingUp,
  AlertCircle, Upload, X, Image, TrendingDown, Pencil,
} from 'lucide-react'
import { adminService } from '../../../services/adminService'
import type {
  AdminProductRequest, StockResponse,
  VariantResponse, VariantRequest,
} from '../../../services/adminService'
import type { CategoryResponse } from '../../../services/productService'
import VariantModal from './components/VariantModal'
import type { AttrEntry, VariantFormState } from './components/VariantFormPanel'
import ProductFormSection from './components/ProductFormSection'
import Breadcrumb from '../../../shared/ui/Breadcrumb'
import './AdminProductFormPage.css'

const EMPTY_VARIANT_FORM = (): VariantFormState => ({
  sku: '',
  attrs: [{ key: '', value: '' }],
  price: '',
  discountRate: '',
})

const EMPTY_FORM = (): AdminProductRequest => ({
  name: '',
  description: '',
  brand: '',
  categoryId: 0,
  imageUrls: [],
})

function fromAttrEntries(entries: AttrEntry[]): Record<string, string> {
  const result: Record<string, string> = {}
  entries.forEach(({ key, value }) => {
    if (key.trim()) result[key.trim()] = value.trim()
  })
  return result
}

function variantFormToRequest(f: VariantFormState): VariantRequest {
  return {
    sku: f.sku.trim(),
    attributes: fromAttrEntries(f.attrs),
    price: parseFloat(f.price),
    discountRate: f.discountRate ? parseFloat(f.discountRate) : null,
  }
}

function variantToFormState(v: VariantResponse): VariantFormState {
  const attrs = Object.entries(v.attributes ?? {}).map(([key, value]) => ({ key, value }))
  return {
    sku: v.sku ?? '',
    attrs: attrs.length > 0 ? attrs : [{ key: '', value: '' }],
    price: String(v.price ?? ''),
    discountRate: v.discountRate == null ? '' : String(v.discountRate),
  }
}

function validateVariantForm(f: VariantFormState): string | null {
  if (!f.sku.trim()) return 'SKU zorunludur.'
  const price = parseFloat(f.price)
  if (!price || price <= 0) return 'Fiyat 0\'dan büyük olmalıdır.'
  if (f.discountRate) {
    const dr = parseFloat(f.discountRate)
    if (dr < 0 || dr > 100) return 'İndirim oranı 0-100 arasında olmalıdır.'
  }
  const valid = f.attrs.filter((a) => a.key.trim() && a.value.trim())
  if (valid.length === 0) return 'En az 1 özellik (ör: Beden = M) girilmelidir.'
  if (f.attrs.some((a) => !a.key.trim() || !a.value.trim())) return 'Tüm özellik alanları doldurulmalıdır.'
  return null
}

export default function AdminProductFormPage() {
  const { id } = useParams<{ id: string }>()
  const isEdit = Boolean(id)
  const productId = id ? Number(id) : null
  const navigate = useNavigate()
  const [form, setForm] = useState<AdminProductRequest>(EMPTY_FORM())
  const [categories, setCategories] = useState<CategoryResponse[]>([])
  const [loadingProduct, setLoadingProduct] = useState(isEdit)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [existingImageUrls, setExistingImageUrls] = useState<string[]>([])
  const [pendingFiles, setPendingFiles] = useState<File[]>([])
  const [uploadError, setUploadError] = useState<string | null>(null)
  const imageInputRef = useRef<HTMLInputElement>(null)
  const [variants, setVariants] = useState<VariantResponse[]>([])
  const [variantStocks, setVariantStocks] = useState<Record<number, StockResponse>>({})
  const [draftVariants, setDraftVariants] = useState<VariantFormState[]>([])
  const [showAddVariant, setShowAddVariant] = useState(false)
  const [newVariantForm, setNewVariantForm] = useState(EMPTY_VARIANT_FORM())
  const [addingVariant, setAddingVariant] = useState(false)
  const [variantError, setVariantError] = useState<string | null>(null)
  const [editingVariantId, setEditingVariantId] = useState<number | null>(null)
  const [editVariantForm, setEditVariantForm] = useState<VariantFormState>(EMPTY_VARIANT_FORM())
  const [editingVariantLoading, setEditingVariantLoading] = useState(false)
  const [editVariantError, setEditVariantError] = useState<string | null>(null)
  const [variantStockTarget, setVariantStockTarget] = useState<number | null>(null)
  const [variantStockAction, setVariantStockAction] = useState<'add' | 'reduce'>('add')
  const [variantStockQty, setVariantStockQty] = useState('')
  const [variantStockNote, setVariantStockNote] = useState('')
  const [variantStockLoading, setVariantStockLoading] = useState(false)
  const [variantStockMsg, setVariantStockMsg] = useState<{ id: number; msg: string; ok: boolean } | null>(null)
  useEffect(() => {
    adminService.getCategories().then(setCategories).catch(() => {})
  }, [])

  useEffect(() => {
    if (!isEdit || !productId) return
    Promise.all([
      adminService.getProduct(productId),
      adminService.getVariants(productId).catch(() => [] as VariantResponse[]),
      adminService.getAllVariantStocks(productId).catch(() => [] as StockResponse[]),
    ]).then(([product, variantData, variantStockData]) => {
      setForm({ name: product.name, description: product.description, brand: product.brand, categoryId: product.category?.id ?? 0, imageUrls: product.imageUrls ?? [] })
      setExistingImageUrls(product.imageUrls ?? [])
      setVariants(variantData)
      const stockMap: Record<number, StockResponse> = {}
      variantStockData.forEach((s) => { if (s.variantId) stockMap[s.variantId] = s })
      setVariantStocks(stockMap)
    }).catch(() => navigate('/admin/products'))
      .finally(() => setLoadingProduct(false))
  }, [isEdit, productId, navigate])
  const totalImages = existingImageUrls.length + pendingFiles.length
  const canAddMore = totalImages < 3

  function handleImageSelect(e: React.ChangeEvent<HTMLInputElement>) {
    setUploadError(null)
    const files = Array.from(e.target.files ?? [])
    const remaining = 3 - totalImages
    if (files.length > remaining) { setUploadError(`En fazla ${remaining} resim daha ekleyebilirsiniz.`); return }
    setPendingFiles((prev) => [...prev, ...files.slice(0, remaining)])
    e.target.value = ''
  }
  function handleVariantAttrChange(i: number, field: 'key' | 'value', val: string) {
    const updated = [...newVariantForm.attrs]
    updated[i] = { ...updated[i], [field]: val }
    setNewVariantForm({ ...newVariantForm, attrs: updated })
  }

  async function handleAddVariant() {
    const err = validateVariantForm(newVariantForm)
    if (err) { setVariantError(err); return }
    setVariantError(null)

    if (isEdit && productId) {
      // Edit mode: save to backend immediately
      setAddingVariant(true)
      try {
        const created = await adminService.createVariant(productId, variantFormToRequest(newVariantForm))
        setVariants((prev) => [...prev, created])
        setNewVariantForm(EMPTY_VARIANT_FORM())
        setShowAddVariant(false)
      } catch {
        setVariantError('Varyant eklenirken hata oluştu.')
      } finally {
        setAddingVariant(false)
      }
    } else {
      // Create mode: stage locally, saved after product is created
      setDraftVariants((prev) => [...prev, { ...newVariantForm }])
      setNewVariantForm(EMPTY_VARIANT_FORM())
      setShowAddVariant(false)
    }
  }

  async function handleDeleteVariant(variantId: number) {
    if (!productId || !confirm('Bu varyantı silmek istediğinize emin misiniz?')) return
    try {
      await adminService.deleteVariant(productId, variantId)
      setVariants((prev) => prev.filter((v) => v.id !== variantId))
      setVariantStocks((prev) => { const n = { ...prev }; delete n[variantId]; return n })
    } catch {
      alert('Varyant silinirken hata oluştu.')
    }
  }

  function openEditVariant(v: VariantResponse) {
    setEditVariantError(null)
    setEditingVariantId(v.id)
    setEditVariantForm(variantToFormState(v))
  }

  function closeEditVariant() {
    setEditingVariantId(null)
    setEditVariantError(null)
    setEditVariantForm(EMPTY_VARIANT_FORM())
  }

  function handleEditVariantAttrChange(i: number, field: 'key' | 'value', val: string) {
    const updated = [...editVariantForm.attrs]
    updated[i] = { ...updated[i], [field]: val }
    setEditVariantForm({ ...editVariantForm, attrs: updated })
  }

  async function handleUpdateVariant() {
    if (!productId || !editingVariantId) return
    const err = validateVariantForm(editVariantForm)
    if (err) { setEditVariantError(err); return }

    setEditVariantError(null)
    setEditingVariantLoading(true)
    try {
      const updated = await adminService.updateVariant(productId, editingVariantId, variantFormToRequest(editVariantForm))
      setVariants((prev) => prev.map((v) => v.id === editingVariantId ? updated : v))
      closeEditVariant()
    } catch {
      setEditVariantError('Varyant güncellenirken hata oluştu.')
    } finally {
      setEditingVariantLoading(false)
    }
  }

  async function handleVariantStockOp(variantId: number) {
    const qty = parseInt(variantStockQty)
    if (!qty || qty < 1) { setVariantStockMsg({ id: variantId, msg: 'Miktar en az 1 olmalıdır.', ok: false }); return }

    const vs = variantStocks[variantId]
    if (variantStockAction === 'reduce' && vs && qty > vs.availableQuantity) {
      setVariantStockMsg({ id: variantId, msg: `Mevcut stok (${vs.availableQuantity}) yetersiz.`, ok: false }); return
    }

    setVariantStockLoading(true)
    try {
      const updated = variantStockAction === 'add'
        ? await adminService.addVariantStock(variantId, qty, variantStockNote || undefined)
        : await adminService.reduceVariantStock(variantId, qty, variantStockNote || undefined)

      setVariantStocks((prev) => ({ ...prev, [variantId]: updated }))
      if (variantStockAction === 'add' && updated.availableQuantity > 0) {
        setVariants((prev) => prev.map((v) => v.id === variantId ? { ...v, status: 'ACTIVE' } : v))
      }
      const label = variantStockAction === 'add' ? `+${qty}` : `-${qty}`
      setVariantStockMsg({ id: variantId, msg: `${label} adet stok güncellendi.`, ok: true })
      setVariantStockQty('')
      setVariantStockNote('')
      setVariantStockTarget(null)
    } catch {
      setVariantStockMsg({ id: variantId, msg: 'Stok işlemi başarısız.', ok: false })
    } finally {
      setVariantStockLoading(false)
    }
  }
  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSaveError(null)
    setUploadError(null)

    if (!form.name.trim()) return setSaveError('Ürün adı zorunludur.')
    if (!form.description.trim()) return setSaveError('Açıklama zorunludur.')
    if (!form.brand.trim()) return setSaveError('Marka zorunludur.')
    if (!form.categoryId) return setSaveError('Kategori seçiniz.')
    if (existingImageUrls.length + pendingFiles.length === 0) return setSaveError('En az 1 ürün resmi zorunludur.')
    if (!isEdit && draftVariants.length === 0) return setSaveError('En az 1 varyant zorunludur. Lütfen varyant ekleyiniz.')

    setSaving(true)
    try {
      const payload: AdminProductRequest = { ...form, imageUrls: existingImageUrls }

      let savedProduct: Awaited<ReturnType<typeof adminService.getProduct>>
      if (isEdit && productId) {
        savedProduct = await adminService.updateProduct(productId, payload)
      } else {
        savedProduct = await adminService.createProduct(payload)
      }

      if (pendingFiles.length > 0) {
        await adminService.uploadProductImages(savedProduct.id, pendingFiles)
        setPendingFiles([])
      }

      // Create any draft variants that were staged in create mode
      for (const draft of draftVariants) {
        await adminService.createVariant(savedProduct.id, variantFormToRequest(draft))
      }

      navigate('/admin/products')
    } catch {
      setSaveError('Kaydedilirken bir hata oluştu.')
    } finally {
      setSaving(false)
    }
  }

  if (loadingProduct) {
    return (
      <div className="pf-loading">
        <div className="pf-spinner" />
      </div>
    )
  }

  const hasVariants = variants.length > 0 || draftVariants.length > 0
  void hasVariants

  return (
    <div className="pf-page">
      <Breadcrumb
        items={[
          { label: 'Admin', to: '/admin' },
          { label: 'Ürünler', to: '/admin/products' },
          { label: isEdit ? 'Ürünü Düzenle' : 'Yeni Ürün Ekle' },
        ]}
        className="breadcrumb--mb-0"
      />
      <div className="pf-header">
        <Link to="/admin/products" className="pf-header__back-link"><ArrowLeft size={20} /></Link>
        <h1 className="pf-header__title">{isEdit ? 'Ürünü Düzenle' : 'Yeni Ürün Ekle'}</h1>
      </div>

      <form onSubmit={handleSubmit} className="pf-form">
        <ProductFormSection title="Temel Bilgiler" icon={<Package size={16} />}>
          <div className="pf-basic-grid">
            <div>
              <label className="pf-label">Ürün Adı *</label>
              <input className="pf-input" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} placeholder="Ürün adı girin" required />
            </div>
            <div>
              <label className="pf-label">Açıklama *</label>
              <textarea className="pf-input pf-textarea" value={form.description} onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))} placeholder="Ürün açıklaması" required />
            </div>
            <div className="pf-two-col">
              <div>
                <label className="pf-label">Marka *</label>
                <input className="pf-input" value={form.brand} onChange={(e) => setForm((f) => ({ ...f, brand: e.target.value }))} placeholder="Marka adı" required />
              </div>
              <div>
                <label className="pf-label">Kategori *</label>
                <select className="pf-input" value={form.categoryId ?? ''} onChange={(e) => setForm((f) => ({ ...f, categoryId: Number(e.target.value) }))} required>
                  <option value="">-- Seçin --</option>
                  {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </div>
            </div>
          </div>
        </ProductFormSection>
        <ProductFormSection title="Ürün Görselleri" icon={<Image size={16} />}
          action={
            canAddMore
              ? <button type="button" className="pf-btn pf-btn--secondary" onClick={() => imageInputRef.current?.click()}><Upload size={14} />Resim Ekle</button>
              : <span className="pf-images-max-text">Maksimum 3 resim</span>
          }
        >
          <input ref={imageInputRef} type="file" accept="image/*" multiple className="pf-hidden-input" onChange={handleImageSelect} />

          {uploadError && <div className="pf-upload-error">{uploadError}</div>}

          <div className="pf-images-row">
            {existingImageUrls.map((url) => (
              <div key={url} className="pf-image-thumb">
                <img src={url} alt="" className="pf-image-thumb__img" />
                <button type="button" onClick={() => setExistingImageUrls((prev) => prev.filter((u) => u !== url))} className="pf-image-thumb__remove-btn">
                  <X size={12} />
                </button>
              </div>
            ))}
            {pendingFiles.map((file, i) => (
              <div key={i} className="pf-image-thumb">
                <img src={URL.createObjectURL(file)} alt="" className="pf-image-thumb__img pf-image-thumb__img--pending" />
                <button type="button" onClick={() => setPendingFiles((prev) => prev.filter((_, j) => j !== i))} className="pf-image-thumb__remove-btn">
                  <X size={12} />
                </button>
                <span className="pf-image-thumb__new-badge">Yeni</span>
              </div>
            ))}
            {totalImages === 0 && (
              <button type="button" onClick={() => imageInputRef.current?.click()} className="pf-image-empty-btn">
                <Upload size={24} /><span>Resim Ekle</span>
              </button>
            )}
          </div>
          <p className="pf-images-hint">En az 1 resim zorunlu · Maksimum 3 resim · Mavi kenarlı olanlar kayıt sonrası yüklenecek.</p>
        </ProductFormSection>
        <ProductFormSection
          title="Varyantlar (Beden / Renk vb.)"
          icon={<Package size={16} />}
          action={
            <button type="button" className="pf-btn pf-btn--primary" onClick={() => { setShowAddVariant(true); setVariantError(null); setNewVariantForm(EMPTY_VARIANT_FORM()) }}>
              <Plus size={14} />Varyant Ekle
            </button>
          }
        >
          {!isEdit && draftVariants.length > 0 && (
            <div className="pf-draft-list">
              <p className="pf-draft-header">{draftVariants.length} varyant ürün kaydedilince oluşturulacak:</p>
              {draftVariants.map((draft, i) => {
                const attrLabel = draft.attrs.filter((a) => a.key && a.value).map((a) => `${a.key}: ${a.value}`).join(', ')
                return (
                  <div key={i} className="pf-draft-item">
                    <div className="pf-draft-item__info">
                      <div className="pf-draft-item__attrs">{attrLabel}</div>
                      <div className="pf-draft-item__meta">
                        SKU: {draft.sku} · ₺{draft.price}
                        {draft.discountRate && <span className="pf-draft-item__discount"> (%{draft.discountRate} indirim)</span>}
                      </div>
                    </div>
                    <button type="button" onClick={() => setDraftVariants((prev) => prev.filter((_, j) => j !== i))} className="pf-draft-item__remove-btn">
                      <Trash2 size={15} />
                    </button>
                  </div>
                )
              })}
            </div>
          )}

          {variants.length === 0 && draftVariants.length === 0 && !showAddVariant && !editingVariantId && (
            <p className="pf-variants-empty">Henüz varyant yok. "Varyant Ekle" ile ekleyebilirsiniz.</p>
          )}

          <div className="pf-variant-list">
            {variants.map((v) => {
              const vs = variantStocks[v.id]
              const isTarget = variantStockTarget === v.id
              const isActive = v.status === 'ACTIVE'
              const attrLabel = Object.entries(v.attributes ?? {}).map(([k, val]) => `${k}: ${val}`).join(', ')

              return (
                <div key={v.id} className="pf-variant-row">
                  <div className="pf-variant-row__header">
                    <span className={`pf-status-badge pf-status-badge--${isActive ? 'active' : 'passive'}`}>
                      {isActive ? 'ACTIVE' : 'PASSIVE'}
                    </span>
                    <div className="pf-variant-row__info">
                      <div className="pf-variant-row__attrs">{attrLabel}</div>
                      <div className="pf-variant-row__meta">
                        SKU: {v.sku} · {v.originalPrice
                          ? <><span className="pf-variant-row__original-price">{v.originalPrice.toLocaleString('tr-TR')} ₺</span><span className="pf-variant-row__discount-price">{v.price.toLocaleString('tr-TR')} · (%{v.discountRate} indirim)</span></>
                          : <span>{v.price.toLocaleString('tr-TR')} ₺</span>
                        }
                      </div>
                    </div>

                    {vs ? (
                      <div className="pf-stock-badges">
                        {[
                          { label: 'Toplam',  value: vs.quantity,           mod: 'total'    },
                          { label: 'Rezerve', value: vs.reservedQuantity,   mod: 'reserved' },
                          { label: 'Mevcut',  value: vs.availableQuantity,  mod: 'available'},
                        ].map(({ label, value, mod }) => (
                          <div key={label} className={`pf-stock-badge pf-stock-badge--${mod}`}>
                            <div className="pf-stock-badge__label">{label}</div>
                            <div className="pf-stock-badge__value">{value}</div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <span className="pf-stock-empty">Stok: —</span>
                    )}

                    <div className="pf-variant-row__actions">
                      <button
                        type="button"
                        onClick={() => { if (editingVariantId === v.id) closeEditVariant(); else openEditVariant(v) }}
                        title="Varyantı düzenle"
                        className={`pf-variant-btn pf-variant-btn--${editingVariantId === v.id ? 'edit-active' : 'edit'}`}
                      >
                        <Pencil size={14} />
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          if (isTarget) { setVariantStockTarget(null) }
                          else { setVariantStockTarget(v.id); setVariantStockAction('add'); setVariantStockQty(''); setVariantStockNote(''); setVariantStockMsg(null) }
                        }}
                        className={`pf-variant-btn pf-variant-btn--${isTarget ? 'stock-active' : 'stock'}`}
                      >
                        {isTarget ? 'Kapat' : 'Stok'}
                      </button>
                      <button type="button" onClick={() => handleDeleteVariant(v.id)} className="pf-variant-btn pf-variant-btn--delete">
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                  {isTarget && (
                    <div className="pf-stock-panel">
                      <div className="pf-stock-panel__toggle">
                        {(['add', 'reduce'] as const).map((a) => (
                          <button
                            key={a}
                            type="button"
                            onClick={() => setVariantStockAction(a)}
                            className={`pf-stock-panel__toggle-btn pf-stock-panel__toggle-btn--${variantStockAction === a ? 'active' : 'inactive'}`}
                          >
                            {a === 'add' ? <><TrendingUp size={13} /> Ekle</> : <><TrendingDown size={13} /> Azalt</>}
                          </button>
                        ))}
                      </div>
                      <div className="pf-stock-panel__form-row">
                        <div>
                          <label className="pf-label">Miktar</label>
                          <input className="pf-input pf-stock-input--qty" type="number" min="1" value={variantStockQty} onChange={(e) => setVariantStockQty(e.target.value)} placeholder="ör: 50" />
                        </div>
                        <div>
                          <label className="pf-label">Not (opsiyonel)</label>
                          <input className="pf-input pf-stock-input--note" value={variantStockNote} onChange={(e) => setVariantStockNote(e.target.value)} placeholder="Stok hareketi notu" />
                        </div>
                        <button
                          type="button"
                          disabled={variantStockLoading}
                          onClick={() => handleVariantStockOp(v.id)}
                          className={`pf-stock-panel__submit-btn${variantStockAction === 'reduce' ? ' pf-stock-panel__submit-btn--reduce' : ''}${variantStockLoading ? ' pf-stock-panel__submit-btn--loading' : ''}`}
                        >
                          {variantStockAction === 'add' ? <TrendingUp size={14} /> : <TrendingDown size={14} />}
                          {variantStockLoading ? 'İşleniyor...' : variantStockAction === 'add' ? 'Stok Ekle' : 'Stok Azalt'}
                        </button>
                      </div>
                      {variantStockMsg?.id === v.id && (
                        <div className={`pf-stock-panel__msg pf-stock-panel__msg--${variantStockMsg.ok ? 'ok' : 'err'}`}>
                          {variantStockMsg.msg}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </ProductFormSection>
        {saveError && (
          <div className="pf-save-error"><AlertCircle size={16} />{saveError}</div>
        )}
        <button type="submit" disabled={saving} className="pf-submit-btn">
          <Save size={16} />{saving ? 'Kaydediliyor...' : isEdit ? 'Değişiklikleri Kaydet' : 'Ürünü Oluştur'}
        </button>
      </form>

      <VariantModal
        open={showAddVariant}
        onClose={() => { setShowAddVariant(false); setVariantError(null) }}
        form={newVariantForm}
        onChange={setNewVariantForm}
        onAttrChange={handleVariantAttrChange}
        onAddAttr={() => setNewVariantForm({ ...newVariantForm, attrs: [...newVariantForm.attrs, { key: '', value: '' }] })}
        onRemoveAttr={(i) => setNewVariantForm({ ...newVariantForm, attrs: newVariantForm.attrs.filter((_, j) => j !== i) })}
        onSubmit={handleAddVariant}
        error={variantError}
        loading={addingVariant}
        submitLabel={isEdit ? 'Varyantı Kaydet' : 'Varyantı Listeye Ekle'}
        title="Yeni Varyant"
      />

      <VariantModal
        open={Boolean(editingVariantId)}
        onClose={closeEditVariant}
        form={editVariantForm}
        onChange={setEditVariantForm}
        onAttrChange={handleEditVariantAttrChange}
        onAddAttr={() => setEditVariantForm({ ...editVariantForm, attrs: [...editVariantForm.attrs, { key: '', value: '' }] })}
        onRemoveAttr={(i) => setEditVariantForm({ ...editVariantForm, attrs: editVariantForm.attrs.filter((_, j) => j !== i) })}
        onSubmit={handleUpdateVariant}
        error={editVariantError}
        loading={editingVariantLoading}
        submitLabel="Varyantı Güncelle"
        title="Varyantı Düzenle"
      />
    </div>
  )
}




