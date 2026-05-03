import VariantFormPanel, { type VariantFormState } from './VariantFormPanel'
import './VariantModal.css'

type Props = {
  open: boolean
  onClose: () => void
  form: VariantFormState
  onChange: (f: VariantFormState) => void
  onAttrChange: (i: number, field: 'key' | 'value', val: string) => void
  onAddAttr: () => void
  onRemoveAttr: (i: number) => void
  onSubmit: () => void
  error: string | null
  loading: boolean
  submitLabel: string
  title: string
}

export default function VariantModal({
  open,
  onClose,
  form,
  onChange,
  onAttrChange,
  onAddAttr,
  onRemoveAttr,
  onSubmit,
  error,
  loading,
  submitLabel,
  title,
}: Props) {
  if (!open) return null

  return (
    <div
      onClick={(e) => { if (e.currentTarget === e.target) onClose() }}
      className="variant-modal-backdrop"
    >
      <div className="variant-modal__inner">
        <VariantFormPanel
          form={form}
          onChange={onChange}
          onAttrChange={onAttrChange}
          onAddAttr={onAddAttr}
          onRemoveAttr={onRemoveAttr}
          onSubmit={onSubmit}
          onCancel={onClose}
          error={error}
          loading={loading}
          submitLabel={submitLabel}
          title={title}
        />
      </div>
    </div>
  )
}

