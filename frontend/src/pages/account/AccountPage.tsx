import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  User, Mail, Phone, ShieldCheck, Calendar,
  LogOut, Pencil, Lock, Check, X, Eye, EyeOff,
  MapPin, Plus, Trash2, Star,
} from 'lucide-react'
import { useAuth } from '../../context/AuthContext'
import SiteHeader from '../../shared/ui/SiteHeader'
import {
  authService,
  type UserProfile,
  type UpdateProfileRequest,
  type AddressResponse,
  type AddressRequest,
} from '../../services/authService'
import { ApiError } from '../../lib/apiClient'
import './AccountPage.css'

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('tr-TR', {
    day: 'numeric', month: 'long', year: 'numeric',
  })
}

export default function AccountPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const [profile, setProfile]   = useState<UserProfile | null>(null)
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState<string | null>(null)

  const [activeTab, setActiveTab] = useState<'profile' | 'password' | 'addresses'>('profile')

  useEffect(() => {
    if (!user) { navigate('/login'); return }
    authService.getProfile()
      .then(setProfile)
      .catch(() => setError('Profil bilgileri yüklenemedi.'))
      .finally(() => setLoading(false))
  }, [user, navigate])

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  if (loading) {
    return (
      <div className="account-page">
        <SiteHeader />
        <main className="account-main">
          <div className="account-loading-title" />
          <div className="account-loading-card">
            {[140, 100, 120].map((_, i) => (
              <div key={i} className="account-loading-bar" />
            ))}
          </div>
        </main>
      </div>
    )
  }

  if (error || !profile) {
    return (
      <div className="account-page">
        <SiteHeader />
        <main className="account-error">{error ?? 'Bir hata oluştu.'}</main>
      </div>
    )
  }

  return (
    <div className="account-page">
      <SiteHeader />

      <main className="account-main">
        <div className="account-header">
          <h1 className="account-header__title">Hesabım</h1>
          <button onClick={handleLogout} className="account-logout-btn">
            <LogOut size={15} /> Çıkış Yap
          </button>
        </div>
        <div className="account-avatar-card">
          <div className="account-avatar-card__icon">
            <User size={28} color="#fff" />
          </div>
          <div>
            <p className="account-avatar-card__name">{profile.firstName} {profile.lastName}</p>
            <p className="account-avatar-card__meta">
              {profile.role === 'ROLE_ADMIN' ? 'Yönetici' : 'Üye'} · {profile.email}
            </p>
          </div>
        </div>
        <div className="account-tabs">
          {([
            { key: 'profile',   label: 'Profil Bilgileri' },
            { key: 'addresses', label: 'Adreslerim' },
            { key: 'password',  label: 'Şifre Değiştir' },
          ] as const).map(({ key, label }) => (
            <button
              key={key}
              onClick={() => setActiveTab(key)}
              className={`account-tab-btn account-tab-btn--${activeTab === key ? 'active' : 'inactive'}`}
            >
              {label}
            </button>
          ))}
        </div>
        {activeTab === 'profile' ? (
          <ProfileTab profile={profile} onUpdated={setProfile} />
        ) : activeTab === 'addresses' ? (
          <AddressTab />
        ) : (
          <PasswordTab />
        )}
      </main>
    </div>
  )
}

