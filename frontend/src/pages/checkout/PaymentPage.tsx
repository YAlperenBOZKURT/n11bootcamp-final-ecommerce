import { useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { CreditCard, Lock, ChevronRight, AlertCircle } from 'lucide-react'
import SiteHeader from '../../shared/ui/SiteHeader'
import { orderService, type OrderItemRequest } from '../../services/orderService'
import { type AddressResponse } from '../../services/authService'
import { type CartItemResponse } from '../../services/cartService'
import { useCart } from '../../context/CartContext'
import { ApiError } from '../../lib/apiClient'
import './PaymentPage.css'

interface LocationState {
  items: OrderItemRequest[]
  totalAmount: number
  couponCode?: string | null
  discountAmount?: number | null
  finalAmount?: number | null
  cartItems: CartItemResponse[]
  address: AddressResponse
}

export default function PaymentPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as LocationState | null
  const { clearCart } = useCart()

  const [cardHolder, setCardHolder] = useState('')
  const [cardNumber, setCardNumber] = useState('')
  const [expireMonth, setExpireMonth] = useState('')
  const [expireYear, setExpireYear] = useState('')
  const [cvc, setCvc] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!state || !state.items?.length) {
    return (
      <div className="payment-page">
        <SiteHeader />
        <div className="payment-empty">
          <AlertCircle size={48} color="#f59e0b" />
          <h2 className="payment-empty__title">Sepet bilgisi bulunamadı</h2>
          <Link to="/cart" className="payment-empty__link">← Sepete Dön</Link>
        </div>
      </div>
    )
  }

  const { items, totalAmount, cartItems, address, couponCode, discountAmount, finalAmount } = state
  const payableAmount = finalAmount ?? totalAmount

  const formatCardNumber = (val: string) =>
    val.replace(/\D/g, '').slice(0, 16).replace(/(.{4})/g, '$1 ').trim()

  const handleCardNumberChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setCardNumber(formatCardNumber(e.target.value))
  }

  const rawCardNumber = cardNumber.replace(/\s/g, '')

  const isFormValid =
    cardHolder.trim().length > 2 &&
    rawCardNumber.length === 16 &&
    expireMonth.length === 2 &&
    expireYear.length === 4 &&
    cvc.length >= 3

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!isFormValid || submitting) return
    setError(null)
    setSubmitting(true)

    try {
      const order = await orderService.placeOrder({
        items,
        couponCode: couponCode ?? null,
        card: {
          cardHolderName: cardHolder.trim(),
          cardNumber: rawCardNumber,
          expireMonth,
          expireYear,
          cvc,
        },
      })

      if (order.status === 'CONFIRMED') {
        await clearCart().catch(() => {})
      }
      navigate(`/orders/${order.orderNumber}`, { replace: true, state: { order } })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Ödeme işlemi başarısız. Lütfen tekrar deneyin.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="payment-page">
      <SiteHeader />

      <main className="payment-main">
        <div className="payment-breadcrumb">
          <Link to="/cart" className="payment-breadcrumb__link">Sepet</Link>
          <ChevronRight size={14} />
          <Link to="/checkout" className="payment-breadcrumb__link">Teslimat & Ödeme</Link>
          <ChevronRight size={14} />
          <span className="payment-breadcrumb__current">Kart Bilgileri</span>
        </div>

        <div className="payment-layout">
          <div className="payment-form-panel">
            <div className="payment-provider-header">
              <div className="payment-provider-icon">
                <CreditCard size={20} color="#fff" />
              </div>
              <div>
                <p className="payment-provider-title">iyzico ile Güvenli Ödeme</p>
                <p className="payment-provider-subtitle">256-bit SSL şifreli bağlantı</p>
              </div>
            </div>
            <div className="payment-card-visual">
              <div className="payment-card-visual__deco1" />
              <div className="payment-card-visual__deco2" />
              <p className="payment-card-visual__label">KART NUMARASI</p>
              <p className="payment-card-visual__number">{cardNumber || '•••• •••• •••• ••••'}</p>
              <div className="payment-card-visual__meta">
                <div className="payment-card-visual__meta-group">
                  <p className="payment-card-visual__meta-sublabel">KART SAHİBİ</p>
                  <p className="payment-card-visual__meta-value">{cardHolder || 'Ad Soyad'}</p>
                </div>
                <div className="payment-card-visual__meta-group payment-card-visual__meta-group--right">
                  <p className="payment-card-visual__meta-sublabel">SON KULLANIM</p>
                  <p className="payment-card-visual__meta-value">{expireMonth || 'AA'}/{expireYear?.slice(2) || 'YY'}</p>
                </div>
              </div>
            </div>

            {error && (
              <div className="payment-error">
                <AlertCircle size={16} />
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="payment-form">
              <FormField label="Kart Üzerindeki İsim">
                <input
                  value={cardHolder}
                  onChange={(e) => setCardHolder(e.target.value)}
                  placeholder="Ad Soyad"
                  autoComplete="cc-name"
                  className="payment-input"
                />
              </FormField>

              <FormField label="Kart Numarası">
                <input
                  value={cardNumber}
                  onChange={handleCardNumberChange}
                  placeholder="0000 0000 0000 0000"
                  maxLength={19}
                  autoComplete="cc-number"
                  inputMode="numeric"
                  className="payment-input"
                />
              </FormField>

              <div className="payment-fields-row">
                <FormField label="Ay">
                  <input
                    value={expireMonth}
                    onChange={(e) => setExpireMonth(e.target.value.replace(/\D/g, '').slice(0, 2))}
                    placeholder="AA"
                    maxLength={2}
                    inputMode="numeric"
                    autoComplete="cc-exp-month"
                    className="payment-input"
                  />
                </FormField>
                <FormField label="Yıl">
                  <input
                    value={expireYear}
                    onChange={(e) => setExpireYear(e.target.value.replace(/\D/g, '').slice(0, 4))}
                    placeholder="YYYY"
                    maxLength={4}
                    inputMode="numeric"
                    autoComplete="cc-exp-year"
                    className="payment-input"
                  />
                </FormField>
                <FormField label="CVC">
                  <input
                    value={cvc}
                    onChange={(e) => setCvc(e.target.value.replace(/\D/g, '').slice(0, 4))}
                    placeholder="•••"
                    maxLength={4}
                    inputMode="numeric"
                    autoComplete="cc-csc"
                    className="payment-input"
                  />
                </FormField>
              </div>

              <button
                type="submit"
                disabled={!isFormValid || submitting}
                className={`payment-submit-btn${(!isFormValid || submitting) ? ' payment-submit-btn--disabled' : ''}`}
              >
                {submitting ? (
                  <>
                    <span className="payment-submit-btn__spinner" />
                    İşleniyor...
                  </>
                ) : (
                  <>
                    <Lock size={16} />
                    ₺{payableAmount.toFixed(2)} Ödemeyi Tamamla
                  </>
                )}
              </button>
            </form>

            <div className="payment-ssl-hint">
              <Lock size={12} />
              Ödeme bilgileriniz iyzico altyapısı ile şifrelenerek güvende tutulur.
            </div>
          </div>
          <div className="payment-summary-cards">
            <div className="payment-summary-card">
              <p className="payment-summary-card__heading">Teslimat Adresi</p>
              <p className="payment-summary-card__title">{address.title}</p>
              <p className="payment-summary-card__sub">{address.recipientName}</p>
              <p className="payment-summary-card__sub">{address.addressLine}, {address.district}/{address.city}</p>
            </div>
            <div className="payment-summary-card">
              <p className="payment-summary-card__heading">Sipariş Özeti</p>
              <div className="payment-summary-card__items">
                {cartItems.map((item) => (
                  <div key={item.itemId} className="payment-summary-card__item">
                    <span className="payment-summary-card__item-name">{item.productName} × {item.quantity}</span>
                    <span className="payment-summary-card__item-price">₺{item.lineTotal.toFixed(2)}</span>
                  </div>
                ))}
              </div>
              {couponCode && discountAmount != null && discountAmount > 0 && (
                <div className="payment-summary-card__discount-row">
                  <span>Kupon İndirimi ({couponCode})</span>
                  <span>-₺{discountAmount.toFixed(2)}</span>
                </div>
              )}
              <div className="payment-summary-card__total-row">
                <span>Toplam</span>
                <span className="payment-summary-card__total-amount">₺{payableAmount.toFixed(2)}</span>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}

function FormField({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="payment-form-group">
      <label className="payment-form-label">{label}</label>
      {children}
    </div>
  )
}

interface LocationState {
  items: OrderItemRequest[]
  totalAmount: number
  couponCode?: string | null
  discountAmount?: number | null
  finalAmount?: number | null
  cartItems: CartItemResponse[]
  address: AddressResponse
}


