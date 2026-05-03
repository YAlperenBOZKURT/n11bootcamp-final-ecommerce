import { useEffect, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { MapPin, CreditCard, ShoppingBag, ChevronRight, Plus, Check, X, ShoppingCart } from 'lucide-react'
import SiteHeader from '../../shared/ui/SiteHeader'
import { useCart } from '../../context/CartContext'
import { useAuth } from '../../context/AuthContext'
import { authService, type AddressResponse, type AddressRequest } from '../../services/authService'
import { ApiError } from '../../lib/apiClient'
import './CheckoutPage.css'

export default function CheckoutPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const { cart } = useCart()

  const [addresses, setAddresses] = useState<AddressResponse[]>([])
  const [selectedAddress, setSelectedAddress] = useState<AddressResponse | null>(null)
  const [addressLoading, setAddressLoading] = useState(true)
  const [showAddressModal, setShowAddressModal] = useState(false)
  const [showAddForm, setShowAddForm] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [confirmed, setConfirmed] = useState(false)
  const [appliedCoupon, setAppliedCoupon] = useState<{ code: string; discountAmount: number; finalAmount: number } | null>(null)

  const [newAddr, setNewAddr] = useState<AddressRequest>({
    title: '', recipientName: '', recipientPhone: '',
    city: '', district: '', neighborhood: '', addressLine: '', zipCode: '',
  })
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!user) { navigate('/login'); return }
    if (!cart || cart.items.length === 0) { navigate('/cart'); return }

    authService.getAddresses()
      .then((list) => {
        setAddresses(list)
        const def = list.find((a) => a.isDefault) ?? list[0] ?? null
        setSelectedAddress(def)
      })
      .catch(() => {})
      .finally(() => setAddressLoading(false))
  }, [user, cart])

  useEffect(() => {
    const fromStorage = sessionStorage.getItem('applied_coupon')
    if (!fromStorage) return
    try {
      const parsed = JSON.parse(fromStorage) as { code: string; discountAmount: number; finalAmount: number }
      if (parsed?.code) setAppliedCoupon(parsed)
    } catch {
      setAppliedCoupon(null)
    }
  }, [])

  const handleSaveAddress = async () => {
    setFormError(null)
    if (!newAddr.title || !newAddr.recipientName || !newAddr.recipientPhone || !newAddr.city || !newAddr.district || !newAddr.addressLine) {
      setFormError('Zorunlu alanları doldurun.')
      return
    }
    setSaving(true)
    try {
      const saved = await authService.createAddress(newAddr)
      setAddresses((prev) => [...prev, saved])
      setSelectedAddress(saved)
      setShowAddForm(false)
      setShowAddressModal(false)
      setNewAddr({ title: '', recipientName: '', recipientPhone: '', city: '', district: '', neighborhood: '', addressLine: '', zipCode: '' })
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Adres kaydedilemedi.')
    } finally {
      setSaving(false)
    }
  }

  const handleProceed = () => {
    if (!selectedAddress || !confirmed) return
    navigate('/checkout/payment', {
      state: {
        items: cart!.items.map((i) => ({
          productId: i.productId,
          variantId: i.variantId ?? null,
          quantity: i.quantity,
        })),
        totalAmount: cart!.totalAmount,
        couponCode: appliedCoupon?.code ?? null,
        discountAmount: appliedCoupon?.discountAmount ?? null,
        finalAmount: appliedCoupon?.finalAmount ?? cart!.totalAmount,
        cartItems: cart!.items,
        address: selectedAddress,
      },
    })
  }

  if (!cart || cart.items.length === 0) return null

  return (
    <div className="checkout-page">
      <SiteHeader />

      <main className="checkout-main">
        <div className="checkout-breadcrumb">
          <Link to="/cart" className="checkout-breadcrumb__link">Sepet</Link>
          <ChevronRight size={14} />
          <span className="checkout-breadcrumb__current">Teslimat & Ödeme</span>
          <ChevronRight size={14} />
          <span className="checkout-breadcrumb__muted">Kart Bilgileri</span>
        </div>

        <div className="checkout-layout">
          <div className="checkout-column">
            <Section icon={<MapPin size={18} color="#6366f1" />} title="Teslimat Adresi">
              {addressLoading ? (
                <AddressSkeleton />
              ) : selectedAddress ? (
                <div>
                  <AddressCard address={selectedAddress} selected />
                  <button onClick={() => setShowAddressModal(true)} className="checkout-address-change-btn">
                    Adresi Değiştir
                  </button>
                </div>
              ) : (
                <div>
                  <p className="checkout-no-address-text">Kayıtlı adresiniz bulunmuyor.</p>
                  <button
                    onClick={() => { setShowAddressModal(true); setShowAddForm(true) }}
                    className="checkout-address-add-btn"
                  >
                    <Plus size={14} /> Adres Ekle
                  </button>
                </div>
              )}
            </Section>
            <Section icon={<CreditCard size={18} color="#6366f1" />} title="Ödeme Yöntemi">
              <div className="checkout-payment-badges">
                <PaymentBadge label="iyzico" selected />
                <PaymentBadge label="Kapıda Ödeme" disabled />
                <PaymentBadge label="Kredi Kartı" disabled />
              </div>
              <p className="checkout-payment-hint">Ödeme bilgilerini bir sonraki adımda gireceksiniz.</p>
            </Section>
            <Section icon={<ShoppingBag size={18} color="#6366f1" />} title={`Ürünler (${cart.items.length})`}>
              <div className="checkout-items-list">
                {cart.items.map((item) => (
                  <div key={item.itemId} className="checkout-item">
                    <div className="checkout-item__icon">
                      <ShoppingCart size={18} color="#c7d2fe" />
                    </div>
                    <div className="checkout-item__info">
                      <p className="checkout-item__name">{item.productName}</p>
                      <p className="checkout-item__meta">{item.quantity} adet ×₺{item.unitPrice.toFixed(2)}</p>
                    </div>
                    <span className="checkout-item__total">₺{item.lineTotal.toFixed(2)}</span>
                  </div>
                ))}
              </div>
            </Section>
          </div>
          <div className="checkout-summary-panel">
            <h2 className="checkout-summary-panel__title">Sipariş Özeti</h2>

            <div className="checkout-summary-items">
              {cart.items.map((item) => (
                <div key={item.itemId} className="checkout-summary-item">
                  <span className="checkout-summary-item__name">{item.productName} ×{item.quantity}</span>
                  <span className="checkout-summary-item__price">₺{item.lineTotal.toFixed(2)}</span>
                </div>
              ))}
            </div>

            <div className="checkout-summary-total-section">
              {appliedCoupon && (
                <div className="checkout-summary-discount-row">
                  <span>Kupon İndirimi ({appliedCoupon.code})</span>
                  <span>-₺{appliedCoupon.discountAmount.toFixed(2)}</span>
                </div>
              )}
              <div className="checkout-summary-total-row">
                <span>Toplam</span>
                <span className="checkout-summary-total-amount">₺{(appliedCoupon?.finalAmount ?? cart.totalAmount).toFixed(2)}</span>
              </div>
              <p className="checkout-summary-vat-hint">KDV dahil</p>
            </div>
            <label className="checkout-confirm-box">
              <div
                onClick={() => setConfirmed((v) => !v)}
                className={`checkout-confirm-indicator checkout-confirm-indicator--${confirmed ? 'checked' : 'unchecked'}`}
              >
                {confirmed && <Check size={11} color="#fff" strokeWidth={3} />}
              </div>
              <span className="checkout-confirm-text">
                Ön bilgilendirme koşullarını ve sipariş onay formunu okudum, onaylıyorum.
              </span>
            </label>
            <button
              onClick={handleProceed}
              disabled={!selectedAddress || !confirmed}
              className={`checkout-cta-btn${(!selectedAddress || !confirmed) ? ' checkout-cta-btn--disabled' : ''}`}
            >
              Ödeme Adımına Geç
            </button>

            {!selectedAddress && (
              <p className="checkout-cta-hint">Devam etmek için adres seçin.</p>
            )}
          </div>
        </div>
      </main>
      {showAddressModal && (
        <div
          onClick={(e) => { if (e.target === e.currentTarget) { setShowAddressModal(false); setShowAddForm(false) } }}
          className="checkout-modal-backdrop"
        >
          <div className="checkout-modal">
            <div className="checkout-modal__header">
              <h3 className="checkout-modal__title">
                {showAddForm ? 'Yeni Adres Ekle' : 'Adres Seç'}
              </h3>
              <button
                onClick={() => { setShowAddressModal(false); setShowAddForm(false) }}
                className="checkout-modal__close-btn"
              >
                <X size={20} />
              </button>
            </div>

            {!showAddForm ? (
              <>
                <div className="checkout-address-options">
                  {addresses.map((addr) => (
                    <button
                      key={addr.id}
                      onClick={() => { setSelectedAddress(addr); setShowAddressModal(false) }}
                      className={`checkout-address-option checkout-address-option--${selectedAddress?.id === addr.id ? 'selected' : 'unselected'}`}
                    >
                      <p className="checkout-address-option__title">{addr.title}</p>
                      <p className="checkout-address-option__recipient">{addr.recipientName} · {addr.recipientPhone}</p>
                      <p className="checkout-address-option__line">{addr.addressLine}, {addr.district}/{addr.city}</p>
                    </button>
                  ))}
                </div>
                <button onClick={() => setShowAddForm(true)} className="checkout-modal-add-btn">
                  <Plus size={15} /> Yeni Adres Ekle
                </button>
              </>
            ) : (
              <div className="checkout-form">
                {formError && <div className="checkout-form-error">{formError}</div>}
                {[
                  { key: 'title', label: 'Adres Başlığı *', placeholder: 'Ev, İş...' },
                  { key: 'recipientName', label: 'Ad Soyad *', placeholder: 'Alıcı adı' },
                  { key: 'recipientPhone', label: 'Telefon *', placeholder: '05xx xxx xx xx' },
                  { key: 'city', label: 'Şehir *', placeholder: 'İstanbul' },
                  { key: 'district', label: 'İlçe *', placeholder: 'Kadıköy' },
                  { key: 'neighborhood', label: 'Mahalle', placeholder: 'Moda Mah.' },
                  { key: 'addressLine', label: 'Adres *', placeholder: 'Sokak, No, Daire...' },
                  { key: 'zipCode', label: 'Posta Kodu', placeholder: '34710' },
                ].map(({ key, label, placeholder }) => (
                  <div key={key} className="checkout-form-field">
                    <label className="checkout-form-label">{label}</label>
                    <input
                      value={(newAddr as any)[key] || ''}
                      onChange={(e) => setNewAddr((prev) => ({ ...prev, [key]: e.target.value }))}
                      placeholder={placeholder}
                      className="checkout-form-input"
                    />
                  </div>
                ))}
                <div className="checkout-form-actions">
                  <button
                    onClick={() => { setShowAddForm(false); setFormError(null) }}
                    className="checkout-form-cancel-btn"
                  >
                    İptal
                  </button>
                  <button
                    onClick={handleSaveAddress}
                    disabled={saving}
                    className="checkout-form-save-btn"
                  >
                    {saving ? 'Kaydediliyor...' : 'Kaydet'}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

function Section({ icon, title, children }: { icon: React.ReactNode; title: string; children: React.ReactNode }) {
  return (
    <div className="checkout-section">
      <div className="checkout-section__header">
        {icon}
        <h2 className="checkout-section__title">{title}</h2>
      </div>
      {children}
    </div>
  )
}

function AddressCard({ address, selected }: { address: AddressResponse; selected?: boolean }) {
  return (
    <div className={`checkout-address-card checkout-address-card--${selected ? 'selected' : 'unselected'}`}>
      <div className="checkout-address-card__header">
        <MapPin size={14} color="#6366f1" />
        <span className="checkout-address-card__title">{address.title}</span>
        {address.isDefault && (
          <span className="checkout-address-card__default-badge">Varsayılan</span>
        )}
      </div>
      <p className="checkout-address-card__recipient">{address.recipientName} · {address.recipientPhone}</p>
      <p className="checkout-address-card__line">{address.addressLine}, {address.district}/{address.city}</p>
    </div>
  )
}

function PaymentBadge({ label, selected, disabled }: { label: string; selected?: boolean; disabled?: boolean }) {
  return (
    <div className={`checkout-payment-badge${selected ? ' checkout-payment-badge--selected' : disabled ? ' checkout-payment-badge--disabled' : ''}`}>
      {selected && <Check size={14} strokeWidth={3} />}
      {label}
    </div>
  )
}

function AddressSkeleton() {
  return (
    <div className="checkout-address-skeleton">
      <div className="checkout-address-skeleton__line1" />
      <div className="checkout-address-skeleton__line2" />
    </div>
  )
}