function ProfileTab({ profile, onUpdated }: {
  profile: UserProfile
  onUpdated: (p: UserProfile) => void
}) {
  const { login: _l, ...auth } = useAuth() as ReturnType<typeof useAuth>
  void _l; void auth

  const [editing, setEditing]     = useState(false)
  const [saving, setSaving]       = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saved, setSaved]         = useState(false)

  const [form, setForm] = useState<UpdateProfileRequest>({
    firstName:   profile.firstName,
    lastName:    profile.lastName,
    phoneNumber: profile.phoneNumber ?? '',
  })

  const handleSave = async () => {
    setSaving(true)
    setSaveError(null)
    try {
      const updated = await authService.updateProfile({
        ...form,
        phoneNumber: form.phoneNumber || null,
      })
      onUpdated(updated)
      setEditing(false)
      setSaved(true)
      setTimeout(() => setSaved(false), 3000)
    } catch (err) {
      setSaveError(err instanceof ApiError ? err.message : 'Güncelleme başarısız.')
    } finally {
      setSaving(false)
    }
  }

  const handleCancel = () => {
    setForm({
      firstName:   profile.firstName,
      lastName:    profile.lastName,
      phoneNumber: profile.phoneNumber ?? '',
    })
    setEditing(false)
    setSaveError(null)
  }

  return (
    <div className="account-tab-card">
      <div className="account-tab-card__header">
        <h2 className="account-tab-card__title">Kişisel Bilgiler</h2>
        <div className="account-toolbar">
          {saved && (
            <span className="account-saved-msg"><Check size={14} /> Kaydedildi</span>
          )}
          {editing ? (
            <>
              <ActionBtn onClick={handleCancel} color="#64748b" bg="#f8fafc" border="#e2e8f0" icon={<X size={14} />} label="İptal" />
              <ActionBtn onClick={handleSave} color="#fff" bg="#6366f1" border="#6366f1" icon={<Check size={14} />} label={saving ? 'Kaydediliyor...' : 'Kaydet'} disabled={saving} />
            </>
          ) : (
            <ActionBtn onClick={() => setEditing(true)} color="#6366f1" bg="#eef2ff" border="#c7d2fe" icon={<Pencil size={14} />} label="Düzenle" />
          )}
        </div>
      </div>

      {saveError && <div className="account-alert account-alert--error">{saveError}</div>}

      <div className="account-fields-grid">
        <Field icon={<User size={15} />} label="Ad" value={form.firstName} editing={editing} onChange={(v) => setForm((f) => ({ ...f, firstName: v }))} />
        <Field icon={<User size={15} />} label="Soyad" value={form.lastName} editing={editing} onChange={(v) => setForm((f) => ({ ...f, lastName: v }))} />
        <Field icon={<Mail size={15} />} label="E-posta" value={profile.email} editing={false} onChange={() => {}} hint="E-posta değiştirilemez" />
        <Field icon={<Phone size={15} />} label="Telefon" value={form.phoneNumber ?? ''} editing={editing} onChange={(v) => setForm((f) => ({ ...f, phoneNumber: v }))} placeholder="5XX XXX XX XX" />
        <Field icon={<ShieldCheck size={15} />} label="Hesap türü" value={profile.role === 'ROLE_ADMIN' ? 'Yönetici' : 'Standart Üye'} editing={false} onChange={() => {}} />
        <Field icon={<Calendar size={15} />} label="Üyelik tarihi" value={profile.createdAt ? formatDate(profile.createdAt) : '—'} editing={false} onChange={() => {}} />
      </div>
    </div>
  )
}

function PasswordTab() {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirm: '' })
  const [show, setShow] = useState({ current: false, new: false, confirm: false })
  const [saving, setSaving]   = useState(false)
  const [error, setError]     = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    if (form.newPassword !== form.confirm) { setError('Yeni şifreler eşleşmiyor.'); return }
    if (form.newPassword.length < 8) { setError('Yeni şifre en az 8 karakter olmalı.'); return }
    setSaving(true)
    try {
      await authService.changePassword({ currentPassword: form.currentPassword, newPassword: form.newPassword })
      setSuccess(true)
      setForm({ currentPassword: '', newPassword: '', confirm: '' })
      setTimeout(() => setSuccess(false), 4000)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Şifre değiştirilemedi.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="account-tab-card">
      <h2 className="account-tab-card__title" style={{ marginBottom: '24px' }}>Şifre Değiştir</h2>

      {success && <div className="account-alert account-alert--success"><Check size={15} /> Şifreniz başarıyla değiştirildi.</div>}
      {error && <div className="account-alert account-alert--error">{error}</div>}

      <form onSubmit={handleSubmit} className="account-password-form">
        {([
          { key: 'currentPassword', label: 'Mevcut Şifre',        showKey: 'current' },
          { key: 'newPassword',     label: 'Yeni Şifre',          showKey: 'new'     },
          { key: 'confirm',         label: 'Yeni Şifre (Tekrar)', showKey: 'confirm' },
        ] as const).map(({ key, label, showKey }) => (
          <div key={key}>
            <label className="account-field__label">{label}</label>
            <div className="account-password-input-wrapper">
              <span className="account-password-input-icon"><Lock size={15} /></span>
              <input
                type={show[showKey] ? 'text' : 'password'}
                value={form[key]}
                onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
                required
                className="account-password-input"
              />
              <button
                type="button"
                onClick={() => setShow((s) => ({ ...s, [showKey]: !s[showKey] }))}
                className="account-password-toggle-btn"
              >
                {show[showKey] ? <EyeOff size={15} /> : <Eye size={15} />}
              </button>
            </div>
          </div>
        ))}

        <button type="submit" disabled={saving} className="account-password-submit-btn">
          {saving ? 'Değiştiriliyor...' : 'Şifreyi Değiştir'}
        </button>
      </form>
    </div>
  )
}

function Field({ icon, label, value, editing, onChange, hint, placeholder }: {
  icon: React.ReactNode
  label: string
  value: string
  editing: boolean
  onChange: (v: string) => void
  hint?: string
  placeholder?: string
}) {
  return (
    <div className="account-field">
      <label className="account-field__label">
        <span className="account-field__label-icon">{icon}</span>
        {label}
      </label>
      {editing ? (
        <input
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          className="account-field__input"
        />
      ) : (
        <div className={`account-field__readonly account-field__readonly--${value ? 'has-value' : 'empty'}`}>
          {value || '—'}
          {hint && <span className="account-field__hint">({hint})</span>}
        </div>
      )}
    </div>
  )
}

