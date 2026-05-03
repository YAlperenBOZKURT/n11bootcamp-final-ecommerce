import { AlertCircle, Plus, Trash2 } from 'lucide-react'
import './VariantFormPanel.css'

export type AttrEntry = { key: string; value: string }

export type VariantFormState = {
  sku: string
  attrs: AttrEntry[]
  price: string
  discountRate: string
}

type Props = {
  form: VariantFormState
  onChange: (f: VariantFormState) => void
  onAttrChange: (i: number, field: 'key' | 'value', val: string) => void
  onAddAttr: () => void
  onRemoveAttr: (i: number) => void
  onSubmit: () => void
  onCancel: () => void
  error: string | null
  loading: boolean
  submitLabel: string
  title: string
}

export default function VariantFormPanel({
  form,
  onChange,
  onAttrChange,
  onAddAttr,
  onRemoveAttr,
  onSubmit,
  onCancel,
  error,
  loading,
  submitLabel,
  title,
}: Props) {
  return (
    <div className="variant-form-panel">
      <div className="variant-form-panel__title">{title}</div>

      <div className="variant-form-panel__field">
        <label className="variant-label">SKU *</label>
        <input
          className="variant-input"
          style={{ width: 240 }}
          value={form.sku}
          onChange={(e) => onChange({ ...form, sku: e.target.value })}
          placeholder="or: ELBISE-SYH-M"
        />
      </div>

      <div className="variant-form-panel__grid">
        <div>
          <label className="variant-label">Fiyat (TL) *</label>
          <input
            className="variant-input"
            type="number"
            min="0.01"
            step="0.01"
            value={form.price}
            onChange={(e) => onChange({ ...form, price: e.target.value })}
            placeholder="or: 499.90"
          />
        </div>
        <div>
          <label className="variant-label">Indirim Orani (%) - opsiyonel</label>
          <input
            className="variant-input"
            type="number"
            min="0"
            max="100"
            step="1"
            value={form.discountRate}
            onChange={(e) => onChange({ ...form, discountRate: e.target.value })}
            placeholder="Bos = indirim yok"
          />
        </div>
      </div>

      <div style={{ marginBottom: '12px' }}>
        <label className="variant-label">Ozellikler * (en az 1)</label>
        <div className="variant-form-panel__attr-list">
          {form.attrs.map((attr, i) => (
            <div key={i} className="variant-form-panel__attr-row">
              <input
                className="variant-input"
                style={{ width: 130 }}
                placeholder="Ozellik (or: Beden)"
                value={attr.key}
                onChange={(e) => onAttrChange(i, 'key', e.target.value)}
              />
              <span className="variant-form-panel__attr-sep">:</span>
              <input
                className="variant-input"
                style={{ width: 130 }}
                placeholder="Deger (or: M)"
                value={attr.value}
                onChange={(e) => onAttrChange(i, 'value', e.target.value)}
              />
              {form.attrs.length > 1 && (
                <button type="button" onClick={() => onRemoveAttr(i)} className="variant-form-panel__attr-remove-btn">
                  <Trash2 size={15} />
                </button>
              )}
            </div>
          ))}
          <button type="button" onClick={onAddAttr} className="variant-btn variant-btn--secondary variant-form-panel__add-attr-btn">
            <Plus size={13} />Ozellik Ekle
          </button>
        </div>
      </div>

      {error && (
        <div className="variant-form-panel__error">
          <AlertCircle size={14} />{error}
        </div>
      )}

      <div className="variant-form-panel__actions">
        <button type="button" disabled={loading} onClick={onSubmit} className="variant-btn variant-btn--primary" style={{ opacity: loading ? 0.7 : 1 }}>
          <Plus size={14} />{loading ? 'Isleniyor...' : submitLabel}
        </button>
        <button type="button" onClick={onCancel} className="variant-btn variant-btn--secondary">İptal</button>
      </div>
    </div>
  )
}