function ActionBtn({ onClick, color, bg, border, icon, label, disabled }: {
  onClick: () => void
  color: string
  bg: string
  border: string
  icon: React.ReactNode
  label: string
  disabled?: boolean
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className="account-action-btn"
      style={{ color, background: bg, borderColor: border }}
    >
      {icon}{label}
    </button>
  )
}

const EMPTY_ADDR = (): AddressRequest => ({
  title: '', recipientName: '', recipientPhone: '',
  city: '', district: '', neighborhood: '',
  addressLine: '', zipCode: '',
})

function AddressTab() {
  const [addresses, setAddresses] = useState<AddressResponse[]>([])
  const [loading, setLoading]     = useState(true)
  const [error, setError]         = useState<string | null>(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing]     = useState<AddressResponse | null>(null)
  const [deleting, setDeleting]   = useState<number | null>(null)

  const load = () => {
    setLoading(true)
    authService.getAddresses()
      .then(setAddresses)
      .catch(() => setError('Adresler yüklenemedi.'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const openCreate = () => { setEditing(null); setModalOpen(true) }
  const openEdit   = (a: AddressResponse) => { setEditing(a); setModalOpen(true) }

  const handleDelete = async (id: number) => {
    setDeleting(id)
    try {
      await authService.deleteAddress(id)
      setAddresses((prev) => prev.filter((a) => a.id !== id))
    } catch {
      setError('Adres silinemedi.')
    } finally {
      setDeleting(null)
    }
  }

  const handleSetDefault = async (id: number) => {
    try {
      const updated = await authService.setDefaultAddress(id)
      setAddresses((prev) => prev.map((a) => ({ ...a, isDefault: a.id === updated.id })))
    } catch {
      setError('Varsayılan adres ayarlanamadı.')
    }
  }

  const handleSaved = (saved: AddressResponse) => {
    setAddresses((prev) => {
      const exists = prev.find((a) => a.id === saved.id)
      if (exists) return prev.map((a) => a.id === saved.id ? saved : a)
      return [...prev, saved]
    })
    setModalOpen(false)
  }

  return (
    <div className="account-tab-card">
      <div className="account-address-tab-header">
        <h2 className="account-tab-card__title">Adreslerim</h2>
        <button onClick={openCreate} className="account-address-add-btn">
          <Plus size={14} /> Yeni Adres Ekle
        </button>
      </div>

      {error && <div className="account-alert account-alert--error">{error}</div>}

      {loading ? (
        <div className="account-address-skeleton">
          {[1, 2].map((i) => (
            <div key={i} className="account-address-skeleton__item" />
          ))}
        </div>
      ) : addresses.length === 0 ? (
        <div className="account-address-empty">
          <MapPin size={36} className="account-address-empty__icon" />
          <p className="account-address-empty__text">Henüz kayıtlı adresiniz yok.</p>
        </div>
      ) : (
        <div className="account-address-list">
          {addresses.map((addr) => (
            <div key={addr.id} className={`account-address-card account-address-card--${addr.isDefault ? 'default' : 'normal'}`}>
              {addr.isDefault && (
                <span className="account-address-card__default-badge">
                  <Star size={10} fill="#6366f1" /> Varsayılan
                </span>
              )}
              <div className="account-address-card__title-row">
                <MapPin size={14} color="#6366f1" /> {addr.title}
              </div>
              <div className="account-address-card__body">
                <div>{addr.recipientName} · {addr.recipientPhone}</div>
                <div>{addr.addressLine}</div>
                <div>
                  {[addr.neighborhood, addr.district, addr.city].filter(Boolean).join(', ')}
                  {addr.zipCode && <span className="account-address-card__zip"> {addr.zipCode}</span>}
                </div>
              </div>
              <div className="account-address-card__actions">
                <button onClick={() => openEdit(addr)} className="account-address-card__btn account-address-card__btn--edit">
                  <Pencil size={12} /> Düzenle
                </button>
                {!addr.isDefault && (
                  <button onClick={() => handleSetDefault(addr.id)} className="account-address-card__btn account-address-card__btn--default">
                    <Star size={12} /> Varsayılan Yap
                  </button>
                )}
                <button
                  onClick={() => handleDelete(addr.id)}
                  disabled={deleting === addr.id}
                  className="account-address-card__btn account-address-card__btn--delete"
                  style={{ opacity: deleting === addr.id ? 0.6 : 1 }}
                >
                  <Trash2 size={12} /> {deleting === addr.id ? 'Siliniyor...' : 'Sil'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {modalOpen && (
        <AddressModal
          initial={editing}
          onClose={() => setModalOpen(false)}
          onSaved={handleSaved}
        />
      )}
    </div>
  )
}

function AddressModal({ initial, onClose, onSaved }: {
  initial: AddressResponse | null
  onClose: () => void
  onSaved: (a: AddressResponse) => void
}) {
  const [form, setForm] = useState<AddressRequest>(
    initial
      ? {
          title: initial.title,
          recipientName: initial.recipientName,
          recipientPhone: initial.recipientPhone,
          city: initial.city,
          district: initial.district,
          neighborhood: initial.neighborhood ?? '',
          addressLine: initial.addressLine,
          zipCode: initial.zipCode ?? '',
        }
      : EMPTY_ADDR()
  )
  const [saving, setSaving] = useState(false)
  const [error, setError]   = useState<string | null>(null)

  const f = (key: keyof AddressRequest) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    if (!form.title.trim())           return setError('Adres başlığı zorunludur.')
    if (!form.recipientName.trim())   return setError('Ad soyad zorunludur.')
    if (!form.recipientPhone.trim())  return setError('Telefon numarası zorunludur.')
    if (!form.city.trim())            return setError('Şehir zorunludur.')
    if (!form.district.trim())        return setError('İlçe zorunludur.')
    if (!form.addressLine.trim())     return setError('Adres satırı zorunludur.')

    setSaving(true)
    try {
      const payload: AddressRequest = {
        ...form,
        neighborhood: form.neighborhood?.trim() || undefined,
        zipCode: form.zipCode?.trim() || undefined,
      }
      const saved = initial
        ? await authService.updateAddress(initial.id, payload)
        : await authService.createAddress(payload)
      onSaved(saved)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Adres kaydedilemedi.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div
      className="account-address-modal-backdrop"
      onClick={(e) => { if (e.target === e.currentTarget) onClose() }}
    >
      <div className="account-address-modal">
        <div className="account-address-modal__header">
          <h3 className="account-address-modal__title">
            {initial ? 'Adresi Düzenle' : 'Yeni Adres Ekle'}
          </h3>
          <button onClick={onClose} className="account-address-modal__close-btn"><X size={20} /></button>
        </div>

        <form onSubmit={handleSubmit} className="account-address-form">
          <div className="account-address-form__field">
            <label className="account-address-form__label">
              Adres Başlığı * <span style={{ fontWeight: 400, color: '#94a3b8' }}>(ör: Ev, İş)</span>
            </label>
            <input className="account-address-form__input" value={form.title} onChange={f('title')} placeholder="Ev" />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div className="account-address-form__field">
              <label className="account-address-form__label">Ad Soyad *</label>
              <input className="account-address-form__input" value={form.recipientName} onChange={f('recipientName')} placeholder="Ad Soyad" />
            </div>
            <div className="account-address-form__field">
              <label className="account-address-form__label">Telefon *</label>
              <input className="account-address-form__input" value={form.recipientPhone} onChange={f('recipientPhone')} placeholder="05XX XXX XX XX" />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div className="account-address-form__field">
              <label className="account-address-form__label">Şehir *</label>
              <input className="account-address-form__input" value={form.city} onChange={f('city')} placeholder="İstanbul" />
            </div>
            <div className="account-address-form__field">
              <label className="account-address-form__label">İlçe *</label>
              <input className="account-address-form__input" value={form.district} onChange={f('district')} placeholder="Kadıköy" />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div className="account-address-form__field">
              <label className="account-address-form__label">
                Mahalle <span style={{ fontWeight: 400, color: '#94a3b8' }}>(opsiyonel)</span>
              </label>
              <input className="account-address-form__input" value={form.neighborhood ?? ''} onChange={f('neighborhood')} placeholder="Moda Mah." />
            </div>
            <div className="account-address-form__field">
              <label className="account-address-form__label">
                Posta Kodu <span style={{ fontWeight: 400, color: '#94a3b8' }}>(opsiyonel)</span>
              </label>
              <input className="account-address-form__input" value={form.zipCode ?? ''} onChange={f('zipCode')} placeholder="34710" />
            </div>
          </div>

          <div className="account-address-form__field">
            <label className="account-address-form__label">Adres *</label>
            <textarea
              className="account-address-form__input"
              style={{ height: '70px', resize: 'vertical' }}
              value={form.addressLine}
              onChange={f('addressLine')}
              placeholder="Sokak, Bina No, Daire No..."
            />
          </div>

          {error && <div className="account-alert account-alert--error">{error}</div>}

          <div className="account-address-form__actions">
            <button type="button" onClick={onClose} className="account-address-form__cancel-btn">İptal</button>
            <button type="submit" disabled={saving} className="account-address-form__save-btn">
              {saving ? 'Kaydediliyor...' : initial ? 'Güncelle' : 'Kaydet'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}




